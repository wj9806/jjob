package com.distributed.scheduler.client.protocol;

import lombok.Data;

import java.io.Serializable;

@Data
public class Message implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String messageId;       // 消息ID
    private MessageType type;       // 消息类型
    private long timestamp;         // 时间戳
    private String clientId;        // 客户端ID
    private Object data;            // 消息数据
    
    public Message() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public Message(MessageType type, Object data) {
        this.type = type;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }
}