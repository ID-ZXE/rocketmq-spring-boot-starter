package com.hang.annotation;

import com.hang.configuration.RocketmqProperties;
import com.hang.listener.MessageListener;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
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

    private static final Set<Method> REGISTERED_METHODS = new HashSet<Method>();

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
            String message = String.format("如果想配置成为message listener,方法必须有且只有一个参数,类型必须为java.lang.String类型: %s method:%s", beanName, methodName);
            if (args.length != 1) {
                LOGGER.error(message);
                throw new RuntimeException(message);
            }
            if (args[0] != String.class) {
                LOGGER.error(message);
                throw new RuntimeException(message);
            }

            String topic = annotation.topic();
            if (StringUtils.isEmpty(topic)) {
                String err = String.format("使用@RocketmqConsumer,必须提供topic, class:%s method:%s", beanName, methodName);
                LOGGER.error(err);
                throw new RuntimeException(err);
            }

            String group = annotation.group();
            if (StringUtils.isEmpty(topic)) {
                String err = String.format("使用@RocketmqConsumer,必须提供group, class:%s method:%s", beanName, methodName);
                LOGGER.error(err);
                throw new RuntimeException(err);
            }

            REGISTERED_METHODS.add(method);
            ListenerHolder listenerHolder = new ListenerHolder(bean, method, topic, group);
            listenerHolder.register();
        }
    }

    private class ListenerHolder {

        private final MessageListener listener;

        private final String topic;

        private final String group;

        public ListenerHolder(Object bean, Method method, String topic, String group) {
            this.topic = topic;
            this.group = group;
            this.listener = new GeneratedListener(bean, method, topic, group);
        }

        public void register() {
            createConsumer(this.topic, this.group, listener);
        }

    }

    public void createConsumer(String topic, String group, MessageListener messageListener) {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(this.properties.getGroupName());
        consumer.setNamesrvAddr(this.properties.getNameSrvAddr());
        consumer.setConsumeThreadMin(this.properties.getConsumerConsumeThreadMin());
        consumer.setConsumeThreadMax(this.properties.getConsumerConsumeThreadMax());
        consumer.registerMessageListener((MessageListenerConcurrently) (msgs, context) -> {
            msgs.forEach(messageListener::onMessage);
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        });

        /**
         * 设置Consumer第一次启动是从队列头部开始消费还是队列尾部开始消费 如果非第一次启动，那么按照上次消费的位置继续消费
         */
        // consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);

        /**
         * 设置消费模型，集群还是广播，默认为集群
         */
        consumer.setMessageModel(MessageModel.BROADCASTING);

        /**
         * 设置一次消费消息的条数，默认为1条
         */
        consumer.setConsumeMessageBatchMaxSize(this.properties.getConsumerConsumeMessageBatchMaxSize());
        try {
            consumer.subscribe(topic, "*");
            consumer.start();
        } catch (MQClientException e) {
            LOGGER.error("info consumer title {}", group);
        }

    }

}
