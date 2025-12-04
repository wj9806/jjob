package com.distributed.scheduler.server.scheduler.strategy;

import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 客户端选择策略工厂
 * 管理所有可用的调度策略，并根据名称获取对应的策略实例
 */
@Component
public class ClientSelectionStrategyFactory {
    
    // 存储所有可用的策略
    private final Map<String, ClientSelectionStrategy> strategies = new HashMap<>();
    
    // 默认策略名称
    public static final String DEFAULT_STRATEGY_NAME = "roundRobin";
    
    // 使用读写锁保证线程安全
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    
    /**
     * 构造函数，初始化所有策略
     */
    public ClientSelectionStrategyFactory() {
        // 注册内置策略
        registerStrategy(new RoundRobinStrategy());
        registerStrategy(new RandomStrategy());
        registerStrategy(new WeightedStrategy());
    }
    
    /**
     * 注册新的调度策略
     * 
     * @param strategy 调度策略实例
     */
    public void registerStrategy(ClientSelectionStrategy strategy) {
        if (strategy == null) {
            throw new IllegalArgumentException("Strategy cannot be null");
        }
        
        lock.writeLock().lock();
        try {
            strategies.put(strategy.getStrategyName(), strategy);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 根据名称获取调度策略
     * 
     * @param strategyName 策略名称
     * @return 对应的策略实例，如果不存在则返回默认策略
     */
    public ClientSelectionStrategy getStrategy(String strategyName) {
        lock.readLock().lock();
        try {
            ClientSelectionStrategy strategy = strategies.get(strategyName);
            // 如果指定的策略不存在，返回默认策略
            if (strategy == null) {
                strategy = strategies.get(DEFAULT_STRATEGY_NAME);
            }
            return strategy;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 获取默认调度策略
     * 
     * @return 默认策略实例
     */
    public ClientSelectionStrategy getDefaultStrategy() {
        return getStrategy(DEFAULT_STRATEGY_NAME);
    }
    
    /**
     * 获取所有可用的策略名称
     * 
     * @return 策略名称数组
     */
    public String[] getAllStrategyNames() {
        lock.readLock().lock();
        try {
            return strategies.keySet().toArray(new String[0]);
        } finally {
            lock.readLock().unlock();
        }
    }
}
