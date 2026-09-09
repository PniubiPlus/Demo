package com.example.mcp.adk;

import com.google.adk.agents.LlmAgent;
import com.google.adk.tools.mcp.McpToolset;
import com.google.adk.tools.mcp.StdioServerParameters;
import com.google.common.collect.ImmutableList;

public final class McpAgent {

    public static final LlmAgent ROOT_AGENT = LlmAgent.builder()
            .name("mcp_learning_agent")
            .description("A small agent that learns MCP tool usage.")
            .model("gemini-2.0-flash")
            .instruction("Use the MCP tools when they can answer the user's request.")
            .tools(ImmutableList.of(createMcpToolset()))
            .build();

    private McpAgent() {
    }

    private static McpToolset createMcpToolset() {
        StdioServerParameters parameters = StdioServerParameters.builder()
                .command(isWindows() ? "cmd.exe" : "npx")
                .args(isWindows()
                        ? ImmutableList.of("/c", "npx", "-y", "@modelcontextprotocol/server-everything")
                        : ImmutableList.of("-y", "@modelcontextprotocol/server-everything"))
                .build();
        return new McpToolset(parameters.toServerParameters());
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}
