package com.example.agent.self;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 自己写 Agent 的核心：一个 while 循环。
 *
 * 1. 把 system + user 消息和工具 Schema 发给 LLM；
 * 2. LLM 要么直接回答（finish），要么返回 tool_calls；
 * 3. 有 tool_calls 就本地执行工具，把结果以 tool message 追加回消息列表，回到第 1 步；
 * 4. 没有工具调用时模型给出最终回答，循环结束。
 *
 * 这就是 Agent 的全部骨架（ReAct / tool-calling loop）。
 */
public final class SelfAgentDemo {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final JsonNodeFactory F = JsonNodeFactory.instance;
    private static final int MAX_STEPS = 8;

    public static void main(String[] args) throws Exception {
        String baseUrl = envOrDefault("LLM_BASE_URL", "https://api.openai.com");
        String apiKey = System.getenv().getOrDefault("LLM_API_KEY", "");
        String model = envOrDefault("LLM_MODEL", "gpt-4o-mini");

        if (apiKey.isBlank()) {
            System.out.println("请先设置环境变量 LLM_API_KEY（可选 LLM_BASE_URL / LLM_MODEL，兼容 OpenAI 协议即可）");
            System.out.println("PowerShell 示例:");
            System.out.println("  $env:LLM_API_KEY=\"sk-...\"");
            System.out.println("  $env:LLM_BASE_URL=\"https://api.openai.com\"   # 或任意 OpenAI 兼容网关");
            System.out.println("  $env:LLM_MODEL=\"gpt-4o-mini\"");
            return;
        }

        Map<String, Tool> toolRegistry = new HashMap<>();
        ArrayNode toolSchemas = F.arrayNode();
        for (Tool tool : DemoTools.all()) {
            toolRegistry.put(tool.name(), tool);
            toolSchemas.add(toolSchema(tool));
        }

        ArrayNode messages = F.arrayNode();
        messages.addObject()
                .put("role", "system")
                .put("content", "你是一个能调用工具的助手。需要数据或计算时优先使用工具，回答保持简短。");
        messages.addObject()
                .put("role", "user")
                .put("content", "北京现在天气怎么样？如果温度是摄氏度，帮我算一下它乘 9 除 5 再加 32 是多少华氏度？");

        OpenAiCompatClient client = new OpenAiCompatClient(baseUrl, apiKey, model);

        for (int step = 1; step <= MAX_STEPS; step++) {
            JsonNode assistant = client.chat(messages, toolSchemas);
            OpenAiCompatClient.appendAssistantMessage(messages, assistant);

            JsonNode toolCalls = assistant.path("tool_calls");
            if (toolCalls.isMissingNode() || !toolCalls.isArray() || toolCalls.isEmpty()) {
                System.out.println("Agent 最终回答: " + assistant.path("content").asText());
                return;
            }

            for (JsonNode toolCall : toolCalls) {
                String callId = toolCall.path("id").asText();
                String toolName = toolCall.path("function").path("name").asText();
                JsonNode arguments = JSON.readTree(toolCall.path("function").path("arguments").asText("{}"));

                Tool tool = toolRegistry.get(toolName);
                String result = tool != null
                        ? tool.execute(arguments)
                        : "未知工具: " + toolName;

                System.out.println("第 " + step + " 轮 -> 调用工具 " + toolName + "(" + arguments + ") = " + result);
                OpenAiCompatClient.appendToolResult(messages, callId, result);
            }
        }
        System.out.println("超过最大轮数 " + MAX_STEPS + "，强制结束");
    }

    /** 组装单个工具的 OpenAI function 定义 */
    private static ObjectNode toolSchema(Tool tool) {
        ObjectNode function = F.objectNode();
        function.put("name", tool.name());
        function.put("description", tool.description());
        function.set("parameters", tool.jsonSchema());

        ObjectNode definition = F.objectNode();
        definition.put("type", "function");
        definition.set("function", function);
        return definition;
    }

    private static String envOrDefault(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}
