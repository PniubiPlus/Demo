"""
LangChain 版 Agent。

与 java-self-agent 完全相同的任务和工具：
核心同样是 tool-calling 循环，但 HTTP 拼装、工具 Schema 生成、消息追加、
工具分发、循环推进、异常回填全部由 create_agent / ChatOpenAI 在框架内部完成。
"""

import os

from langchain.agents import create_agent
from langchain.tools import tool
from langchain_openai import ChatOpenAI


@tool
def get_weather(city: str) -> str:
    """查询指定城市当前天气（演示用假数据）。"""
    return f"{city} 当前晴，18 度，湿度 40%"


@tool
def calculator(expression: str) -> str:
    """计算一个四则运算表达式的值，如 '(2+3)*4'。"""
    return str(eval(expression, {"__builtins__": {}}, {}))  # type: ignore[no-eval]


SYSTEM_PROMPT = "你是一个能调用工具的助手。需要数据或计算时优先使用工具，回答保持简短。"
QUESTION = "北京现在天气怎么样？如果温度是摄氏度，帮我算一下它乘 9 除 5 再加 32 是多少华氏度？"


def main() -> None:
    api_key = os.getenv("LLM_API_KEY") or os.getenv("OPENAI_API_KEY", "")
    base_url = os.getenv("LLM_BASE_URL", "https://api.openai.com/v1")
    model_name = os.getenv("LLM_MODEL", "gpt-4o-mini")

    if not api_key:
        print("请先设置环境变量 LLM_API_KEY（或 OPENAI_API_KEY）")
        print("PowerShell 示例:")
        print('  $env:LLM_API_KEY="sk-..."')
        print('  $env:LLM_BASE_URL="https://api.openai.com/v1"   # 或任意 OpenAI 兼容 base')
        print('  $env:LLM_MODEL="gpt-4o-mini"')
        return

    llm = ChatOpenAI(model=model_name, api_key=api_key, base_url=base_url, temperature=0)

    agent = create_agent(
        model=llm,
        tools=[get_weather, calculator],
        system_prompt=SYSTEM_PROMPT,
    )

    result = agent.invoke({"messages": [{"role": "user", "content": QUESTION}]})

    last_message = result["messages"][-1]
    content = last_message.content
    if not isinstance(content, str):
        # 内容可能是多段结构化块（如 reasoning + text），只取可读文本
        content = "".join(
            block.get("text", "") if isinstance(block, dict) else str(block)
            for block in content
        )
    print("Agent 最终回答:")
    print(content)


if __name__ == "__main__":
    main()
