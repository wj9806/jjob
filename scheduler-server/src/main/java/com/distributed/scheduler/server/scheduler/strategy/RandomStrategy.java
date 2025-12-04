package com.distributed.scheduler.server.scheduler.strategy;

import com.distributed.scheduler.client.model.ClientInfo;
import com.distributed.scheduler.client.model.TaskInfo;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * 随机调度策略
 * 从可用客户端中随机选择一个，适合对性能要求较高的场景
 */
public class RandomStrategy extends AbstractClientSelectionStrategy {
    
    private final Random random = new Random();
    
    @Override
    protected String doSelectClient(Map<String, ClientInfo> clients, TaskInfo taskInfo) {
        List<String> clientIds = clients.keySet().stream().collect(Collectors.toList());
        
        if (clientIds.isEmpty()) {
            return null;
        }
        
        // 随机选择客户端
        int randomIndex = random.nextInt(clientIds.size());
        String selectedClientId = clientIds.get(randomIndex);
        
        logger.debug("Random strategy selected client: {} for task: {}", 
                selectedClientId, taskInfo.getTaskName());
        
        return selectedClientId;
    }
    
    @Override
    public String getStrategyName() {
        return "random";
    }
}
