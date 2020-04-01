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

    /**
     * 主题
     * @return
     */
    String topic();

    /**
     * group 可以缺省
     * @return
     */
    String group() default "";

    /**
     * rocketmq的tag,*的时候表示匹配所有
     * @return
     */
    String tag() default "*";

    /**
     * 默认为集群模式
     * @return
     */
    boolean isBroadCasting() default false;

}
