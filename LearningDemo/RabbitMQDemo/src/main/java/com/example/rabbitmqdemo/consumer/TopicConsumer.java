package com.example.rabbitmqdemo.consumer;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class TopicConsumer {

    // 监听 Topic 队列：匹配到 demo.# 这类规则时会收到消息
    @RabbitListener(queues = "${app.rabbitmq.queue.topic}")
    public void receive(String message) {
        System.out.println("【Topic 主题交换机】通配符消息收到: " + message);
    }
}
