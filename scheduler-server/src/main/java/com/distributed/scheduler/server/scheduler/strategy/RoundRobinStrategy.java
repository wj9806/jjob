package com.distributed.scheduler.server.scheduler.strategy;

import com.distributed.scheduler.client.model.ClientInfo;
import com.distributed.scheduler.client.model.TaskInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 轮询调度策略
 * 依次选择客户端，实现简单的负载均衡
 */
public class RoundRobinStrategy extends AbstractClientSelectionStrategy {
    
    // 使用原子变量确保线程安全
    private final AtomicInteger counter = new AtomicInteger(0);
    
    @Override
    protected String doSelectClient(Map<String, ClientInfo> clients, TaskInfo taskInfo) {
        List<String> clientIds = new ArrayList<>(clients.keySet());
        
        if (clientIds.isEmpty()) {
            return null;
        }
        
        // 使用轮询算法选择客户端
        int index = Math.abs(counter.getAndIncrement() % clientIds.size());
        String selectedClientId = clientIds.get(index);
        
        logger.debug("RoundRobin strategy selected client: {} for task: {}", 
                selectedClientId, taskInfo.getTaskName());
        
        return selectedClientId;
    }
    
    @Override
    public String getStrategyName() {
        return "roundRobin";
    }
}
