package com.distributed.scheduler.test;

import com.distributed.scheduler.client.model.TaskInfo;
import com.distributed.scheduler.client.task.TaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * 测试任务执行器 - 带cron表达式
 * 实现TaskExecutor接口，用于测试与server端的通信和基于cron表达式的任务调度
 */
public class TestTaskExecutorWithCron implements TaskExecutor {

    private static final Logger log = LoggerFactory.getLogger(TestTaskExecutorWithCron.class);

    @Override
    public Object execute(TaskInfo taskInfo) throws Exception {
        System.out.println("[" + new Date() + "] 执行cron测试任务: " + taskInfo.getTaskId());

        // 模拟任务执行
        try {
            Thread.sleep(1000); // 模拟执行时间
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new Exception("任务执行被中断", e);
        }
        
        String result = "cron任务执行成功，时间: " + new Date();
        System.out.println("[" + new Date() + "] " + result);
        return result;
    }

    // 使用注解后，这些方法可以返回默认值，任务信息将从注解获取
    @Override
    public String getTaskName() {
        return "testTaskExecutorWithCron";
    }

    @Override
    public String getTaskGroup() {
        return "testGroup";
    }

    @Override
    public String getCronExpression() {
        // 每5秒执行一次
        return "*/5 * * * * ?";
    }

    public TestTaskExecutorWithCron() {
        System.out.println("创建带cron的测试任务执行器");
    }
}