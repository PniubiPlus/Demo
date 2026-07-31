package com.example.rabbitmqdemo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConfigurationProperties(prefix = "app.rabbitmq")
public class DemoProperties {

    // application.yml 中定义的队列名称集合
    private Map<String, String> queue;
    // application.yml 中定义的交换机名称集合
    private Map<String, String> exchange;
    // application.yml 中定义的 routingKey 配置集合
    private Map<String, Object> routingKey;

    public Map<String, String> getQueue() {
        return queue;
    }

    public void setQueue(Map<String, String> queue) {
        this.queue = queue;
    }

    public Map<String, String> getExchange() {
        return exchange;
    }

    public void setExchange(Map<String, String> exchange) {
        this.exchange = exchange;
    }

    public Map<String, Object> getRoutingKey() {
        return routingKey;
    }

    public void setRoutingKey(Map<String, Object> routingKey) {
        this.routingKey = routingKey;
    }
}
