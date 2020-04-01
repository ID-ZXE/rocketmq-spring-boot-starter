# rocketmq-spring-boot-starter
一个简单的spring-boot-stater,参考了别人的实现,封装了一层,只使用@RocketmqConsumer注解就可以完成消费,开箱即用

#### RocketmqConsumer注解
```
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Inherited
public @interface RocketmqConsumer {
    /**
     * 主题
     */
    String topic();

    /**
     * group 可以缺省
     */
    String group() default "";

    /**
     * rocketmq的tag,*的时候表示匹配所有
     */
    String tag() default "*";

    /**
     * 默认为集群模式
     */
    boolean isBroadCasting() default false;
}
```

#### 引入依赖
```
把代码download到本地
mvn clean install
然后添加依赖

<dependency>
    <groupId>com.hang</groupId>
    <artifactId>rocketmq-spring-boot-starter</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
<dependency>
    <groupId>org.apache.rocketmq</groupId>
    <artifactId>rocketmq-client</artifactId>
    <version>4.7.0</version>
</dependency>
```

#### 配置项 application.properties
```
#rocket配置
#发送同一类消息的设置为同一个group，保证唯一,默认不需要设置
#rocketmq会使用ip@pid(pid代表jvm名字)作为唯一标示
rocketmq.groupName=defaultGroup
#是否开启自动配置
rocketmq.isEnable=true
#mq的nameserver地址
rocketmq.namesrvAddr=middleware-server-1:9876
#消息最大长度 默认1024*4(4M)
rocketmq.producer.maxMessageSize=4096
#发送消息超时时间,默认3000
rocketmq.producer.sendMsgTimeout=3000
#发送消息失败重试次数，默认2
rocketmq.producer.retryTimesWhenSendFailed=3
#消费者线程数量
rocketmq.consumer.consumeThreadMin=5
rocketmq.consumer.consumeThreadMax=32
#设置一次消费消息的条数，默认为1条
rocketmq.consumer.consumeMessageBatchMaxSize=1
```

#### 示例代码
##### comsumer
```
@Component
public class MyConsumer {

    @Resource
    private MyService myService;

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * 默认为集群消费模式,可以通过isBroadCasting进行设置
     * tag默认为*,匹配所有
     * group默认为properties文件中的通用配置,也可以自行指定
     */
    @RocketmqConsumer(topic = "topicA")
    public void topicA(Message message) {
        String data = new String(message.getBody(), StandardCharsets.UTF_8);
        LOGGER.info("topicA deal data : {}", myService.dealMessage(data));
    }

    @RocketmqConsumer(topic = "topicB", group = "springBootDemo")
    public void topicB(Message message) {
        String data = new String(message.getBody(), StandardCharsets.UTF_8);
        LOGGER.info("topicB deal data : {}", myService.dealMessage(data));
    }
}
```
##### producer
```
@Resource
private DefaultMQProducer defaultMQProducer;

@GetMapping("sendMessage")
public String sendMessage(@RequestParam String data, @RequestParam String topic) throws RemotingException, MQClientException, InterruptedException {
    Message message = new Message();
    message.setBody(data.getBytes());
    message.setTopic(topic);
    defaultMQProducer.send(message, new SendCallback() {
        @Override
        public void onSuccess(SendResult sendResult) {
            LOGGER.info("send success");
        }

        @Override
        public void onException(Throwable e) {
            LOGGER.error("send error", e);
        }
    });
    return "success";
}
```

#### 结果
```
com.hang.demo.SpringBootDemoApplication  : send success
com.hang.demo.consumer.MyConsumer        : topicA deal data : deal:data
com.hang.demo.SpringBootDemoApplication  : send success
com.hang.demo.consumer.MyConsumer        : topicA deal data : deal:data2
```