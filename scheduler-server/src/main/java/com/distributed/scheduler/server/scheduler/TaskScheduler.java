package com.distributed.scheduler.server.scheduler;

import com.distributed.scheduler.client.model.ClientInfo;
import com.distributed.scheduler.client.model.TaskInfo;
import com.distributed.scheduler.server.manager.ClientManager;
import com.distributed.scheduler.server.scheduler.strategy.ClientSelectionStrategy;
import com.distributed.scheduler.server.scheduler.strategy.ClientSelectionStrategyFactory;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;

@Component
public class TaskScheduler {
    private static final Logger logger = LoggerFactory.getLogger(TaskScheduler.class);
    private final Map<String, TaskInfo> tasks = new ConcurrentHashMap<>();
    private final Map<String, Timeout> taskFutures = new ConcurrentHashMap<>();
    private final HashedWheelTimer scheduler = new HashedWheelTimer(100, TimeUnit.MILLISECONDS, 1024);
    private final ScheduledExecutorService heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();

    @Autowired
    private ClientManager clientManager;
    
    @Autowired
    private TaskTrigger taskTrigger;
    
    @Autowired
    private ClientSelectionStrategyFactory strategyFactory;
    
    @PostConstruct
    public void init() {
        // 启动心跳检测任务
        heartbeatScheduler.scheduleAtFixedRate(() -> {
            clientManager.cleanupTimeoutClients();
        }, 30, 60, TimeUnit.SECONDS);
    }
    
    /**
     * 添加任务并记录注册客户端ID
     */
    public void addTask(TaskInfo taskInfo, String clientId) {
        // 确保初始执行次数为0
        if (taskInfo.getExecutionCount() < 0) {
            taskInfo.setExecutionCount(0);
        }
        
        // 获取或创建任务
        TaskInfo existingTask = tasks.computeIfAbsent(taskInfo.getTaskId(), k -> taskInfo);
        
        // 记录注册客户端
        existingTask.addRegisteredClient(clientId);
        
        // 如果任务已启用且未被调度，则调度任务
        if (existingTask.isEnabled() && !taskFutures.containsKey(existingTask.getTaskId())) {
            scheduleTask(existingTask);
        }
        
        logger.debug("Task registered by client: taskId={}, clientId={}", taskInfo.getTaskId(), clientId);
    }
    
    /**
     * 移除任务
     */
    public void removeTask(String taskId) {
        tasks.remove(taskId);
        
        // 取消调度的任务
        cancelTask(taskId);
        
        logger.info("Task removed: {}", taskId);
    }
    
    /**
     * 取消任务调度但保留任务信息
     */
    public void cancelTask(String taskId) {
        // 取消调度的任务
        Timeout timeout = taskFutures.remove(taskId);
        if (timeout != null) {
            timeout.cancel();
            logger.info("Task schedule cancelled: {}", taskId);
        }
    }
    
    /**
     * 根据客户端ID取消该客户端注册的所有任务
     */
    public void cancelTasksByClientId(String clientId) {
        if (clientId == null) return;
        
        logger.info("Cancelling tasks registered by client: {}", clientId);
        
        // 遍历所有任务
        for (Map.Entry<String, TaskInfo> entry : tasks.entrySet()) {
            TaskInfo taskInfo = entry.getValue();
            
            // 如果任务包含该客户端的注册信息
            if (taskInfo.isClientRegistered(clientId)) {
                // 移除客户端的注册记录
                taskInfo.removeRegisteredClient(clientId);
                
                // 如果任务没有其他注册客户端，取消任务调度
                if (taskInfo.getRegisteredClientCount() == 0) {
                    cancelTask(taskInfo.getTaskId());
                    logger.info("Task cancelled because no clients registered: {}", taskInfo.getTaskId());
                }
            }
        }
    }
    
    /**
     * 调度任务
     */
    private void scheduleTask(TaskInfo taskInfo) {
        String taskId = taskInfo.getTaskId();
        
        try {
            if (!StringUtils.hasText(taskInfo.getCronExpression())) return;
            // 解析cron表达式
            CronExpression cronExpression = CronExpression.parse(taskInfo.getCronExpression());
            
            // 计算下次执行时间
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime nextExecutionTime = cronExpression.next(now);
            
            if (nextExecutionTime == null) {
                logger.warn("Invalid cron expression for task: {}", taskId);
                return;
            }
            
            // 计算延迟时间
            long delay = Duration.between(now, nextExecutionTime).toMillis();
            
            // 调度任务执行
            Timeout timeout = scheduler.newTimeout(new TimerTask() {
                @Override
                public void run(Timeout timeout) throws Exception {
                    try {
                        if (taskInfo.isOneRunning()) {
                            // 检查任务是否已经在运行，如果在运行则不触发
                            if (!taskTrigger.isTaskRunning(taskId)) {
                                invokeTask(taskInfo);
                            }
                        } else {
                            invokeTask(taskInfo);
                        }

                        // 任务执行完成后，重新调度下一次执行
                        scheduleTask(taskInfo);

                    } catch (Exception e) {
                        logger.error("Error scheduling task: {}", taskId, e);
                        // 发生异常时，重新调度下一次执行
                        scheduleTask(taskInfo);
                    }
                }
            }, delay, TimeUnit.MILLISECONDS);
            
            // 保存任务的Timeout对象，以便后续可以取消调度
            taskFutures.put(taskId, timeout);
            
            logger.debug("Task scheduled: {}, next execution at: {}", taskId, nextExecutionTime);
            
        } catch (Exception e) {
            logger.error("Error parsing cron expression for task: {}", taskId, e);
        }
    }

    /**
     * 执行任务调度
     */
    private void invokeTask(TaskInfo taskInfo) {
        // 查找对应分组的客户端
        Map<String, ClientInfo> clientMap = clientManager.getClientsByGroup(taskInfo.getTaskGroup());
        if (!clientMap.isEmpty()) {
            // 获取任务指定的调度策略，如果未指定则使用默认的轮询策略
            String strategyName = taskInfo.getScheduleStrategy();
            if (strategyName == null || strategyName.trim().isEmpty()) {
                strategyName = "roundRobin";
                logger.debug("Task {} did not specify a scheduling strategy, using default: roundRobin", taskInfo.getTaskId());
            }
            
            // 获取对应的调度策略
            ClientSelectionStrategy strategy = strategyFactory.getStrategy(strategyName);
            
            // 使用策略选择客户端
            String clientId = strategy.selectClient(clientMap, taskInfo);
            
            if (clientId != null) {
                // 触发任务
                taskTrigger.triggerTask(taskInfo, clientId);
            } else {
                logger.warn("No client selected for task: {}", taskInfo.getTaskName());
            }
        }
    }

    /**
     * 获取所有任务
     */
    public Map<String, TaskInfo> getAllTasks() {
        return new ConcurrentHashMap<>(tasks);
    }
    
    /**
     * 获取任务总数
     */
    public int getTaskCount() {
        return tasks.size();
    }
    
    /**
     * 获取启用的任务数量
     */
    public int getEnabledTaskCount() {
        return (int) tasks.values().stream().filter(TaskInfo::isEnabled).count();
    }
    
    /**
     * 获取禁用的任务数量
     */
    public int getDisabledTaskCount() {
        return (int) tasks.values().stream().filter(task -> !task.isEnabled()).count();
    }
    
    /**
     * 组件销毁时关闭HashedWheelTimer
     */
    @PreDestroy
    public void destroy() {
        scheduler.stop();
        heartbeatScheduler.shutdown();
        logger.info("TaskScheduler destroyed and resources released");
    }
}