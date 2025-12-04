package com.distributed.scheduler.client.task;

import com.distributed.scheduler.client.model.TaskInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class TaskRegistry {
    private static final Logger logger = LoggerFactory.getLogger(TaskRegistry.class);
    private final Map<String, TaskExecutor> taskExecutors = new ConcurrentHashMap<>();
    private final Map<String, Class<? extends TaskExecutor>> taskClasses = new ConcurrentHashMap<>();
    private final Map<String, TaskInfo> taskInfos = new ConcurrentHashMap<>();
    
    /**
     * 注册任务
     */
    public TaskInfo registerTask(Class<? extends TaskExecutor> executorClass) {
        try {
            // 创建临时实例用于获取任务信息
            TaskExecutor tempExecutor = executorClass.newInstance();
            
            // 验证任务名称不能为空
            if (tempExecutor.getTaskName() == null || tempExecutor.getTaskName().trim().isEmpty()) {
                throw new IllegalArgumentException("Task name cannot be null or empty");
            }
            
            // 验证任务分组不能为空
            if (tempExecutor.getTaskGroup() == null || tempExecutor.getTaskGroup().trim().isEmpty()) {
                throw new IllegalArgumentException("Task group cannot be null or empty");
            }
            
            // 使用任务名称和分组的组合作为任务ID，确保不同实例上的同一任务生成相同的taskId
            String taskId = generateTaskId(tempExecutor.getTaskName(), tempExecutor.getTaskGroup());
            
            // 检查任务名称是否在同一分组内重复
            if (taskInfos.containsKey(taskId)) {
                throw new IllegalArgumentException("Task with name '" + tempExecutor.getTaskName() + "' already exists in group '" + tempExecutor.getTaskGroup() + "'");
            }
            
            TaskInfo taskInfo = new TaskInfo();
            taskInfo.setTaskId(taskId);
            taskInfo.setTargetClass(executorClass.getName());
            taskInfo.setTargetMethod("execute");
            
            // 从接口方法获取任务信息
            taskInfo.setTaskName(tempExecutor.getTaskName());
            taskInfo.setTaskGroup(tempExecutor.getTaskGroup());
            taskInfo.setCronExpression(tempExecutor.getCronExpression());
            taskInfo.setEnabled(true);
            
            // 保存任务执行器类信息
            taskClasses.put(taskId, executorClass);
            
            // 对于单例任务，创建实例并初始化
            if (tempExecutor.isSingleton()) {
                TaskExecutor executor = tempExecutor;
                try {
                    // 调用任务执行器的初始化方法
                    executor.init();
                    logger.debug("Singleton task executor initialized: {} in group: {}", taskInfo.getTaskName(), taskInfo.getTaskGroup());
                    taskExecutors.put(taskId, executor);
                } catch (Exception e) {
                    logger.error("Failed to initialize singleton task executor: {} in group: {}", taskInfo.getTaskName(), taskInfo.getTaskGroup(), e);
                    throw new RuntimeException("Failed to initialize singleton task executor", e);
                }
            }
            
            taskInfos.put(taskId, taskInfo);
            
            logger.info("Task registered: {} in group: {}, TaskId: {}, Singleton: {}", 
                taskInfo.getTaskName(), taskInfo.getTaskGroup(), taskId, tempExecutor.isSingleton());
            return taskInfo;
        } catch (InstantiationException | IllegalAccessException e) {
            logger.error("Failed to create task executor instance: {}", e.getMessage());
            throw new RuntimeException("Failed to create task executor instance", e);
        }
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
        TaskExecutor executor = taskExecutors.remove(taskId);
        taskClasses.remove(taskId);
        taskInfos.remove(taskId);
        
        // 只销毁单例任务的执行器实例
        if (executor != null) {
            try {
                // 调用任务执行器的销毁方法
                executor.destroy();
                logger.debug("Singleton task executor destroyed: {}", taskId);
            } catch (Exception e) {
                logger.error("Failed to destroy singleton task executor: {}", taskId, e);
            }
        }
        
        logger.info("Task unregistered: {}", taskId);
    }
    
    /**
     * 获取任务执行器
     */
    public TaskExecutor getTaskExecutor(String taskId) throws Exception {
        TaskExecutor executor = taskExecutors.get(taskId);
        
        // 如果是单例且已存在，直接返回
        if (executor != null) {
            return executor;
        }
        
        // 对于非单例任务，每次调用都创建新实例并初始化
        Class<? extends TaskExecutor> executorClass = taskClasses.get(taskId);
        if (executorClass != null) {
            executor = executorClass.newInstance();
            try {
                executor.init();
                logger.debug("Non-singleton task executor initialized: {}", taskId);
            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize non-singleton task executor", e);
            }
        }
        
        return executor;
    }

    /**
     * 判断任务执行器是否已存在
     */
    public boolean containsTaskExecutor(TaskExecutor executor) {
        return taskExecutors.containsValue(executor);
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
    
    /**
     * 获取所有任务执行器
     */
    public Map<String, TaskExecutor> getAllTaskExecutors() {
        return new ConcurrentHashMap<>(taskExecutors);
    }
}