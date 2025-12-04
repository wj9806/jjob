package com.distributed.scheduler.client.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClientInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String clientId;        // 客户端唯一标识
    private String hostName;        // 客户端主机名 (与前端一致)
    private String ipAddress;       // 客户端IP地址
    private int port;               // 客户端端口
    private String group;           // 客户端分组
    private String applicationName; // 应用名称
    private long registrationTime;  // 注册时间 (与前端一致)
    private long lastHeartbeatTime; // 最后心跳时间 (与前端一致)
    private int taskExecutionCount; // 任务执行次数
    private int weight = 1;         // 权重
    private boolean online = true;  // 在线状态
    
    public ClientInfo(String host, int port, String group, String applicationName) {
        this.clientId = UUID.randomUUID().toString();
        this.hostName = host;
        this.ipAddress = "";
        this.port = port;
        this.group = group;
        this.applicationName = applicationName;
        this.registrationTime = System.currentTimeMillis();
        this.lastHeartbeatTime = System.currentTimeMillis();
        this.taskExecutionCount = 0;
        this.online = true; // 默认在线
    }
    
    public int getTaskExecutionCount() {
        return taskExecutionCount;
    }
    
    public void setTaskExecutionCount(int taskExecutionCount) {
        this.taskExecutionCount = taskExecutionCount;
    }
}