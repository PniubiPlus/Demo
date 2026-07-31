package com.example.rabbitmqdemo.consumer;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class WorkConsumerTwo {

    // 监听工作队列的第二个消费者：与 Consumer-1 一起分摊消息
    @RabbitListener(queues = "${app.rabbitmq.queue.work}")
    public void receive(String message) {
        System.out.println("【多消费者分摊任务 Consumer-2】处理任务: " + message);
    }
}
