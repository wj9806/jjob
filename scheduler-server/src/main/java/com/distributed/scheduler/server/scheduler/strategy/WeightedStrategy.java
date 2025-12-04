package com.distributed.scheduler.server.scheduler.strategy;

import com.distributed.scheduler.client.model.ClientInfo;
import com.distributed.scheduler.client.model.TaskInfo;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 权重调度策略
 * 根据客户端的权重值进行选择，权重越高的客户端被选中的概率越大
 */
public class WeightedStrategy extends AbstractClientSelectionStrategy {
    
    // 默认权重值
    private static final int DEFAULT_WEIGHT = 1;
    
    // 用于权重轮询的计数器
    private final AtomicInteger counter = new AtomicInteger(0);
    
    @Override
    protected String doSelectClient(Map<String, ClientInfo> clients, TaskInfo taskInfo) {
        if (clients.isEmpty()) {
            return null;
        }
        
        // 计算总权重并构建权重映射
        Map<String, Integer> weights = new HashMap<>();
        int totalWeight = 0;
        
        for (Map.Entry<String, ClientInfo> entry : clients.entrySet()) {
            String clientId = entry.getKey();
            ClientInfo clientInfo = entry.getValue();
            // 获取客户端权重，如果未设置则使用默认权重
            int weight = (clientInfo.getWeight() > 0) ? clientInfo.getWeight() : DEFAULT_WEIGHT;
            weights.put(clientId, weight);
            totalWeight += weight;
        }
        
        // 如果所有客户端权重都为0，降级为随机选择
        if (totalWeight == 0) {
            logger.debug("All clients have zero weight, falling back to random selection");
            return selectRandomClient(clients);
        }
        
        // 使用权重轮询算法选择客户端
        String selectedClientId = selectByWeight(weights, totalWeight);
        
        logger.debug("Weighted strategy selected client: {} for task: {}, weight: {}", 
                selectedClientId, taskInfo.getTaskName(), weights.get(selectedClientId));
        
        return selectedClientId;
    }
    
    /**
     * 根据权重选择客户端
     * 使用轮询方式结合权重，确保按比例分配任务
     */
    private String selectByWeight(Map<String, Integer> weights, int totalWeight) {
        // 获取当前计数并取模总权重
        int position = Math.abs(counter.getAndIncrement() % totalWeight);
        
        // 遍历权重映射，根据位置选择客户端
        int currentWeight = 0;
        for (Map.Entry<String, Integer> entry : weights.entrySet()) {
            currentWeight += entry.getValue();
            if (position < currentWeight) {
                return entry.getKey();
            }
        }
        
        // 兜底逻辑，返回第一个客户端
        return weights.keySet().iterator().next();
    }
    
    /**
     * 随机选择客户端（降级策略）
     */
    private String selectRandomClient(Map<String, ClientInfo> clients) {
        List<String> clientIds = new ArrayList<>(clients.keySet());
        if (clientIds.isEmpty()) {
            return null;
        }
        Random random = new Random();
        return clientIds.get(random.nextInt(clientIds.size()));
    }
    
    @Override
    public String getStrategyName() {
        return "weighted";
    }
}
