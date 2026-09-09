# 自己写 Agent（Java）vs 用 LangChain 写 Agent（Python）

两个 demo 回答**同一个问题**、注册**同样的两个工具**（`get_weather`、`calculator`）、都走 OpenAI 兼容接口，唯一的变量是：**谁来搭 Agent 的骨架**。

| 目录 | 方式 | 依赖 |
| --- | --- | --- |
| `java-self-agent` | 纯手写：JDK HttpClient + Jackson，不用任何 AI 框架 | 仅 `jackson-databind` |
| `python-langchain-agent` | 用 LangChain 的 `create_agent` + `@tool` | `langchain`、`langchain-openai` |

## Agent 的本质：模型 + 工具 + 一个循环

两个 demo 跑的是同一个循环，一条不多一条不少：

1. 把 `system + user` 消息和工具 Schema 发给 LLM；
2. LLM 要么直接回答，要么返回 `tool_calls`（"我要调 get_weather(city=北京)"）；
3. 有 `tool_calls` 就本地执行工具，把结果以 `tool` 消息追加回消息列表，回到第 1 步；
4. 没有工具调用时，模型给出最终回答，循环结束。

区别只在于：这 4 步在 Java 版里是一行行写出来的代码；在 Python 版里是框架内部的一次 `invoke()`。

## 运行

两个 demo 读同一组环境变量，方便对照（任意 OpenAI 兼容服务均可：OpenAI、DeepSeek、Qwen、Moonshot、本地 vLLM 等）：

| 环境变量 | 说明 | 默认值 |
| --- | --- | --- |
| `LLM_API_KEY` | 必填 | 无 |
| `LLM_BASE_URL` | 接口地址（Java 版填到域名，Python 版填到 `/v1`） | `https://api.openai.com` |
| `LLM_MODEL` | 模型名 | `gpt-4o-mini` |

### Java（需要 JDK 17+、Maven 3.9+）

```powershell
cd java-self-agent
$env:LLM_API_KEY="sk-..."
mvn compile exec:java
```

### Python（需要 Python 3.10+）

```powershell
cd python-langchain-agent
pip install -r requirements.txt
$env:LLM_API_KEY="sk-..."
python agent.py
```

## 不谈语言：自己写 vs 用 LangChain，到底差在哪

先划清一个界限：下面所有差异都来自"**自研 vs 框架**"，而不是 Java vs Python。Java 生态同样有 LangChain4j、Spring AI；Python 里你也可以用 `httpx` 自己撸一个 agent。语言只是载体。

### 1. 你写的是"机制"，框架版写的是"配置"

- Java 版每个环节都在明面上：HTTP 请求体怎么拼、工具的 JSON Schema 长什么样、`tool_calls` 怎么解析、工具结果怎么以 `tool` 消息回填、循环何时终止。**代码即文档**，想理解 Agent 是什么，读这 4 个类就够了。
- Python 版只声明三样东西：用哪个模型、有哪些工具、系统提示词是什么。"怎么运转"全部沉到 `create_agent` 里。写起来快，但循环过程对你是不可见的。

### 2. 工具定义：手写 Schema vs 自动生成

- Java 版：每个工具要手工写出 OpenAI function 格式的 JSON Schema（`Tool.jsonSchema()`），参数类型、必填项、描述全靠自己维护，改一个字段要同步改两处。
- LangChain 版：`@tool` 装饰器从**函数签名 + docstring 自动生成 Schema**。函数加个参数，Schema 自动跟上。工具一多（十几个起），这个差距是数量级的。

### 3. 模型适配：写死一个协议 vs 可插拔

- Java 版：代码写死了 OpenAI 的请求/响应格式。想换 Anthropic？请求体、响应体、tool_calls 字段名都不一样，等于再写一个客户端。
- LangChain 版：模型是一个可替换组件（`ChatOpenAI` / `ChatAnthropic` / Ollama……），Agent 代码一行不改。框架还负责把"这个模型支持原生 tool calling、那个不支持（退化成 ReAct 文本协议）"这类**模型能力差异**抹平——自己写时这些坑全要自己踩。

### 4. 横向能力：自己造轮子 vs 即插即用

自己写的版本只有"循环"这一个能力。往下每加一个常见需求，都是自己扩展工程：

| 需求 | 自己写 | LangChain |
| --- | --- | --- |
| 多轮记忆 | 自己维护消息列表 / 外挂存储 | `checkpointer` + `thread_id` 一行配置 |
| 流式输出 | 解析 SSE 流，逐 token 处理 | `agent.stream(...)` |
| 工具调用失败重试 | 自己 try/catch + 回填错误消息 | 重试/降级中间件 |
| 并行工具调用 | 自己做并发调度 | 内置并行执行 |
| 全链路追踪 | 自己打日志 | LangSmith 开箱即用（每轮 prompt、每次工具调用全程可见） |
| 人审 / 护栏 | 自己在循环里插逻辑 | Human-in-the-loop、PII 等中间件 |

### 5. 可控性与调试的取舍

- 自己写：**全透明**。prompt 实际长什么样、发了什么、收到了什么，print 一下就是真相。没有黑盒，也没有意外。
- LangChain：中间隔着多层抽象（消息模型、状态图、中间件栈）。当结果不符合预期时，第一个问题往往是"**框架到底把什么 prompt 发给了模型**"——要读框架源码或开 LangSmith 才能回答。抽象省下的事，最终以调试成本的形式还回来一部分。

### 6. 依赖与生命周期

- 自己写：只依赖 HTTP + JSON 协议。协议本身极稳定，代码放五年基本还能跑。
- LangChain：API 迭代很快（`AgentExecutor` → LangGraph → `create_agent`），大版本升级是常态，跟着升级、迁移是持续成本；换来的是社区新模式（MCP、多 Agent、深思考等）总能很快用上。

## 怎么选

- **学原理 / 需要极致可控**（安全审计、特殊协议、极简依赖）：自己写。`java-self-agent` 那 4 个类就是 Agent 的全部真相。
- **做产品 / 要交付**：用框架。工具多了、要记忆、要流式、要观测，自研的边际成本陡增，框架把这些变成了配置项。
- 这两者不冲突：**自己写一遍，是用好任何框架的前提**——因为你已经知道框架在替你做那 4 步循环，框架出问题时你能立刻定位是哪一步。

## 一句话总结

自己写 Agent，写的是 Agent 本身：循环、协议、调度，一切可见，一切自理；用 LangChain 写 Agent，写的是 Agent 的需求：模型、工具、提示词，其余交给框架——**前者给你理解和控制力，后者给你速度和生态**。
