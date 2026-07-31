package com.example.rabbitmqdemo.consumer;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class WorkConsumerOne {

    // 监听工作队列的第一个消费者：用于演示任务分摊
    @RabbitListener(queues = "${app.rabbitmq.queue.work}")
    public void receive(String message) {
        System.out.println("【多消费者分摊任务 Consumer-1】处理任务: " + message);
    }
}
