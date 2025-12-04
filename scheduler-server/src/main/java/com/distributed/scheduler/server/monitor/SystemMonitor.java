package com.distributed.scheduler.server.monitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Method;

/**
 * 系统监控类，用于获取CPU、内存、磁盘和网络信息
 */
@Component
public class SystemMonitor {
    private static final Logger logger = LoggerFactory.getLogger(SystemMonitor.class);
    
    // 操作系统MXBean
    private final OperatingSystemMXBean osMXBean;
    // 内存MXBean
    private final MemoryMXBean memoryMXBean;
    
    public SystemMonitor() {
        this.osMXBean = ManagementFactory.getOperatingSystemMXBean();
        this.memoryMXBean = ManagementFactory.getMemoryMXBean();
    }
    
    /**
     * 获取CPU使用率
     * @return CPU使用率，范围0-100
     */
    public double getCpuUsage() {
        try {
            // 使用反射获取com.sun.management.OperatingSystemMXBean的getSystemCpuLoad方法
            Method method = osMXBean.getClass().getMethod("getSystemCpuLoad");
            // 设置方法可访问，绕过Java的访问检查
            method.setAccessible(true);
            double cpuLoad = (double) method.invoke(osMXBean);
            return cpuLoad < 0 ? 0 : cpuLoad * 100;
        } catch (Exception e) {
            logger.error("Failed to get CPU usage:", e);
            return 0;
        }
    }
    
    /**
     * 获取操作系统内存使用率
     * @return 内存使用率，范围0-100
     */
    public double getMemoryUsage() {
        try {
            // 使用反射获取com.sun.management.OperatingSystemMXBean的内存相关方法
            Method getTotalMemoryMethod = osMXBean.getClass().getMethod("getTotalPhysicalMemorySize");
            Method getFreeMemoryMethod = osMXBean.getClass().getMethod("getFreePhysicalMemorySize");
            
            // 设置方法可访问
            getTotalMemoryMethod.setAccessible(true);
            getFreeMemoryMethod.setAccessible(true);
            
            // 调用方法获取内存信息
            long totalMemory = (long) getTotalMemoryMethod.invoke(osMXBean);
            long freeMemory = (long) getFreeMemoryMethod.invoke(osMXBean);
            
            // 计算内存使用率
            long usedMemory = totalMemory - freeMemory;
            return totalMemory > 0 ? (double) usedMemory / totalMemory * 100 : 0;
        } catch (Exception e) {
            logger.error("Failed to get system memory usage:", e);
            return 0;
        }
    }
    
    /**
     * 获取磁盘使用率
     * @return 磁盘使用率，范围0-100
     */
    public double getDiskUsage() {
        try {
            // 获取根目录的磁盘使用情况
            File root = new File(".");
            long totalSpace = root.getTotalSpace();
            long freeSpace = root.getFreeSpace();
            return totalSpace > 0 ? (double) (totalSpace - freeSpace) / totalSpace * 100 : 0;
        } catch (Exception e) {
            logger.error("Failed to get disk usage:", e);
            return 0;
        }
    }
    

    
    /**
     * 获取所有系统监控数据
     * @return 系统监控数据
     */
    public SystemMonitorData getSystemMonitorData() {
        SystemMonitorData data = new SystemMonitorData();
        data.setTimestamp(System.currentTimeMillis());
        data.setCpuUsage(getCpuUsage());
        data.setMemoryUsage(getMemoryUsage());
        data.setDiskUsage(getDiskUsage());
        return data;
    }
    
}