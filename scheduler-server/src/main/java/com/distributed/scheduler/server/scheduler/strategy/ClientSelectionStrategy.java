package com.distributed.scheduler.server.scheduler.strategy;

import com.distributed.scheduler.client.model.ClientInfo;
import com.distributed.scheduler.client.model.TaskInfo;

import java.util.Map;

/**
 * 客户端选择策略接口
 * 定义不同调度策略的共同行为
 */
public interface ClientSelectionStrategy {
    
    /**
     * 根据任务信息从可用客户端中选择一个客户端
     * 
     * @param clients 可用客户端映射，key为clientId，value为ClientInfo
     * @param taskInfo 当前要调度的任务信息
     * @return 选中的客户端ID，如果没有可用客户端则返回null
     */
    String selectClient(Map<String, ClientInfo> clients, TaskInfo taskInfo);
    
    /**
     * 获取策略名称
     * 
     * @return 策略名称
     */
    String getStrategyName();
}
