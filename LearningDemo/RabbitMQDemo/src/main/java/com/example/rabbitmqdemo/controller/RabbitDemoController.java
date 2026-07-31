package com.example.rabbitmqdemo.controller;

import com.example.rabbitmqdemo.service.RabbitDemoService;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/rabbit")
@Validated
public class RabbitDemoController {

    private final RabbitDemoService rabbitDemoService;

    public RabbitDemoController(RabbitDemoService rabbitDemoService) {
        this.rabbitDemoService = rabbitDemoService;
    }

    // 一对一：发一条消息，只有一个消费者处理
    @PostMapping("/single")
    public ResponseEntity<Map<String, Object>> sendSingle(@RequestBody MessageRequest request) {
        rabbitDemoService.sendSingle(request.message());
        return ok("single", request.message());
    }

    // 多消费者分摊任务：一次发多条消息，两个消费者轮流处理
    @PostMapping("/work")
    public ResponseEntity<Map<String, Object>> sendWork(@RequestBody MessageRequest request) {
        rabbitDemoService.sendWork(request.message());
        return ok("work", request.message());
    }

    // Fanout 扇形交换机：广播消息，所有订阅者都能收到
    @PostMapping("/fanout")
    public ResponseEntity<Map<String, Object>> sendFanout(@RequestBody MessageRequest request) {
        rabbitDemoService.sendFanout(request.message());
        return ok("fanout", request.message());
    }

    // Direct 直连交换机：RoutingKey 精准匹配才能收到
    @PostMapping("/direct")
    public ResponseEntity<Map<String, Object>> sendDirect(@RequestBody MessageWithRoutingKeyRequest request) {
        rabbitDemoService.sendDirect(request.routingKey(), request.message());
        return ok("direct", request.message());
    }

    // Topic 主题交换机：通过 * 和 # 通配符匹配 RoutingKey
    @PostMapping("/topic")
    public ResponseEntity<Map<String, Object>> sendTopic(@RequestBody MessageWithRoutingKeyRequest request) {
        rabbitDemoService.sendTopic(request.routingKey(), request.message());
        return ok("topic", request.message());
    }

    // 创建订单：消息先进入 TTL 队列，15 秒后过期进入死信队列
    @PostMapping("/order")
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody OrderRequest request) {
        rabbitDemoService.createOrder(request.orderId(), request.content());
        return ok("order-ttl", request.orderId());
    }

    // 查询订单状态：观察是否从 CREATED 变为 CANCELLED
    @GetMapping("/order/{orderId}")
    public ResponseEntity<Map<String, Object>> queryOrder(@PathVariable String orderId) {
        return ResponseEntity.ok(new LinkedHashMap<>(rabbitDemoService.getOrderStatus(orderId)));
    }

    // 统一构造返回结果，提示用户去控制台查看消费者输出
    private ResponseEntity<Map<String, Object>> ok(String type, Object payload) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", type);
        result.put("payload", payload);
        result.put("hint", "查看控制台日志可以看到消费者输出");
        return ResponseEntity.ok(result);
    }

    // 请求体：只带一个消息内容
    public record MessageRequest(@NotBlank String message) {}
    // 请求体：带路由键和消息内容，用于 Direct / Topic
    public record MessageWithRoutingKeyRequest(@NotBlank String routingKey, @NotBlank String message) {}
    // 请求体：创建订单需要订单号和订单内容
    public record OrderRequest(@NotBlank String orderId, @NotBlank String content) {}
}
