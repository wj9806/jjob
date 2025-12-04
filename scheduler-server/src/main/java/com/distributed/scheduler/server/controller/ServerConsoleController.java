package com.distributed.scheduler.server.controller;

import com.distributed.scheduler.server.manager.ClientManager;
import com.distributed.scheduler.client.model.ClientInfo;
import com.distributed.scheduler.server.monitor.SystemMonitorData;
import com.distributed.scheduler.server.scheduler.TaskScheduler;
import com.distributed.scheduler.client.model.TaskInfo;
import com.distributed.scheduler.server.monitor.SystemMonitor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public class ServerConsoleController {

    @Autowired
    private ClientManager clientManager;
    
    @Autowired
    private TaskScheduler taskScheduler;
    
    @Autowired
    private SystemMonitor systemMonitor;

    /**
     * 获取所有注册的客户端信息
     */
    @GetMapping("/api/clients")
    public Map<String, ClientInfo> getAllClients() {
        return clientManager.getAllClients();
    }

    /**
     * 获取系统状态信息
     */
    @GetMapping("/api/status")
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new ConcurrentHashMap<>();
        status.put("clients", clientManager.getAllClients().size());
        status.put("totalTasks", taskScheduler.getTaskCount());
        status.put("enabledTasks", taskScheduler.getEnabledTaskCount());
        status.put("disabledTasks", taskScheduler.getDisabledTaskCount());
        // 暂时使用0作为占位符，因为TaskScheduler没有这些方法
        status.put("runningTasks", 0);
        status.put("pendingTasks", 0);
        status.put("completedTasks", 0);
        status.put("failedTasks", 0);
        status.put("systemTime", System.currentTimeMillis());
        return status;
    }
    
    @GetMapping("/api/monitor")
    public SystemMonitorData getMonitorData() {
        return systemMonitor.getSystemMonitorData();
    }
    
    /**
     * 获取所有注册的任务信息
     */
    @GetMapping("/api/tasks")
    public Map<String, TaskInfo> getAllTasks() {
        return taskScheduler.getAllTasks();
    }
}