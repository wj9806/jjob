package com.distributed.scheduler.client;

import com.distributed.scheduler.client.model.TaskInfo;
import com.distributed.scheduler.client.model.TaskStatus;
import com.distributed.scheduler.client.protocol.Message;
import com.distributed.scheduler.client.protocol.MessageType;
import com.distributed.scheduler.client.task.TaskExecutor;
import com.distributed.scheduler.client.task.TaskRegistry;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.Date;
import java.util.concurrent.RejectedExecutionException;

public class ClientHandler extends SimpleChannelInboundHandler<Message> {
    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);
    private final ClientScheduler clientScheduler;
    private final TaskRegistry taskRegistry;
    
    public ClientHandler(ClientScheduler clientScheduler, TaskRegistry taskRegistry) {
        this.clientScheduler = clientScheduler;
        this.taskRegistry = taskRegistry;
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message message) throws Exception {
        switch (message.getType()) {
            case TASK_TRIGGER:
                // 处理任务触发
                handleTaskTrigger(message);
                break;
            case SERVER_RESPONSE:
                // 处理服务端响应
                handleServerResponse(message);
                break;
            default:
                logger.warn("Unknown message type: {}", message.getType());
        }
    }
    
    /**
     * 处理任务触发
     */
    private void handleTaskTrigger(Message message) {
        try {
            TaskInfo taskInfo = (TaskInfo) message.getData();
            String clientId = clientScheduler.getClientInfo().getClientId();
            logger.trace("Task trigger received for: {}", taskInfo.getTaskName());
            
            // 创建任务状态
            TaskStatus status = new TaskStatus();
            status.setTaskId(taskInfo.getTaskId());
            status.setTaskName(taskInfo.getTaskName());
            status.setInstanceId(UUID.randomUUID().toString());
            status.setClientId(clientId);
            status.setStatus(TaskStatus.Status.RUNNING);
            status.setStartTime(new Date());
            status.setOneRunning(taskInfo.isOneRunning());
            
            // 发送任务开始执行状态
            sendTaskStatus(status);
            
            // 检查线程池是否可用
            if (clientScheduler.getTaskExecutorService() == null || 
                clientScheduler.getTaskExecutorService().isShutdown() || 
                clientScheduler.getTaskExecutorService().isTerminated()) {
                logger.error("Task executor service is not available, cannot execute task: {}", 
                    taskInfo.getTaskName());
                
                // 如果线程池不可用，直接标记任务失败
                status.setStatus(TaskStatus.Status.FAILED);
                status.setEndTime(new Date());
                status.setErrorMsg("Task executor service is not available");
                sendTaskStatus(status);
                return;
            }
            
            // 使用线程池异步执行任务，避免阻塞Netty的IO线程
            try {
                clientScheduler.getTaskExecutorService().submit(() -> {
                    String currentThreadName = Thread.currentThread().getName();
                    logger.debug("Starting task execution in thread: {}, Task: {}, Instance: {}", 
                        currentThreadName, taskInfo.getTaskName(), status.getInstanceId());
                        
                    try {
                        executeTask(taskInfo);
                        // 更新状态为成功
                        status.setStatus(TaskStatus.Status.SUCCESS);
                        status.setEndTime(new Date());
                        status.setExecutionTime(status.getEndTime().getTime() - status.getStartTime().getTime());
                        logger.debug("Task execution completed successfully: {}, Instance: {}, Thread: {}", 
                            taskInfo.getTaskName(), status.getInstanceId(), currentThreadName);
                    } catch (Exception e) {
                        // 更新状态为失败
                        status.setStatus(TaskStatus.Status.FAILED);
                        status.setEndTime(new Date());
                        status.setErrorMsg(e.getMessage());
                        status.setExecutionTime(status.getEndTime().getTime() - status.getStartTime().getTime());
                        logger.error("Task execution failed: {}, Instance: {}, Thread: {}", 
                            taskInfo.getTaskName(), status.getInstanceId(), currentThreadName, e);
                    } finally {
                        // 发送任务结束状态
                        logger.debug("Sending final status for task: {}, Instance: {}, Thread: {}", 
                            taskInfo.getTaskName(), status.getInstanceId(), currentThreadName);
                        sendTaskStatus(status);
                    }
                });
            } catch (RejectedExecutionException e) {
                // 处理任务被拒绝的情况
                logger.error("Task rejected by executor service: {}, Error: {}", 
                    taskInfo.getTaskName(), e.getMessage());
                
                status.setStatus(TaskStatus.Status.FAILED);
                status.setEndTime(new Date());
                status.setErrorMsg("Task execution rejected by executor service: " + e.getMessage());
                sendTaskStatus(status);
            }
        } catch (Exception e) {
            // 捕获handleTaskTrigger方法中的所有异常，确保不会影响Netty的事件循环线程
            logger.error("Unexpected error in handleTaskTrigger", e);
        }
    }

    /**
     * 执行任务
     */
    private void executeTask(TaskInfo taskInfo) throws Exception {
        String taskName = taskInfo.getTaskName();
        logger.debug("Executing task: {}", taskName);
        
        long startTime = System.currentTimeMillis();
        
        TaskExecutor executor = null;
        boolean isSingleton = false;
        
        try {
            // 使用TaskRegistry获取任务执行器
            executor = taskRegistry.getTaskExecutor(taskInfo.getTaskId());
            if (executor != null) {
                logger.trace("Executing task using registered executor: {}", executor.getTaskName());
                executor.execute(taskInfo);
                
                long executionTime = System.currentTimeMillis() - startTime;
                logger.debug("Task executed successfully with registered executor: {}, Execution time: {}ms", 
                    taskName, executionTime);
                
                // 检查是否为单例（单例任务会被存储在taskExecutors中）
                isSingleton = taskRegistry.containsTaskExecutor(executor);
            } else {
                // 如果没有找到注册的执行器，使用反射执行
                logger.debug("No registered executor found, using reflection for task: {}", taskName);
                
                // 验证必要的字段
                if (taskInfo.getTargetClass() == null || taskInfo.getTargetClass().trim().isEmpty()) {
                    throw new IllegalArgumentException("Task target class cannot be empty");
                }
                
                if (taskInfo.getTargetMethod() == null || taskInfo.getTargetMethod().trim().isEmpty()) {
                    throw new IllegalArgumentException("Task target method cannot be empty");
                }
                
                try {
                    // 加载类
                    Class<?> targetClass = Class.forName(taskInfo.getTargetClass());
                    Object instance = targetClass.getDeclaredConstructor().newInstance();
                    
                    logger.debug("Executing task: {}.{}", taskInfo.getTargetClass(), taskInfo.getTargetMethod());
                    
                    // 实际执行代码 (这里补充完整之前的注释部分)
                    java.lang.reflect.Method method = targetClass.getMethod(taskInfo.getTargetMethod(), TaskInfo.class);
                    method.invoke(instance, taskInfo);
                    
                    long executionTime = System.currentTimeMillis() - startTime;
                    logger.debug("Task executed successfully with reflection: {}, Execution time: {}ms", 
                        taskName, executionTime);
                } catch (Exception e) {
                    logger.error("Reflection execution failed for task: {}", taskName, e);
                    throw new Exception("Failed to execute task using reflection: " + e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            logger.error("Error executing task: {}", taskName, e);
            throw e;
        } finally {
            // 对于非单例任务，执行完成后需要销毁
            if (executor != null && !isSingleton) {
                try {
                    executor.destroy();
                    logger.debug("Non-singleton task executor destroyed: {}", taskName);
                } catch (Exception e) {
                    logger.error("Failed to destroy non-singleton task executor: {}", taskName, e);
                    // 销毁失败不影响任务执行结果，只记录日志
                }
            }
        }
    }
    
    /**
     * 发送任务状态
     */
    private void sendTaskStatus(TaskStatus status) {
        try {
            Message message = new Message();
            message.setType(MessageType.TASK_STATUS_REPORT);
            message.setClientId(clientScheduler.getClientInfo().getClientId());
            message.setData(status);
            
            // 记录任务状态发送信息，包括任务ID、实例ID和状态
            logger.debug("Sending task status - TaskID: {}, InstanceID: {}, Status: {}, Thread: {}", 
                status.getTaskId(), status.getInstanceId(), status.getStatus(), 
                Thread.currentThread().getName());
            
            // 通过ClientScheduler发送消息
            clientScheduler.sendMessage(message);
            logger.debug("Task status successfully reported: {}, Status: {}, Thread: {}", 
                status.getTaskId(), status.getStatus(), Thread.currentThread().getName());
        } catch (Exception e) {
            logger.error("Failed to send task status for task: {}, Status: {}", 
                status.getTaskId(), status.getStatus(), e);
        }
    }
    
    /**
     * 处理服务端响应
     */
    private void handleServerResponse(Message message) {
        logger.debug("Received server response: {}", message.getData());
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.info("Client channel activated");
        super.channelActive(ctx);
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.warn("Client channel inactive");
        clientScheduler.reconnect();
        super.channelInactive(ctx);
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("Client exception", cause);
        // 不要立即关闭连接，让重连机制处理
        // 只有在连接不可恢复时才关闭
        if (ctx.channel().isActive()) {
            logger.debug("Connection still active, keeping channel open");
        } else {
            logger.debug("Connection already inactive, closing context");
            ctx.close();
        }
    }
}