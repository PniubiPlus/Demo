package com.example;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

public class LLMConfig {

    private final String baseUrl;
    private final String model;
    private final String apiKey;

    public LLMConfig(String baseUrl, String model, String apiKey) {
        this.baseUrl = requireText(baseUrl, "baseUrl");
        this.model = requireText(model, "model");
        this.apiKey = requireText(apiKey, "apiKey");
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getModel() {
        return model;
    }

    public String getApiKey() {
        return apiKey;
    }

    public static LLMConfig fromYaml() {
        try (InputStream inputStream = LLMConfig.class.getClassLoader().getResourceAsStream("application.yml")) {
            if (inputStream == null) {
                throw new IllegalStateException("未找到 application.yml，请放到 src/main/resources 目录下");
            }

            Yaml yaml = new Yaml();
            Object data = yaml.load(inputStream);
            if (!(data instanceof Map<?, ?> root)) {
                throw new IllegalStateException("application.yml 格式不正确");
            }

            Object llmObject = root.get("llm");
            if (!(llmObject instanceof Map<?, ?> llm)) {
                throw new IllegalStateException("application.yml 中缺少 llm 配置");
            }

            String baseUrl = toText(llm.get("base-url"));
            String model = toText(llm.get("model"));
            String apiKey = toText(llm.get("api-key"));

            return new LLMConfig(baseUrl, model, apiKey);
        } catch (Exception e) {
            throw new IllegalStateException("读取 application.yml 失败: " + e.getMessage(), e);
        }
    }

    private static String toText(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " 不能为空");
        }
        return value.trim();
    }
}
