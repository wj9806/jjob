package com.distributed.scheduler.client.task;

import com.distributed.scheduler.client.model.TaskInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class TaskRegistry {
    private static final Logger logger = LoggerFactory.getLogger(TaskRegistry.class);
    private final Map<String, TaskExecutor> taskExecutors = new ConcurrentHashMap<>();
    private final Map<String, TaskInfo> taskInfos = new ConcurrentHashMap<>();
    
    /**
     * 注册任务
     */
    public void registerTask(TaskExecutor executor) {
        // 验证任务名称不能为空
        if (executor.getTaskName() == null || executor.getTaskName().trim().isEmpty()) {
            throw new IllegalArgumentException("Task name cannot be null or empty");
        }
        
        // 验证任务分组不能为空
        if (executor.getTaskGroup() == null || executor.getTaskGroup().trim().isEmpty()) {
            throw new IllegalArgumentException("Task group cannot be null or empty");
        }
        
        // 使用任务名称和分组的组合作为任务ID，确保不同实例上的同一任务生成相同的taskId
        String taskId = generateTaskId(executor.getTaskName(), executor.getTaskGroup());
        
        // 检查任务名称是否在同一分组内重复
        if (taskInfos.containsKey(taskId)) {
            throw new IllegalArgumentException("Task with name '" + executor.getTaskName() + "' already exists in group '" + executor.getTaskGroup() + "'");
        }
        
        TaskInfo taskInfo = new TaskInfo();
        taskInfo.setTaskId(taskId);
        taskInfo.setTargetClass(executor.getClass().getName());
        taskInfo.setTargetMethod("execute");
        
        // 从接口方法获取任务信息
        taskInfo.setTaskName(executor.getTaskName());
        taskInfo.setTaskGroup(executor.getTaskGroup());
        taskInfo.setCronExpression(executor.getCronExpression());
        taskInfo.setEnabled(true);
        
        taskExecutors.put(taskInfo.getTaskId(), executor);
        taskInfos.put(taskInfo.getTaskId(), taskInfo);
        
        logger.info("Task registered: {} in group: {}, TaskId: {}", taskInfo.getTaskName(), taskInfo.getTaskGroup(), taskId);
    }
    
    /**
     * 根据任务名称和分组生成唯一的任务ID
     * @param taskName 任务名称
     * @param taskGroup 任务分组
     * @return 生成的任务ID
     */
    private String generateTaskId(String taskName, String taskGroup) {
        // 使用任务名称和分组的组合作为唯一标识，确保相同任务在不同实例上生成相同的taskId
        return taskName + "_" + taskGroup;
    }
    
    /**
     * 注销任务
     */
    public void unregisterTask(String taskId) {
        taskExecutors.remove(taskId);
        taskInfos.remove(taskId);
        logger.info("Task unregistered: {}", taskId);
    }
    
    /**
     * 获取任务执行器
     */
    public TaskExecutor getTaskExecutor(String taskId) {
        return taskExecutors.get(taskId);
    }
    
    /**
     * 获取任务信息
     */
    public TaskInfo getTaskInfo(String taskId) {
        return taskInfos.get(taskId);
    }
    
    /**
     * 获取所有任务信息
     */
    public Map<String, TaskInfo> getAllTaskInfos() {
        return new ConcurrentHashMap<>(taskInfos);
    }
}