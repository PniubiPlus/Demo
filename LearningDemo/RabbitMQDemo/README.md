# RabbitMQDemo

这是一个面向零基础的 RabbitMQ 学习示例，基于 Spring Boot 3 + Spring AMQP。

## 包含的示例

- 一对一消息模型
- 多消费者分摊任务：两个监听同一个队列
- Fanout 扇形交换机：广播发布订阅
- Direct 直连交换机：精准匹配 RoutingKey
- Topic 主题交换机：`*` 和 `#` 通配符
- DLX 死信队列 + TTL 延迟队列：订单超时取消

## 项目结构

- `config`：RabbitMQ 队列、交换机、绑定关系
- `controller`：提供发送消息的 REST 接口
- `service`：封装发送逻辑和订单状态
- `consumer`：各种消费者示例

## 运行前准备

1. 本地安装并启动 RabbitMQ
2. 默认连接地址：`localhost:5672`
3. 默认账号：`guest / guest`

如果 RabbitMQ 不在本机，请修改 `src/main/resources/application.yml`

## 启动项目

```bash
mvn spring-boot:run
```

## 接口说明

### 1. 一对一

请求：

```bash
POST /rabbit/single
{
  "message": "hello single"
}
```

### 2. 多消费者分摊任务

请求：

```bash
POST /rabbit/work
{
  "message": "job"
}
```

同一个队列有两个监听器，消息会被两个消费者轮流处理，达到“分摊任务”的效果。

### 3. Fanout 扇形交换机

请求：

```bash
POST /rabbit/fanout
{
  "message": "broadcast"
}
```

Fanout 不看 RoutingKey，所有绑定到该交换机的队列都会收到消息。

### 4. Direct 直连交换机

请求：

```bash
POST /rabbit/direct
{
  "routingKey": "demo.direct.key",
  "message": "direct msg"
}
```

只有 RoutingKey 精准匹配时，消息才会投递到队列。

### 5. Topic 主题交换机

请求：

```bash
POST /rabbit/topic
{
  "routingKey": "demo.topic.java",
  "message": "topic msg"
}
```

Topic 支持：

- `*`：匹配一个单词
- `#`：匹配零个或多个单词

例如：

- `demo.*`
- `demo.#`
- `demo.topic.*`

### 6. DLX 死信队列 + TTL 延迟队列

创建订单：

```bash
POST /rabbit/order
{
  "orderId": "1001",
  "content": "apple"
}
```

规则说明：

- 消息先进入 TTL 队列
- 15 秒后如果还没被正常消费，就会变成死信
- 死信被路由到 DLX 队列
- DLX 消费者把订单状态改为 `CANCELLED`

查询订单：

```bash
GET /rabbit/order/1001
```

## 你可以怎么理解它

- **队列**：消息暂存的地方
- **交换机**：决定消息发给哪个队列
- **RoutingKey**：交换机分配消息时的匹配条件
- **消费者**：真正处理消息的程序
- **死信队列**：消息“过期、拒收、积压后”的去处

## 学习建议

建议按这个顺序看：

1. 先理解一对一
2. 再看多消费者分摊任务
3. 再理解 Fanout / Direct / Topic 的区别
4. 最后理解 TTL 和 DLX

## 说明

这个项目主要用于学习 RabbitMQ 概念，控制台日志就是最直观的观察方式。
