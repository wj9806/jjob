package com.distributed.scheduler.client.protocol;

public enum MessageType {
    CLIENT_REGISTER,      // 客户端注册
    CLIENT_HEARTBEAT,     // 客户端心跳
    CLIENT_UNREGISTER,    // 客户端注销
    TASK_TRIGGER,         // 任务触发
    TASK_STATUS_REPORT,   // 任务状态上报
    TASK_RESULT_REPORT,   // 任务结果上报
    SERVER_RESPONSE       // 服务端响应
}