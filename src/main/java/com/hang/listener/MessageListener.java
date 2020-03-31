package com.hang.listener;

import org.apache.rocketmq.common.message.Message;

/**
 * @author hangs.zhang
 * @date 2020/3/30 下午7:13
 * *********************
 * function:
 */
public interface MessageListener {

    void onMessage(Message message);

}
