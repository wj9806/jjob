package com.distributed.scheduler.test;

import com.distributed.scheduler.client.ClientScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.CountDownLatch;

/**
 * 测试应用主类
 * 用于测试scheduler-client与scheduler-server的通信
 */
@SpringBootApplication
public class SchedulerTestApplication {

    private final Logger logger = LoggerFactory.getLogger(SchedulerTestApplication.class);

    private static ClientScheduler clientScheduler;

    public static void main(String[] args) {
        SpringApplication.run(SchedulerTestApplication.class, args);
    }

    @Bean
    public ApplicationRunner init(ConfigurableApplicationContext context) {
        return args -> {
            clientScheduler = new ClientScheduler();
            
            // 连接到服务器，参数：服务器地址、端口、客户端组、应用名称，启用注解扫描
            String serverHost = "localhost"; // 服务器主机地址
            int serverPort = 8888;           // 服务器端口
            String clientGroup = "testGroup"; // 客户端组
            String applicationName = "SchedulerTestApplication"; // 应用名称
            
            logger.info("正在初始化客户端，连接到服务器: " + serverHost + ":" + serverPort);
            clientScheduler.init(serverHost, serverPort, clientGroup, applicationName);
            clientScheduler.registerTask(TestTaskExecutor.class);
            //clientScheduler.registerTask(TestTaskExecutorWithCron.class);
            // 创建一个线程启动客户端
            Thread clientThread = new Thread(() -> {
                try {
                    logger.info("正在启动客户端...");
                    clientScheduler.start();
                } catch (Exception e) {
                    System.err.println("客户端启动失败: " + e.getMessage());
                    e.printStackTrace();
                }
            });
            clientThread.setDaemon(false);
            clientThread.start();
            
            // 保持应用运行
            CountDownLatch latch = new CountDownLatch(1);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("正在关闭客户端...");
                if (clientScheduler != null) {
                    clientScheduler.stop();
                }
                latch.countDown();
            }));
        };
    }
}