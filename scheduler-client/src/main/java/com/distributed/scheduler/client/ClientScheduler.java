package com.distributed.scheduler.client;

import com.distributed.scheduler.client.model.ClientInfo;
import com.distributed.scheduler.client.model.TaskInfo;
import com.distributed.scheduler.client.protocol.Message;
import com.distributed.scheduler.client.protocol.MessageType;
import com.distributed.scheduler.client.task.TaskExecutor;
import com.distributed.scheduler.client.task.TaskRegistry;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientScheduler {
    private static final Logger logger = LoggerFactory.getLogger(ClientScheduler.class);
    @Getter
    private ClientInfo clientInfo;
    private Channel serverChannel;
    private final NioEventLoopGroup group = new NioEventLoopGroup(1);
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ScheduledExecutorService heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();
    // 任务执行线程池，用于并行执行任务
    private ThreadPoolExecutor taskExecutorService;
    private boolean started = false;
    private final TaskRegistry taskRegistry = new TaskRegistry();
    private boolean initialized = false; // 标记是否已调用init方法
    
    private String serverHost;
    private int serverPort;
    private String clientGroup;
    private String applicationName;
    
    // 线程池默认配置参数
    private static final int CORE_POOL_SIZE = Runtime.getRuntime().availableProcessors() * 2;
    private static final int MAX_POOL_SIZE = Runtime.getRuntime().availableProcessors() * 4;
    private static final int QUEUE_CAPACITY = 100;
    private static final long KEEP_ALIVE_TIME = 60L;
    
    /**
     * 默认构造函数
     */
    public ClientScheduler() {
        // 默认构造函数
        // 初始化任务执行线程池
        this.taskExecutorService = createTaskExecutorService();
    }
    
    /**
     * Initialize client, support annotation scanning
     */
    public void init(String serverHost, int serverPort, String clientGroup, String applicationName) {
        if (initialized) {
            logger.warn("ClientScheduler has already been initialized");
            return;
        }
        
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.clientGroup = clientGroup;
        this.applicationName = applicationName;
        
        try {
            // 初始化客户端信息
            InetAddress localHost = InetAddress.getLocalHost();
            String hostName = localHost.getHostName();
            String ipAddress = localHost.getHostAddress();
            
            // 使用正确的构造函数
            this.clientInfo = new ClientInfo(hostName, 0, clientGroup, applicationName);
            this.clientInfo.setIpAddress(ipAddress);
            
            logger.info("Client initialized: {} ({})", clientInfo.getClientId(), clientInfo.getIpAddress());
            
            // 标记为已初始化
            this.initialized = true;
            
        } catch (UnknownHostException e) {
            logger.error("Failed to initialize client info", e);
            throw new RuntimeException("Failed to initialize client info", e);
        }
    }
    
    /**
     * Start client
     */
    public void start() {
        if (started) {
            return;
        }
        
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            // 添加编解码器和处理器
                            ch.pipeline().addLast(
                                    new ObjectEncoder(),
                                    new ObjectDecoder(Integer.MAX_VALUE, ClassResolvers.weakCachingConcurrentResolver(this.getClass().getClassLoader())),
                                    new ClientHandler(ClientScheduler.this, taskRegistry)
                            );
                        }
                    });
            
            connect(bootstrap);
            
            // 启动心跳
            startHeartbeat();
            
            // 启动重连机制
            startReconnectTask(bootstrap);
            
            started = true;
            logger.info("Client scheduler started");
        } catch (Exception e) {
            logger.error("Failed to start client: {}", e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 连接到服务器
     */
    private void connect(Bootstrap bootstrap) {
        try {
            // Connect to server
            ChannelFuture future = bootstrap.connect(serverHost, serverPort).sync();
            serverChannel = future.channel();
            logger.info("Connected to server: {}:{}", serverHost, serverPort);
            
            // Send registration message
            register();
            
            // 注册任务到服务端
            registerTasks();
            
            // Add channel close listener
            future.channel().closeFuture().addListener(f -> {
                logger.warn("Disconnected from server, will try to reconnect...");
                serverChannel = null;
            });
        } catch (Exception e) {
            logger.error("Failed to connect to server: {}", e.getMessage());
        }
    }
    
    /**
     * 启动重连任务
     */
    private void startReconnectTask(Bootstrap bootstrap) {
        logger.debug("Starting reconnect task");
        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (started && (serverChannel == null || !serverChannel.isActive())) {
                    logger.info("Trying to reconnect to server: {}:{}", serverHost, serverPort);
                    connect(bootstrap);
                }
            } catch (Exception e) {
                logger.warn("Reconnect failed: {}", e.getMessage());
            }
        }, 5, 10, TimeUnit.SECONDS); // 5秒后开始重连，之后每10秒尝试一次
    }
    
    /**
     * 注册到服务器
     */
    private void register() {
        if (serverChannel != null && serverChannel.isActive()) {
            Message message = new Message();
            message.setType(MessageType.CLIENT_REGISTER);
            message.setClientId(clientInfo.getClientId());
            message.setData(clientInfo);
            serverChannel.writeAndFlush(message);
        }
    }
    
    /**
     * 启动心跳
     */
    private void startHeartbeat() {
        heartbeatScheduler.scheduleAtFixedRate(this::sendHeartbeat, 10, 30, TimeUnit.SECONDS);
    }
    
    /**
     * 发送心跳
     */
    private void sendHeartbeat() {
        if (serverChannel != null && serverChannel.isActive()) {
            try {
                logger.debug("Sending heartbeat..." + clientInfo.getClientId());
                Message message = new Message();
                message.setType(MessageType.CLIENT_HEARTBEAT);
                message.setClientId(clientInfo.getClientId());
                serverChannel.writeAndFlush(message);
            } catch (Exception e) {
                logger.warn("Failed to send heartbeat, will try to reconnect: {}", e.getMessage());
                // 如果发送心跳失败，立即尝试重连
                serverChannel = null;
                reconnectIfNeeded();
            }
        } else {
            logger.warn("Server channel is not active, will try to reconnect");
            // 如果通道不活跃，立即尝试重连
            reconnectIfNeeded();
        }
    }
    
    /**
     * 立即尝试重连
     */
    private void reconnectIfNeeded() {
        if (started && (serverChannel == null || !serverChannel.isActive())) {
            logger.info("Attempting immediate reconnect to server: {}:{}", serverHost, serverPort);
            try {
                Bootstrap bootstrap = new Bootstrap();
                bootstrap.group(group)
                        .channel(NioSocketChannel.class)
                        .handler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) throws Exception {
                                ch.pipeline().addLast(
                                        new ObjectEncoder(),
                                        new ObjectDecoder(Integer.MAX_VALUE, ClassResolvers.weakCachingConcurrentResolver(this.getClass().getClassLoader())),
                                        new ClientHandler(ClientScheduler.this, taskRegistry)
                                );
                            }
                        });
                connect(bootstrap);
            } catch (Exception e) {
                logger.warn("Immediate reconnect failed, will try again later: {}", e.getMessage());
            }
        }
    }
    
    /**
     * 注册任务到服务端
     */
    private void registerTasks() {
        for (TaskInfo taskInfo : taskRegistry.getAllTaskInfos().values()) {
            Message message = new Message();
            message.setType(MessageType.TASK_STATUS_REPORT); // 使用这个类型进行任务注册，服务端已修改支持
            message.setClientId(clientInfo.getClientId());
            message.setData(taskInfo);
            sendMessage(message);
            logger.debug("Task registered to server: {}", taskInfo.getTaskName());
        }
    }
    
    /**
     * 注册任务执行器
     */
    public void registerTask(TaskExecutor executor) throws Exception {
        if (executor == null) {
            throw new IllegalArgumentException("Task executor cannot be null");
        }
        
        // 验证任务名称不能为空
        if (executor.getTaskName() == null || executor.getTaskName().trim().isEmpty()) {
            throw new IllegalArgumentException("Task name cannot be null or empty");
        }
        
        // 验证任务分组不能为空
        if (executor.getTaskGroup() == null || executor.getTaskGroup().trim().isEmpty()) {
            throw new IllegalArgumentException("Task group cannot be null or empty");
        }
        
        TaskInfo taskInfo = new TaskInfo();
        // 使用任务名称和分组的组合作为任务ID，确保不同实例上的同一任务生成相同的taskId
        String taskId = executor.getTaskName() + "_" + executor.getTaskGroup();
        taskInfo.setTaskId(taskId);
        taskInfo.setTaskName(executor.getTaskName());
        taskInfo.setTaskGroup(executor.getTaskGroup());
        taskInfo.setOneRunning(false);
        taskInfo.setCronExpression(executor.getCronExpression());
        taskInfo.setEnabled(true);
        
        taskRegistry.registerTask(executor);
        logger.info("Task registered locally: {}", executor.getTaskName());
        
        // 如果已连接到服务器，立即注册到服务器
        if (serverChannel != null && serverChannel.isActive()) {
            Message message = new Message();
            message.setType(MessageType.TASK_STATUS_REPORT);
            message.setClientId(clientInfo.getClientId());
            message.setData(taskInfo);
            sendMessage(message);
        }
    }
    
    /**
     * 根据执行器查找任务信息
     */
    private TaskInfo findTaskInfoByExecutor(TaskExecutor executor) {
        // 尝试通过任务名称查找
        for (TaskInfo info : taskRegistry.getAllTaskInfos().values()) {
            if (info.getTaskName().equals(executor.getTaskName())) {
                return info;
            }
        }
        return null;
    }
    
    /**
     * 停止客户端
     */
    public void stop() {
        started = false;
        logger.info("Shutting down client scheduler...");
        
        try {
            if (serverChannel != null) {
                // 发送注销消息
                Message message = new Message();
                message.setType(MessageType.CLIENT_UNREGISTER);
                message.setClientId(clientInfo.getClientId());
                serverChannel.writeAndFlush(message);
                
                // 关闭通道
                serverChannel.close().sync();
                logger.info("Client unregistered and disconnected");
            }
        } catch (InterruptedException e) {
            logger.error("Error while stopping client", e);
            Thread.currentThread().interrupt();
        } finally {
            // 关闭事件循环组
            group.shutdownGracefully();
            
            // 关闭调度器
            scheduler.shutdown();
            heartbeatScheduler.shutdown();
            
            // 关闭任务执行线程池，使用超时等待确保任务完成
            if (taskExecutorService != null) {
                logger.info("Shutting down task executor service...");
                taskExecutorService.shutdown();
                try {
                    if (!taskExecutorService.awaitTermination(5, TimeUnit.SECONDS)) {
                        logger.warn("Task executor service did not terminate gracefully, forcing shutdown");
                        List<Runnable> remainingTasks = taskExecutorService.shutdownNow();
                        logger.warn("Remaining tasks in queue: {}", remainingTasks.size());
                    }
                } catch (InterruptedException e) {
                    logger.error("Interrupted while waiting for task executor service to terminate", e);
                    taskExecutorService.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
            
            logger.info("Client scheduler stopped");
        }
    }
    
    /**
     * 发送消息到服务器
     */
    public void sendMessage(Message message) {
        if (serverChannel != null && serverChannel.isActive()) {
            serverChannel.writeAndFlush(message);
        } else {
            logger.warn("Cannot send message: not connected to server");
        }
    }
    
    /**
     * 创建任务执行线程池
     */
    private ThreadPoolExecutor createTaskExecutorService() {
        BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
        ThreadFactory threadFactory = new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger(1);
            
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "task-executor-" + counter.getAndIncrement());
                thread.setDaemon(false);
                // 设置未捕获异常处理器
                thread.setUncaughtExceptionHandler((t, e) -> {
                    logger.error("Uncaught exception in task thread: {}", t.getName(), e);
                });
                return thread;
            }
        };
        
        // 创建拒绝策略，打印详细日志
        RejectedExecutionHandler rejectionHandler = (r, executor) -> {
            logger.error("Task rejected, executor saturated - poolSize: {}, activeCount: {}, queueSize: {}",
                executor.getPoolSize(), executor.getActiveCount(), executor.getQueue().size());
            try {
                // 尝试在调用线程执行，避免任务丢失
                if (!executor.isShutdown()) {
                    logger.warn("Attempting to execute rejected task in caller's thread");
                    r.run();
                }
            } catch (Exception e) {
                logger.error("Failed to execute rejected task in caller's thread", e);
                throw new RejectedExecutionException("Task execution rejected and fallback failed", e);
            }
        };
        
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                MAX_POOL_SIZE,
                KEEP_ALIVE_TIME,
                TimeUnit.SECONDS,
                workQueue,
                threadFactory,
                rejectionHandler
        );
        
        // 允许核心线程超时回收，当系统负载低时减少资源占用
        executor.allowCoreThreadTimeOut(true);
        
        return executor;
    }
    
    /**
     * 获取任务执行线程池
     */
    public ThreadPoolExecutor getTaskExecutorService() {
        return taskExecutorService;
    }
    
    /**
     * 重新连接到服务器
     */
    public void reconnect() {
        if (started) {
            logger.info("Manual reconnect triggered");
            // 创建一个新的Bootstrap实例并连接
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                        .channel(NioSocketChannel.class)
                        .handler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) throws Exception {
                                ch.pipeline().addLast(
                                        new ObjectEncoder(),
                                        new ObjectDecoder(Integer.MAX_VALUE, ClassResolvers.weakCachingConcurrentResolver(this.getClass().getClassLoader())),
                                        new ClientHandler(ClientScheduler.this, taskRegistry)
                                );
                            }
                        });
            
            connect(bootstrap);
        }
    }
}