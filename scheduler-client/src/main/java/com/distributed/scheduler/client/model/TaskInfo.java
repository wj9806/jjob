package com.distributed.scheduler.client.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String taskId;           // 任务唯一标识
    private String taskName;         // 任务名称
    private String taskGroup;        // 任务分组
    private String cronExpression;   // 定时表达式
    private String targetClass;      // 目标执行类
    private String targetMethod;     // 目标执行方法
    private Map<String, Object> params; // 任务参数
    private boolean enabled;         // 是否启用
    private String description;      // 任务描述
    private int executionCount;      // 任务执行次数
    private boolean oneRunning = false;  // 是否只允许一个任务实例运行，默认为false表示允许多实例并行执行
    private String scheduleStrategy; // 调度策略，可选值：roundRobin, random, weighted
    private Set<String> registeredClients = new CopyOnWriteArraySet<>(); // 注册该任务的客户端ID集合
    
    /**
     * 添加注册客户端
     */
    public void addRegisteredClient(String clientId) {
        registeredClients.add(clientId);
    }
    
    /**
     * 移除注册客户端
     */
    public void removeRegisteredClient(String clientId) {
        registeredClients.remove(clientId);
    }
    
    /**
     * 检查客户端是否已注册该任务
     */
    public boolean isClientRegistered(String clientId) {
        return registeredClients.contains(clientId);
    }
    
    /**
     * 获取注册客户端数量
     */
    public int getRegisteredClientCount() {
        return registeredClients.size();
    }
}