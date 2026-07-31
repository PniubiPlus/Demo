package com.example.rabbitmqdemo.consumer;

import com.example.rabbitmqdemo.service.RabbitDemoService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class DeadLetterConsumer {

    private final RabbitDemoService rabbitDemoService;
    private final RabbitTemplate rabbitTemplate;

    public DeadLetterConsumer(RabbitDemoService rabbitDemoService, RabbitTemplate rabbitTemplate) {
        this.rabbitDemoService = rabbitDemoService;
        this.rabbitTemplate = rabbitTemplate;
    }

    // 监听死信队列：TTL 队列中的订单过期后会被路由到这里
    @RabbitListener(queues = "${app.rabbitmq.queue.order}")
    public void receiveFromDeadLetter(String message) {
        String orderId = extractOrderId(message);
        System.out.println("【DLX 死信队列】订单超时被取消，内容: " + message);
        rabbitDemoService.markOrderCancelled(orderId);
    }

    // 从消息中提取订单号，格式约定为 "orderId:content"
    private String extractOrderId(String message) {
        int colon = message.indexOf(':');
        return colon >= 0 ? message.substring(0, colon).trim() : message;
    }
}
