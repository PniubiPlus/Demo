package com.example.rabbitmqdemo.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitConfig {

    @Bean
    public Queue singleQueue(DemoProperties properties) {
        // 最基础的一对一队列：一个消息进入，一个消费者处理
        return new Queue(properties.getQueue().get("single"), true);
    }

    @Bean
    public Queue workQueue(DemoProperties properties) {
        // 工作队列：多个消费者监听同一个队列，消息会被分摊处理
        return new Queue(properties.getQueue().get("work"), true);
    }

    @Bean
    public Queue fanoutQueue(DemoProperties properties) {
        // Fanout 示例队列：只要绑定到 Fanout 交换机，就能收到广播消息
        return new Queue(properties.getQueue().get("fanout"), true);
    }

    @Bean
    public Queue directQueue(DemoProperties properties) {
        // Direct 示例队列：只有 RoutingKey 精准匹配才会收到消息
        return new Queue(properties.getQueue().get("direct"), true);
    }

    @Bean
    public Queue topicQueue(DemoProperties properties) {
        // Topic 示例队列：用于演示 * 和 # 的通配符匹配
        return new Queue(properties.getQueue().get("topic"), true);
    }

    @Bean
    public Queue orderQueue(DemoProperties properties) {
        // 死信队列：TTL 队列过期后，消息最终会被路由到这里
        return new Queue(properties.getQueue().get("order"), true);
    }

    @Bean
    public Queue orderTtlQueue(DemoProperties properties) {
        Map<String, Object> args = new HashMap<>();
        // 过期后的消息会进入哪个交换机
        args.put("x-dead-letter-exchange", properties.getExchange().get("order.dlx"));
        // 过期后的消息进入死信交换机时使用的 RoutingKey
        args.put("x-dead-letter-routing-key", properties.getQueue().get("order"));
        // 15 秒后消息过期，模拟订单超时场景
        args.put("x-message-ttl", 15000);
        return QueueBuilder.durable(properties.getQueue().get("order.ttl")).withArguments(args).build();
    }

    @Bean
    public DirectExchange directExchange(DemoProperties properties) {
        return new DirectExchange(properties.getExchange().get("direct"));
    }

    @Bean
    public FanoutExchange fanoutExchange(DemoProperties properties) {
        return new FanoutExchange(properties.getExchange().get("fanout"));
    }

    @Bean
    public TopicExchange topicExchange(DemoProperties properties) {
        return new TopicExchange(properties.getExchange().get("topic"));
    }

    @Bean
    public DirectExchange orderTtlExchange(DemoProperties properties) {
        return new DirectExchange(properties.getExchange().get("order.ttl"));
    }

    @Bean
    public DirectExchange orderDlxExchange(DemoProperties properties) {
        return new DirectExchange(properties.getExchange().get("order.dlx"));
    }

    @Bean
    public Binding fanoutBinding(FanoutExchange fanoutExchange, Queue fanoutQueue) {
        // Fanout 不需要 RoutingKey，绑定后会接收交换机广播的所有消息
        return BindingBuilder.bind(fanoutQueue).to(fanoutExchange);
    }

    @Bean
    public Binding directBinding(DirectExchange directExchange, Queue directQueue, DemoProperties properties) {
        // Direct 交换机必须精确匹配 RoutingKey 才会路由到队列
        return BindingBuilder.bind(directQueue).to(directExchange).with(properties.getRoutingKey().get("direct").toString());
    }

    @Bean
    public Binding topicBinding(TopicExchange topicExchange, Queue topicQueue) {
        // Topic 使用 demo.#，表示 demo. 开头的所有路由键都能匹配
        return BindingBuilder.bind(topicQueue).to(topicExchange).with("demo.#");
    }

    @Bean
    public Binding orderTtlBinding(DirectExchange orderTtlExchange, Queue orderTtlQueue) {
        // TTL 队列先接收订单消息，消息过期后再进入死信交换机
        return BindingBuilder.bind(orderTtlQueue).to(orderTtlExchange).with(orderTtlQueue.getName());
    }

    @Bean
    public Binding orderDlxBinding(DirectExchange orderDlxExchange, Queue orderQueue) {
        // 死信交换机把过期消息路由到真正的订单死信队列
        return BindingBuilder.bind(orderQueue).to(orderDlxExchange).with(orderQueue.getName());
    }
}
