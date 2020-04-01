package com.hang.configuration;

import com.hang.annotation.ConsumerAnnotationScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

import javax.annotation.Resource;

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
    private RocketmqProperties rocketmqProperties;

    static final String ROCKETMQ_CLIENT_ANNOTATION = "ROCKETMQ_CLIENT_ANNOTATION";

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

}
