package com.distributed.scheduler.server.scheduler;

import com.distributed.scheduler.client.model.TaskInfo;
import com.distributed.scheduler.client.model.TaskStatus;
import com.distributed.scheduler.client.protocol.Message;
import com.distributed.scheduler.client.protocol.MessageType;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class TaskTrigger {
    private static final Logger logger = LoggerFactory.getLogger(TaskTrigger.class);
    // 保存连接的客户端通道
    private final ConcurrentMap<String, Channel> clientChannels = new ConcurrentHashMap<>();
    // 跟踪正在执行的任务，对于oneRunning=true的任务，只需要跟踪taskId
    private final ConcurrentMap<String, Boolean> runningTasks = new ConcurrentHashMap<>();
    
    /**
     * 触发任务
     */
    public void triggerTask(TaskInfo taskInfo, String clientId) {
        String taskId = taskInfo.getTaskId();
        
        // 为每个任务实例生成唯一的实例ID
        String instanceId = UUID.randomUUID().toString();
        
        // 只有当oneRunning=true时才检查是否有实例正在运行
        if (taskInfo.isOneRunning()) {
            // 检查任务是否正在执行
            if (runningTasks.containsKey(taskId)) {
                logger.debug("Task {} is already running, skipping trigger", taskId);
                return;
            }
            // 标记任务为正在执行
            runningTasks.put(taskId, Boolean.TRUE);
        }
        
        // 增加任务执行次数
        taskInfo.setExecutionCount(taskInfo.getExecutionCount() + 1);
        
        // 创建任务触发消息
        Message message = new Message();
        message.setMessageId(instanceId); // 使用instanceId作为messageId，方便后续跟踪
        message.setType(MessageType.TASK_TRIGGER);
        message.setClientId(clientId);
        message.setData(taskInfo);
        
        // 获取客户端通道并发送消息
        Channel channel = clientChannels.get(clientId);
        if (channel != null && channel.isActive()) {
            logger.debug("Triggering task: {} to client: {}, execution count: {}", taskInfo.getTaskName(), clientId, taskInfo.getExecutionCount());
            channel.writeAndFlush(message);
        } else {
            logger.warn("Cannot trigger task {}: client channel {} is not active", taskInfo.getTaskName(), clientId);
            // 如果发送失败，移除运行标记
            runningTasks.remove(taskId);
        }
    }
    
    /**
     * 任务完成通知，用于清除运行中的任务标记
     */
    public void notifyTaskCompleted(String taskId) {
        // 直接移除运行标记，对于oneRunning=true的任务，这会允许新的实例被触发
        // 对于oneRunning=false的任务，这个操作不会影响并行执行
        if (runningTasks.containsKey(taskId)) {
            runningTasks.remove(taskId);
            logger.debug("Task {} running mark cleared, can be triggered again", taskId);
        } else {
            logger.debug("Task {} was not tracked as running", taskId);
        }
    }
    
    /**
     * 任务完成通知重载版本，根据任务配置决定是否清除运行标记
     */
    public void notifyTaskCompleted(TaskStatus taskStatus) {
        String taskId = taskStatus.getTaskId();
        // 只有当任务配置为oneRunning=true时，才需要清除运行标记
        // 对于oneRunning=false的任务，允许多实例并行执行，不需要跟踪运行状态
        if (taskStatus.isOneRunning()) {
            if (runningTasks.containsKey(taskId)) {
                runningTasks.remove(taskId);
                logger.debug("Task {} running mark cleared based on oneRunning=true, can be triggered again", taskId);
            }
        } else {
            // 对于允许多实例并行执行的任务，不需要清除运行标记
            // 实际上，这类任务可能根本没有被添加到runningTasks中
            logger.debug("Task {} completed, but not clearing running mark due to oneRunning=false", taskId);
        }
    }
    
    /**
     * 检查任务是否正在运行
     */
    public boolean isTaskRunning(String taskId) {
        return runningTasks.containsKey(taskId);
    }

    /**
     * 注册客户端通道
     */
    public void registerClientChannel(String clientId, Channel channel) {
        clientChannels.put(clientId, channel);
        logger.debug("Client channel registered: {}", clientId);
    }
    
    /**
     * 移除客户端通道
     */
    public void removeClientChannel(String clientId) {
        clientChannels.remove(clientId);
        logger.debug("Client channel removed: {}", clientId);
    }
    
}