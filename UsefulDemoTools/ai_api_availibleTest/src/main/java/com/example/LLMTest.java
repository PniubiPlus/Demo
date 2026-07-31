package com.example;

public class LLMTest {

    public static void main(String[] args) {
        try {
            LLMConfig config = LLMConfig.fromYaml();
            LLMClient client = new LLMClient();
            String answer = client.testConnection(config, "请回复一句简短的测试成功信息。");
            System.out.println("模型调用成功，返回内容如下：");
            System.out.println(answer);
        } catch (Exception e) {
            System.err.println("模型调用失败：" + e.getMessage());
            e.printStackTrace(System.err);
        }
    }
}
