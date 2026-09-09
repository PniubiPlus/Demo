package com.example.agent.self;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;

/** 两个演示工具：查“天气”和算术，足以让 LLM 表现出“选择工具 -> 传参 -> 汇总”的行为 */
public final class DemoTools {

    private static final JsonNodeFactory F = JsonNodeFactory.instance;

    private DemoTools() {
    }

    public static List<Tool> all() {
        List<Tool> tools = new ArrayList<>();
        tools.add(weather());
        tools.add(calculator());
        return tools;
    }

    private static Tool weather() {
        ObjectNode schema = F.objectNode();
        schema.put("type", "object");
        ObjectNode props = schema.putObject("properties");
        props.putObject("city").put("type", "string").put("description", "城市名，如 Beijing");
        schema.putArray("required").add("city");

        return new Tool() {
            @Override public String name() { return "get_weather"; }

            @Override public String description() { return "查询指定城市当前天气（演示用假数据）"; }

            @Override public ObjectNode jsonSchema() { return schema; }

            @Override public String execute(JsonNode arguments) {
                String city = arguments.path("city").asText("unknown");
                // 演示用固定数据；真实场景这里会调用天气 API
                return city + " 当前晴，18 度，湿度 40%";
            }
        };
    }

    private static Tool calculator() {
        ObjectNode schema = F.objectNode();
        schema.put("type", "object");
        ObjectNode props = schema.putObject("properties");
        ObjectNode expression = props.putObject("expression");
        expression.put("type", "string");
        expression.put("description", "四则运算表达式，如 (2+3)*4");
        schema.putArray("required").add("expression");

        return new Tool() {
            @Override public String name() { return "calculator"; }

            @Override public String description() { return "计算一个四则运算表达式的值"; }

            @Override public ObjectNode jsonSchema() { return schema; }

            @Override public String execute(JsonNode arguments) {
                String expression = arguments.path("expression").asText("");
                return String.valueOf(Eval.eval(expression));
            }
        };
    }

    /** 极简四则运算求值（只支持 + - * / 和括号），避免引入额外依赖 */
    private static final class Eval {
        private String input;
        private int pos;

        static double eval(String text) {
            return new Eval(text).parseExpression();
        }

        private Eval(String text) {
            this.input = text.replaceAll("\\s+", "");
        }

        private double parseExpression() {
            double value = parseTerm();
            while (pos < input.length()) {
                char op = input.charAt(pos);
                if (op != '+' && op != '-') {
                    break;
                }
                pos++;
                double right = parseTerm();
                value = op == '+' ? value + right : value - right;
            }
            return value;
        }

        private double parseTerm() {
            double value = parseFactor();
            while (pos < input.length()) {
                char op = input.charAt(pos);
                if (op != '*' && op != '/') {
                    break;
                }
                pos++;
                double right = parseFactor();
                value = op == '*' ? value * right : value / right;
            }
            return value;
        }

        private double parseFactor() {
            if (pos < input.length() && input.charAt(pos) == '(') {
                pos++;
                double value = parseExpression();
                if (pos < input.length() && input.charAt(pos) == ')') {
                    pos++;
                }
                return value;
            }
            int start = pos;
            while (pos < input.length() && (Character.isDigit(input.charAt(pos)) || input.charAt(pos) == '.')) {
                pos++;
            }
            return Double.parseDouble(input.substring(start, pos));
        }
    }
}
