package com.example.agent.self;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * 手写的 OpenAI Chat Completions 客户端。
 * 不依赖任何 AI 框架，只暴露一个能力：把 messages + tools 发过去，拿回 assistant message。
 */
public final class OpenAiCompatClient {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    private final String baseUrl;
    private final String apiKey;
    private final String model;

    public OpenAiCompatClient(String baseUrl, String apiKey, String model) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.model = model;
    }

    public JsonNode chat(ArrayNode messages, ArrayNode toolSchemas) throws Exception {
        ObjectNode body = JSON.createObjectNode();
        body.put("model", model);
        body.set("messages", messages);
        body.set("tools", toolSchemas);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/v1/chat/completions"))
                .timeout(Duration.ofSeconds(120))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body)))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("LLM 调用失败 HTTP " + response.statusCode() + ": " + response.body());
        }
        return JSON.readTree(response.body()).path("choices").path(0).path("message");
    }

    /** 把 OpenAI 返回的 assistant message（含 tool_calls）原样追加回消息列表 */
    public static void appendAssistantMessage(ArrayNode messages, JsonNode assistantMessage) {
        messages.add(assistantMessage.deepCopy());
    }

    /** 把工具执行结果作为 tool message 追加回消息列表 */
    public static void appendToolResult(ArrayNode messages, String toolCallId, String result) {
        ObjectNode toolMessage = messages.addObject();
        toolMessage.put("role", "tool");
        toolMessage.put("tool_call_id", toolCallId);
        toolMessage.put("content", result);
    }
}
