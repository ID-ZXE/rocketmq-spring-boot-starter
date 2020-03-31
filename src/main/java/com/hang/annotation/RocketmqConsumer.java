package com.hang.annotation;

import java.lang.annotation.*;

/**
 * @author hangs.zhang
 * @date 2020/3/30 下午6:41
 * *********************
 * function:
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Inherited
public @interface RocketmqConsumer {

    String topic();

    String group();

}
