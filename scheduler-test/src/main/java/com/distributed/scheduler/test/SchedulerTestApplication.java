package com.distributed.scheduler.test;

import com.distributed.scheduler.client.ClientScheduler;

/**
 * 测试应用主类
 * 用于测试scheduler-client与scheduler-server的通信
 */

public class SchedulerTestApplication {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SchedulerTestApplication.class);

    public static void main(String[] args) {
        ClientScheduler clientScheduler = new ClientScheduler();

        String serverHost = "localhost"; // 服务器主机地址
        int serverPort = 8888;           // 服务器端口
        String clientGroup = "testGroup"; // 客户端组
        String applicationName = "SchedulerTestApplication"; // 应用名称

        clientScheduler.init(serverHost, serverPort, clientGroup, applicationName);
        clientScheduler.registerTask(TestTaskExecutor.class);
        try {
            clientScheduler.start();
        } catch (Exception e) {
            System.err.println("客户端启动失败: " + e.getMessage());
        }

        // 保持应用运行
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("正在关闭客户端...");
            clientScheduler.stop();
        }));
    }

}