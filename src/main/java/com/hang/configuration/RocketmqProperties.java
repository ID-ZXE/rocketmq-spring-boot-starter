package com.hang.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author hangs.zhang
 * @date 2020/03/30 23:11
 * *****************
 * function:
 */
@Data
@ConfigurationProperties(prefix = "rocketmq")
public class RocketmqProperties {

    private boolean isEnable;

    private String nameSrvAddr;

    private String groupName;

    private int producerMaxMessageSize = 1024;

    private int producerSendMsgTimeout = 2000;

    private int producerRetryTimesWhenSendFailed = 2;

    private int consumerConsumeThreadMin = 5;

    private int consumerConsumeThreadMax = 30;

    private int consumerConsumeMessageBatchMaxSize = 1;

}
