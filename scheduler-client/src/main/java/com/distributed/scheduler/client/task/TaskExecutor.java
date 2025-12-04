package com.distributed.scheduler.client.task;

import com.distributed.scheduler.client.model.TaskInfo;

/**
 * 任务执行器接口
 * 用户需要实现此接口来定义具体的任务执行逻辑
 */
public interface TaskExecutor {

    /**
     * 初始化任务执行器
     * @throws Exception 初始化过程中的异常
     */
    default void init() throws Exception {

    }

    /**
     * 销毁任务执行器
     * @throws Exception 销毁过程中的异常
     */
    default void destroy() throws Exception {

    }
    
    /**
     * 执行任务
     * @param taskInfo 任务信息
     * @return 任务执行结果
     * @throws Exception 执行过程中的异常
     */
    Object execute(TaskInfo taskInfo) throws Exception;
    
    /**
     * 获取任务名称
     * @return 任务名称
     */
    String getTaskName();
    
    /**
     * 获取任务分组
     * @return 任务分组
     */
    String getTaskGroup();
    
    /**
     * 获取Cron表达式
     * @return Cron表达式
     */
    String getCronExpression();
    
    /**
     * 判断任务执行器是否为单例
     * @return true表示单例，false表示非单例
     */
    default boolean isSingleton() {
        return true;
    }
}