package com.hang.annotation;

import com.hang.listener.MessageListener;
import javassist.*;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author hangs.zhang
 * @date 2020/3/30 下午7:14
 * *********************
 * function:
 */
public class GeneratedListener implements MessageListener {

    private static final AtomicInteger INDEX = new AtomicInteger();

    private final MessageListener LISTENER;

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    GeneratedListener(Object bean, Method method) {
        Class<?> targetClass = AopUtils.getTargetClass(bean);
        try {
            Class<?> listenerClass = compile(bean, method, targetClass);
            Constructor<?> constructor = listenerClass.getConstructor(targetClass);
            LISTENER = (MessageListener) constructor.newInstance(bean);
        } catch (Exception e) {
            throw new RuntimeException("init generate listener error");
        }
    }

    private Class<?> compile(Object bean, Method method, Class<?> targetClass) throws NotFoundException, CannotCompileException {
        int current = INDEX.incrementAndGet();
        String generateListenerClassName = "RocketMQListener" + current;
        ClassLoader classLoader = bean.getClass().getClassLoader();

        ClassPool pool = new ClassPool(true);
        pool.appendClassPath(new LoaderClassPath(classLoader));

        CtClass ctClass = pool.makeClass(generateListenerClassName);
        ctClass.addInterface(pool.get(MessageListener.class.getName()));
        String fullName = targetClass.getCanonicalName();
        ctClass.addField(CtField.make("private " + fullName + " target=null;\n", ctClass));
        ctClass.addConstructor(CtNewConstructor.make("public " + generateListenerClassName + "(" + fullName + " target){this.target=target;}\n", ctClass));
        ctClass.addMethod(CtMethod.make("public void onMessage(java.lang.String message){this.target." + method.getName() + "(message);}\n", ctClass));
        return ctClass.toClass(classLoader, null);
    }

    @Override
    public void onMessage(Message message) {
        this.LISTENER.onMessage(message);
    }

}
