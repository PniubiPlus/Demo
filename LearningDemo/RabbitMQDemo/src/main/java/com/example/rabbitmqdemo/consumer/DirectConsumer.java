package com.example.rabbitmqdemo.consumer;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class DirectConsumer {

    // 监听 Direct 队列：只有 routingKey 精准匹配才会收到
    @RabbitListener(queues = "${app.rabbitmq.queue.direct}")
    public void receive(String message) {
        System.out.println("【Direct 直连交换机】精准匹配 RoutingKey 收到: " + message);
    }
}
