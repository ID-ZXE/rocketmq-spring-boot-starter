package com.hang.configuration;

import com.hang.annotation.ConsumerAnnotationScanner;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;

/**
 * @author hangs.zhang
 * @date 2020/03/30 23:17
 * *****************
 * function:
 */
@Configuration
@EnableConfigurationProperties({RocketmqProperties.class})
@Import(RocketmqAutoConfiguration.Register.class)
public class RocketmqAutoConfiguration {

    @Resource
    private RocketmqProperties properties;

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String ROCKETMQ_CLIENT_ANNOTATION = "ROCKETMQ_CLIENT_ANNOTATION";

    static class Register implements ImportBeanDefinitionRegistrar {

        @Override
        public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
            if (!registry.containsBeanDefinition(ROCKETMQ_CLIENT_ANNOTATION)) {
                GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
                beanDefinition.setBeanClass(ConsumerAnnotationScanner.class);
                registry.registerBeanDefinition(ROCKETMQ_CLIENT_ANNOTATION, beanDefinition);
            }
        }
    }

    /**
     * 注入一个默认的消费者
     *
     * @return
     * @throws MQClientException
     */
    @Bean
    public DefaultMQProducer defaultMQProducer() throws MQClientException {
        if (StringUtils.isEmpty(properties.getGroupName())) {
            throw new MQClientException(-1, "groupName is blank");
        }

        if (StringUtils.isEmpty(properties.getNameSrvAddr())) {
            throw new MQClientException(-1, "nameServerAddr is blank");
        }
        DefaultMQProducer producer;
        producer = new DefaultMQProducer(properties.getGroupName());

        producer.setNamesrvAddr(properties.getNameSrvAddr());
        // producer.setCreateTopicKey("AUTO_CREATE_TOPIC_KEY");

        // 如果需要同一个jvm中不同的producer往不同的mq集群发送消息，需要设置不同的instanceName
        // producer.setInstanceName(instanceName);
        producer.setMaxMessageSize(properties.getProducerMaxMessageSize());
        producer.setSendMsgTimeout(properties.getProducerSendMsgTimeout());
        // 如果发送消息失败，设置重试次数，默认为2次
        producer.setRetryTimesWhenSendFailed(properties.getProducerRetryTimesWhenSendFailed());

        try {
            producer.start();
            LOGGER.info("producer is start ! groupName:{},namesrvAddr:{}", properties.getGroupName(),
                    properties.getNameSrvAddr());
        } catch (MQClientException e) {
            LOGGER.error("producer start error", e);
            throw e;
        }
        return producer;
    }

}
