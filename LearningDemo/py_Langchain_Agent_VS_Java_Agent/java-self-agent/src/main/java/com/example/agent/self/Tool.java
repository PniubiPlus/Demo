package com.example.agent.self;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Agent 能力单元。
 * 每个 Tool 对外暴露 JSON Schema，Agent 负责把 LLM 产生的 arguments 原样传进来。
 */
public interface Tool {

    String name();

    String description();

    /** JSON Schema，符合 OpenAI function-calling 的 parameters 格式 */
    com.fasterxml.jackson.databind.node.ObjectNode jsonSchema();

    /** 执行工具，返回字符串结果（会作为 tool message 回填给 LLM） */
    String execute(JsonNode arguments);
}
