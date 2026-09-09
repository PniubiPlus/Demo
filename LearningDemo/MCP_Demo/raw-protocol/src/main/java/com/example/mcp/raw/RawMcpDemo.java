package com.example.mcp.raw;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class RawMcpDemo {

    private static final ObjectMapper JSON = new ObjectMapper();

    private RawMcpDemo() {
    }

    public static void main(String[] args) throws Exception {
        Process process = new ProcessBuilder(serverCommand())
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start();

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {

            JsonNode initializeResult = request(writer, reader, 1, "initialize", JSON.readTree("""
                    {
                      "protocolVersion": "2025-06-18",
                      "capabilities": {},
                      "clientInfo": {"name": "raw-java-demo", "version": "1.0.0"}
                    }
                    """));
            System.out.println("1. 初始化成功: " + initializeResult.path("serverInfo"));

            notification(writer, "notifications/initialized", JSON.createObjectNode());

            JsonNode toolsResult = request(writer, reader, 2, "tools/list", JSON.createObjectNode());
            System.out.println("2. 服务端工具列表:");
            toolsResult.path("tools").forEach(tool ->
                    System.out.println("   - " + tool.path("name").asText()));

            JsonNode arguments = JSON.createObjectNode()
                    .put("message", "Hello MCP from raw Java");
            ObjectNode callParams = JSON.createObjectNode()
                    .put("name", "echo")
                    .set("arguments", arguments);
            JsonNode callResult = request(writer, reader, 3, "tools/call", callParams);
            System.out.println("3. 调用 echo 工具结果: " + callResult.path("content"));
        } finally {
            process.destroy();
        }
    }

    private static List<String> serverCommand() {
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            return List.of("cmd.exe", "/c", "npx", "-y", "@modelcontextprotocol/server-everything");
        }
        return List.of("npx", "-y", "@modelcontextprotocol/server-everything");
    }

    private static JsonNode request(BufferedWriter writer, BufferedReader reader,
                                    int id, String method, JsonNode params) throws Exception {
        ObjectNode request = JSON.createObjectNode()
                .put("jsonrpc", "2.0")
                .put("id", id)
                .put("method", method)
                .set("params", params);
        send(writer, request);

        while (true) {
            String line = reader.readLine();
            if (line == null) {
                throw new IllegalStateException("MCP Server 在响应前退出");
            }
            JsonNode message = JSON.readTree(line);
            if (message.path("id").asInt(-1) == id) {
                if (message.has("error")) {
                    throw new IllegalStateException("MCP 错误: " + message.get("error"));
                }
                return message.get("result");
            }
        }
    }

    private static void notification(BufferedWriter writer, String method, JsonNode params)
            throws Exception {
        ObjectNode notification = JSON.createObjectNode()
                .put("jsonrpc", "2.0")
                .put("method", method)
                .set("params", params);
        send(writer, notification);
    }

    private static void send(BufferedWriter writer, JsonNode message) throws Exception {
        writer.write(JSON.writeValueAsString(message));
        writer.newLine();
        writer.flush();
    }
}
