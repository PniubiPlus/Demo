package com.example.rabbitmqdemo.service;

import com.example.rabbitmqdemo.config.DemoProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RabbitDemoService {

    private final RabbitTemplate rabbitTemplate;
    private final DemoProperties properties;
    private final Map<String, String> orderStore = new ConcurrentHashMap<>();

    public RabbitDemoService(RabbitTemplate rabbitTemplate, DemoProperties properties) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    // 最基础的消息发送：把消息直接发到一对一队列
    public void sendSingle(String message) {
//        rabbitTemplate.convertAndSend(properties.getQueue().get("single"), message);
        rabbitTemplate.convertAndSend("exchangeName.exchage","routingkey" ,"message");
    }

    // 一次发送 10 条任务消息，用于观察多个消费者如何分摊
    public void sendWork(String message) {
        for (int i = 1; i <= 10; i++) {
            rabbitTemplate.convertAndSend(properties.getQueue().get("work"), message + " #" + i);
        }
    }

    // Fanout：交换机不看 routingKey，适合广播场景
    public void sendFanout(String message) {
        rabbitTemplate.convertAndSend(properties.getExchange().get("fanout"), "", message);
    }

    // Direct：routingKey 必须和绑定值完全一致
    public void sendDirect(String routingKey, String message) {
        rabbitTemplate.convertAndSend(properties.getExchange().get("direct"), routingKey, message);
    }

    // Topic：routingKey 可以使用通配符规则进行匹配
    public void sendTopic(String routingKey, String message) {
        rabbitTemplate.convertAndSend(properties.getExchange().get("topic"), routingKey, message);
    }

    // 创建订单：先记录状态，再把消息放入 TTL 队列，等待过期进入死信队列
    public void createOrder(String orderId, String content) {
        orderStore.put(orderId, "CREATED");
        rabbitTemplate.convertAndSend(
                properties.getExchange().get("order.ttl"),
                properties.getQueue().get("order.ttl"),
                orderId + ":" + content
        );
    }

    // 查询订单当前状态：用于观察超时前后状态变化
    public Map<String, String> getOrderStatus(String orderId) {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("orderId", orderId);
        result.put("status", orderStore.getOrDefault(orderId, "NOT_FOUND"));
        return result;
    }

    // 死信队列消费到过期订单后，把订单标记为取消
    public void markOrderCancelled(String orderId) {
        orderStore.put(orderId, "CANCELLED");
    }
}
