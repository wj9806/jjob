package com.distributed.scheduler.server.scheduler.strategy;

import com.distributed.scheduler.client.model.ClientInfo;
import com.distributed.scheduler.client.model.TaskInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

/**
 * 客户端选择策略抽象基类
 * 提供通用的前置检查和基础功能
 */
public abstract class AbstractClientSelectionStrategy implements ClientSelectionStrategy {
    
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    
    @Override
    public String selectClient(Map<String, ClientInfo> clients, TaskInfo taskInfo) {
        // 前置检查
        if (clients == null || clients.isEmpty()) {
            logger.warn("No clients available for task: {}", taskInfo.getTaskName());
            return null;
        }
        
        if (taskInfo == null) {
            logger.warn("Task info is null");
            return null;
        }
        
        // 实际选择逻辑由子类实现
        return doSelectClient(clients, taskInfo);
    }
    
    /**
     * 实际的客户端选择逻辑，由子类实现
     * 
     * @param clients 可用客户端映射
     * @param taskInfo 当前要调度的任务信息
     * @return 选中的客户端ID
     */
    protected abstract String doSelectClient(Map<String, ClientInfo> clients, TaskInfo taskInfo);
    
    /**
     * 获取客户端ID集合
     * 
     * @param clients 客户端映射
     * @return 客户端ID集合
     */
    protected Set<String> getClientIds(Map<String, ClientInfo> clients) {
        return clients.keySet();
    }
    
    /**
     * 获取客户端信息
     * 
     * @param clients 客户端映射
     * @param clientId 客户端ID
     * @return 客户端信息
     */
    protected ClientInfo getClientInfo(Map<String, ClientInfo> clients, String clientId) {
        return clients.get(clientId);
    }
}
