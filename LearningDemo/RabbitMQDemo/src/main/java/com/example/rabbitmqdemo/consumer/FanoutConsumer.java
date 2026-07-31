package com.example.rabbitmqdemo.consumer;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class FanoutConsumer {

    // 监听 Fanout 队列：交换机广播后，所有绑定队列都能收到
    @RabbitListener(queues = "${app.rabbitmq.queue.fanout}")
    public void receive(String message) {
        System.out.println("【Fanout 扇形交换机】广播订阅收到: " + message);
    }
}
