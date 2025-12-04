package com.distributed.scheduler.client.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskStatus implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public enum Status {
        WAITING,    // 等待执行
        RUNNING,    // 执行中
        SUCCESS,    // 执行成功
        FAILED      // 执行失败
    }
    
    private String taskId;           // 任务ID
    private String taskName;         // 任务名称
    private String instanceId;       // 实例ID
    private String clientId;         // 客户端ID
    private Status status;           // 执行状态
    private Date startTime;          // 开始时间
    private Date endTime;            // 结束时间
    private String errorMsg;         // 错误信息
    private long executionTime;      // 执行时长(毫秒)
    private boolean oneRunning;      // 任务配置：是否只允许一个实例运行，用于服务端处理
}