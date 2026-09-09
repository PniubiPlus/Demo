# MCP Java 初步学习 Demo

这个目录用同一个公开测试服务 `@modelcontextprotocol/server-everything` 演示 MCP，避免业务代码干扰学习。需要 JDK 17+、Maven 3.9+、Node.js/npm（含 `npx`）。

## MCP 是否区分框架？

MCP 本身是协议，不属于 LangChain4j、Google ADK 或某个模型厂商。服务端只要遵循 MCP，任意兼容客户端都能连接。

框架主要影响 **客户端如何把 MCP 工具交给模型/Agent**：

| 模块 | 目的 | 是否调用 LLM | 适合先学什么 |
| --- | --- | --- | --- |
| `raw-protocol` | 直接发送 JSON-RPC | 否 | 看清初始化、列工具、调用工具三个协议步骤 |
| `langchain4j-client` | 用 LangChain4j 封装 MCP Client | 否 | 学会少量代码连接和调用 MCP |
| `google-adk-agent` | 把 MCP Toolset 挂到 ADK Agent | 是 | 学会 MCP 如何进入 Agent 工具循环 |

建议按表格从上到下阅读。前两版不需要 API Key，便于把注意力放在 MCP 上。

## 1. 原始协议版

入口：`raw-protocol/src/main/java/com/example/mcp/raw/RawMcpDemo.java`

它通过 stdio 启动 MCP Server，并依次发送：

1. `initialize`：协商协议版本和能力。
2. `notifications/initialized`：告知服务端初始化完成。
3. `tools/list`：发现工具。
4. `tools/call`：调用 `echo` 工具。

运行：

```powershell
mvn -pl raw-protocol compile exec:java
```

重点观察 `RawMcpDemo.request()`：MCP 消息就是 JSON-RPC 2.0，每条 stdio 消息占一行。

## 2. LangChain4j 版

入口：`langchain4j-client/src/main/java/com/example/mcp/langchain4j/LangChain4jMcpDemo.java`

框架替你完成协议初始化和消息匹配，学习代码只剩四步：

1. 创建 `StdioMcpTransport`。
2. 创建 `DefaultMcpClient`。
3. `listTools()`。
4. `executeTool()`。

运行：

```powershell
mvn -pl langchain4j-client compile exec:java
```

当前示例故意直接调用工具，不接 LLM。理解后，可以创建 `McpToolProvider`，再传给 LangChain4j `AiServices.builder(...).toolProvider(...)`，模型就能自行选择 MCP 工具。

## 3. Google ADK 版

入口：`google-adk-agent/src/main/java/com/example/mcp/adk/McpAgent.java`

这里展示 Agent 框架的核心接法：

1. 用 `StdioServerParameters` 描述 MCP Server。
2. 包装成 `McpToolset`。
3. 将 Toolset 放进 `LlmAgent.tools(...)`。

先编译：

```powershell
mvn -pl google-adk-agent compile
```

运行 ADK Agent 需要 Gemini 凭据和 ADK Runner/Dev UI。设置凭据后，可按 Google ADK Java 文档使用开发界面加载 `McpAgent.ROOT_AGENT`。该模块只保留 Agent 定义，避免 Session、Runner 和 UI 代码掩盖 MCP 接入重点。

## 一次编译全部模块

```powershell
mvn clean compile
```

## 读完后可做的三个小练习

1. 把前两版调用的 `echo` 改成 Server Everything 暴露的其他工具。
2. 把 stdio MCP Server 换成你自己的 Server，客户端代码通常不需要改变业务层。
3. 将 LangChain4j 版接入一个支持 Tool Calling 的模型，比较“程序直接选工具”和“模型自行选工具”的区别。

## 常见问题

- Windows 上执行 `npx`：示例通过 `cmd.exe /c npx ...` 启动，避免 Java 找不到 `npx.cmd`。
- 首次运行较慢：`npx -y` 会下载测试 MCP Server。
- stdio Server 的标准输出只能放 MCP 消息；日志应写标准错误，否则会破坏协议。
- 生产环境不要无条件运行未知 MCP Server；工具调用拥有的文件、网络和命令权限都应按最小权限控制。
