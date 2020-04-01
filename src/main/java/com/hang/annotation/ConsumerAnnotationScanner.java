package com.hang.annotation;

import com.hang.configuration.RocketmqProperties;
import com.hang.listener.MessageListener;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.MQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.StringUtils;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * @author hangs.zhang
 * @date 2020/3/30 下午6:47
 * *********************
 * function:
 */
public class ConsumerAnnotationScanner implements BeanPostProcessor {

    @Resource
    private RocketmqProperties properties;

    private static final Set<Method> REGISTERED_METHODS = new HashSet<>();

    private static final Set<ListenerHolder> LISTENER_HOLDERS = new HashSet<>();

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    /**
     * postProcessAfterInitialization 此时bean已经加载好了
     */
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        parseMethods(bean, bean.getClass().getDeclaredMethods());
        return bean;
    }

    @PreDestroy
    public void destory() {
        LISTENER_HOLDERS.forEach(ListenerHolder::destory);
    }

    private void parseMethods(final Object bean, Method[] methods) {
        String beanName = bean.getClass().getCanonicalName();
        for (Method method : methods) {
            if (REGISTERED_METHODS.contains(method)) return;

            RocketmqConsumer annotation = AnnotationUtils.findAnnotation(method, RocketmqConsumer.class);
            if (annotation == null) continue;

            if (!Modifier.isPublic(method.getModifiers())) {
                throw new RuntimeException("标记RocketnqConsumer的方法必须是public的");
            }

            String methodName = method.getName();
            Class<?>[] args = method.getParameterTypes();
            String message = String.format("如果想配置成为message listener,方法必须有且只有一个参数,类型必须为org.apache.rocketmq.common.message.Message类型: %s method:%s", beanName, methodName);
            if (args.length != 1) {
                LOGGER.error(message);
                throw new RuntimeException(message);
            }
            if (args[0] != Message.class) {
                LOGGER.error(message);
                throw new RuntimeException(message);
            }

            String topic = annotation.topic();
            if (StringUtils.isEmpty(topic)) {
                String err = String.format("使用@RocketmqConsumer,必须提供topic, class:%s method:%s", beanName, methodName);
                LOGGER.error(err);
                throw new RuntimeException(err);
            }

            REGISTERED_METHODS.add(method);
            ListenerHolder listenerHolder = new ListenerHolder(bean, method);
            listenerHolder.register(annotation);
            LISTENER_HOLDERS.add(listenerHolder);
        }
    }

    private class ListenerHolder {

        private final MessageListener LISTENER;

        private MQPushConsumer consumer;

        public ListenerHolder(Object bean, Method method) {
            this.LISTENER = new GeneratedListener(bean, method);
        }

        public void register(RocketmqConsumer annotation) {
            createConsumer(annotation, LISTENER);
        }

        public void destory() {
            consumer.shutdown();
        }

        public void createConsumer(RocketmqConsumer annotation, MessageListener messageListener) {
            String groupName = StringUtils.isEmpty(annotation.group()) ? properties.getGroupName() : annotation.group();
            if(StringUtils.isEmpty(groupName)) {
                throw new RuntimeException("group name 未设置");
            }
            DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(groupName);
            consumer.setNamesrvAddr(properties.getNameSrvAddr());
            consumer.setConsumeThreadMin(properties.getConsumerConsumeThreadMin());
            consumer.setConsumeThreadMax(properties.getConsumerConsumeThreadMax());
            consumer.registerMessageListener((MessageListenerConcurrently) (msgs, context) -> {
                msgs.forEach(messageListener::onMessage);
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            });


            // 设置Consumer第一次启动是从队列头部开始消费还是队列尾部开始消费 如果非第一次启动，那么按照上次消费的位置继续消费
            consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);

            // 设置消费模型，集群还是广播，默认为集群
            if (annotation.isBroadCasting()) {
                consumer.setMessageModel(MessageModel.BROADCASTING);
            } else {
                consumer.setMessageModel(MessageModel.CLUSTERING);
            }

            // 设置一次消费消息的条数，默认为1条
            consumer.setConsumeMessageBatchMaxSize(properties.getConsumerConsumeMessageBatchMaxSize());
            try {
                consumer.subscribe(annotation.topic(), annotation.tag());
                consumer.start();
                LOGGER.info("init consumer group:{}, topic:{} success", groupName, annotation.topic());
            } catch (MQClientException e) {
                LOGGER.error("init consumer group:{}, topic:{} error", groupName, annotation.topic(), e);
            }
            this.consumer = consumer;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ListenerHolder that = (ListenerHolder) o;
            return Objects.equals(LISTENER, that.LISTENER) &&
                    Objects.equals(consumer, that.consumer);
        }

        @Override
        public int hashCode() {
            return Objects.hash(LISTENER, consumer);
        }
    }

}
