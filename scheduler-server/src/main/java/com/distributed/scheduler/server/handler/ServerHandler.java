package com.distributed.scheduler.server.handler;

import com.distributed.scheduler.client.model.ClientInfo;
import com.distributed.scheduler.client.model.TaskInfo;
import com.distributed.scheduler.client.model.TaskStatus;
import com.distributed.scheduler.client.protocol.Message;
import com.distributed.scheduler.client.protocol.MessageType;
import com.distributed.scheduler.server.manager.ClientManager;
import com.distributed.scheduler.server.monitor.TaskStatusMonitor;
import com.distributed.scheduler.server.scheduler.TaskScheduler;
import com.distributed.scheduler.server.scheduler.TaskTrigger;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@Sharable
public class ServerHandler extends SimpleChannelInboundHandler<Message> {
    
    private static final Logger logger = LoggerFactory.getLogger(ServerHandler.class);
    
    @Autowired
    private ClientManager clientManager;
    
    @Autowired
    private TaskStatusMonitor taskStatusMonitor;
    
    @Autowired
    private TaskTrigger taskTrigger;
    
    @Autowired
    private ApplicationContext applicationContext;
    
    @Autowired
    private TaskScheduler taskScheduler;
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message message) throws Exception {
        switch (message.getType()) {
            case CLIENT_REGISTER:
                handleClientRegister(ctx, message);
                break;
            case CLIENT_HEARTBEAT:
                handleClientHeartbeat(ctx, message);
                break;
            case CLIENT_UNREGISTER:
                handleClientUnregister(ctx, message);
                break;
            case TASK_STATUS_REPORT:
                handleTaskStatusReport(ctx, message);
                break;
            case TASK_RESULT_REPORT:
                handleTaskResultReport(ctx, message);
                break;
            default:
                logger.warn("Unknown message type: {}", message.getType());
        }
    }
    
    /**
     * 处理客户端注册
     */
    private void handleClientRegister(ChannelHandlerContext ctx, Message message) {
        ClientInfo clientInfo = (ClientInfo) message.getData();
        ChannelId channelId = ctx.channel().id();
        String clientId = clientInfo.getClientId();
        
        // 从连接中获取客户端IP地址
        if (clientInfo.getIpAddress() == null) {
            String clientIp = ctx.channel().remoteAddress().toString().split(":")[0].replace("/", "");
            clientInfo.setIpAddress(clientIp);
        }

        // 注册客户端
        clientManager.registerClient(channelId.asLongText(), clientInfo);
        // 使用clientId注册通道，而不是channelId
        taskTrigger.registerClientChannel(clientId, ctx.channel());
        
        logger.info("Client registered: {} from {} ({})", clientId, clientInfo.getHostName(), clientInfo.getIpAddress());
        
        // 发送响应
        sendResponse(ctx, "Client registered successfully");
    }
    
    /**
     * 处理客户端心跳
     */
    private void handleClientHeartbeat(ChannelHandlerContext ctx, Message message) {
        String clientId = message.getClientId();
        if (clientId != null) {
            logger.debug("Heartbeat received from client: {}", clientId);
            clientManager.updateHeartbeat(clientId);
        }
    }
    
    /**
     * 处理客户端注销
     */
    private void handleClientUnregister(ChannelHandlerContext ctx, Message message) {
        String clientId = message.getClientId();
        
        // 取消该客户端注册的所有任务
        taskScheduler.cancelTasksByClientId(clientId);
        
        clientManager.unregisterClient(clientId);
        // 使用clientId移除通道
        taskTrigger.removeClientChannel(clientId);
        
        logger.info("Client unregistered: {}, tasks cancelled", clientId);
        ctx.close();
    }
    
    /**
     * 处理任务状态报告
     */
    private void handleTaskStatusReport(ChannelHandlerContext ctx, Message message) {
        Object data = message.getData();
        if (data instanceof TaskStatus) {
            TaskStatus status = (TaskStatus) data;
            taskStatusMonitor.updateTaskStatus(status);
            logger.debug("Task status updated: {} - {}", status.getTaskId(), status.getStatus());
            
            // 如果任务执行成功，更新任务执行次数
            if (status.getStatus() == TaskStatus.Status.SUCCESS) {
                String clientId = status.getClientId();
                if (clientId != null) {
                    ClientInfo clientInfo = clientManager.getClient(clientId);
                    if (clientInfo != null) {
                        clientInfo.setTaskExecutionCount(clientInfo.getTaskExecutionCount() + 1);
                        logger.debug("Client {} task execution count updated to {}",
                                clientId, clientInfo.getTaskExecutionCount());
                    }
                }
            }
            
            // 当任务执行完成时（无论成功或失败），使用新的notifyTaskCompleted方法
            // 传递完整的TaskStatus对象，该方法会根据任务的oneRunning配置决定是否清除运行标记
            // 这样对于oneRunning=false的任务，允许多实例并行执行
            if (status.getStatus() == TaskStatus.Status.SUCCESS || 
                status.getStatus() == TaskStatus.Status.FAILED) {
                taskTrigger.notifyTaskCompleted(status);
                logger.debug("Task {} completed with status: {}", status.getTaskId(), status.getStatus());
            }
        } else if (data instanceof TaskInfo) {
            // 处理任务注册
            TaskInfo taskInfo = (TaskInfo) data;
            String clientId = message.getClientId();
            logger.info("Task registered: {} from client: {}", taskInfo.getTaskName(), clientId);
            // 将任务添加到任务调度器，并传递客户端ID
            TaskScheduler taskScheduler = applicationContext.getBean(TaskScheduler.class);
            taskScheduler.addTask(taskInfo, clientId);
        }
    }
    
    /**
     * 处理任务结果报告
     */
    private void handleTaskResultReport(ChannelHandlerContext ctx, Message message) {
        // 处理任务执行结果
        String clientId = message.getClientId();
        logger.info("Task result received: {} from client {}", message.getData(), clientId);
        
        // 注意：任务执行次数的更新已在handleTaskStatusReport方法中处理
        // 当任务状态为SUCCESS时会更新计数，避免重复计数
    }
    
    /**
     * 发送响应消息
     */
    private void sendResponse(ChannelHandlerContext ctx, Object data) {
        Message response = new Message();
        response.setType(MessageType.SERVER_RESPONSE);
        response.setTimestamp(new Date().getTime());
        response.setData(data);
        ctx.writeAndFlush(response);
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // 客户端断开连接时清理资源
        String channelId = ctx.channel().id().asLongText();
        // 查找对应的客户端ID并注销
        String clientId = clientManager.findClientIdByChannelId(channelId);
        if (clientId != null) {
            clientManager.unregisterClient(clientId);
            // 使用clientId移除通道
            taskTrigger.removeClientChannel(clientId);
            logger.info("Client disconnected: {}", clientId);
        }
        super.channelInactive(ctx);
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("Exception caught in server handler", cause);
        ctx.close();
    }
}