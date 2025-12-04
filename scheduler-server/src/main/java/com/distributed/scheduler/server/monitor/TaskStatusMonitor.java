package com.distributed.scheduler.server.monitor;

import com.distributed.scheduler.client.model.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TaskStatusMonitor {
    private static final Logger logger = LoggerFactory.getLogger(TaskStatusMonitor.class);
    private final Map<String, TaskStatus> taskStatusMap = new ConcurrentHashMap<>();
    
    /**
     * 更新任务状态
     */
    public void updateTaskStatus(TaskStatus status) {
        taskStatusMap.put(status.getInstanceId(), status);
        logger.debug("Task status updated: {} - {}", status.getTaskId(), status.getStatus());
    }
    
    /**
     * 获取任务状态
     */
    public TaskStatus getTaskStatus(String instanceId) {
        return taskStatusMap.get(instanceId);
    }
    
    /**
     * 获取任务的所有实例状态
     */
    public Map<String, TaskStatus> getTaskInstances(String taskId) {
        Map<String, TaskStatus> instances = new ConcurrentHashMap<>();
        taskStatusMap.forEach((instanceId, status) -> {
            if (status.getTaskId().equals(taskId)) {
                instances.put(instanceId, status);
            }
        });
        return instances;
    }
    
    /**
     * 清理过期的任务状态记录
     */
    public void cleanupOldStatus(long keepTimeMillis) {
        long currentTime = System.currentTimeMillis();
        taskStatusMap.entrySet().removeIf(entry -> {
            TaskStatus status = entry.getValue();
            return status.getEndTime() != null && 
                   currentTime - status.getEndTime().getTime() > keepTimeMillis;
        });
    }
}