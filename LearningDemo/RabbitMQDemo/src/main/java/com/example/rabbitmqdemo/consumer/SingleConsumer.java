package com.example.rabbitmqdemo.consumer;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class SingleConsumer {

    // 监听一对一队列：一条消息只会被一个消费者处理
    @RabbitListener(queues = "${app.rabbitmq.queue.single}")
    public void receive(String message) {
        System.out.println("【一对一 SingleConsumer】收到消息: " + message);
    }
}
