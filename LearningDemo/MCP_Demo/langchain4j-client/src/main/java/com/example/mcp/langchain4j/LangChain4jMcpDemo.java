package com.example.mcp.langchain4j;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;

import java.util.List;

public final class LangChain4jMcpDemo {

    private LangChain4jMcpDemo() {
    }

    public static void main(String[] args) throws Exception {
        McpTransport transport = StdioMcpTransport.builder()
                .command(serverCommand())
                .logEvents(true)
                .build();

        try (McpClient client = DefaultMcpClient.builder()
                .key("learning-server")
                .transport(transport)
                .build()) {

            System.out.println("MCP Server 提供的工具:");
            client.listTools().forEach(tool ->
                    System.out.println("- " + tool.name() + ": " + tool.description()));

            ToolExecutionRequest request = ToolExecutionRequest.builder()
                    .name("echo")
                    .arguments("{\"message\":\"Hello MCP from LangChain4j\"}")
                    .build();

            System.out.println("\n直接调用 echo:");
            System.out.println(client.executeTool(request));
        }
    }

    private static List<String> serverCommand() {
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            return List.of("cmd.exe", "/c", "npx", "-y", "@modelcontextprotocol/server-everything");
        }
        return List.of("npx", "-y", "@modelcontextprotocol/server-everything");
    }
}
