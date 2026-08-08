package com.example.rabbitmqdemo.consumer;

import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.stereotype.Component;

@Component
@RabbitListener(bindings = @QueueBinding(
        value = @Queue(name = "",durable = "true"),
        exchange = @Exchange(name = "exchangeName",type = ExchangeTypes.DIRECT,durable = "true"),
        key =
))
public class SingleConsumer {

    // 监听一对一队列：一条消息只会被一个消费者处理           midengxing
    @RabbitHandler
    public void receive(String message) {
        System.out.println("【一对一 SingleConsumer】收到消息: " + message);
    }
}
