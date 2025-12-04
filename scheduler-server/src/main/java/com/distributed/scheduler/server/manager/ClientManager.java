package com.distributed.scheduler.server.manager;

import com.distributed.scheduler.client.model.ClientInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class ClientManager {
    private static final Logger logger = LoggerFactory.getLogger(ClientManager.class);
    private final Map<String, ClientInfo> clients = new ConcurrentHashMap<>();
    private final Map<String, String> channelIdToClientId = new ConcurrentHashMap<>(); // 通道ID到客户端ID的映射
    private static final long HEARTBEAT_TIMEOUT = 60000; // 60秒心跳超时
    
    /**
     * 注册客户端
     */
    public void registerClient(String channelId, ClientInfo clientInfo) {
        clients.put(clientInfo.getClientId(), clientInfo);
        channelIdToClientId.put(channelId, clientInfo.getClientId());
        logger.debug("Client registered: {} - {}", clientInfo.getClientId(), clientInfo.getApplicationName());
    }
    
    /**
     * 更新客户端心跳
     */
    public boolean updateHeartbeat(String clientId) {
        ClientInfo client = clients.get(clientId);
        if (client != null) {
            client.setLastHeartbeatTime(System.currentTimeMillis());
            if (!client.isOnline()) {
                // 如果客户端之前是离线状态，现在恢复为在线状态
                client.setOnline(true);
                logger.info("Client is back online: {}", clientId);
            }
            return true;
        }
        return false;
    }
    
    /**
     * 注销客户端
     */
    public void unregisterClient(String clientId) {
        clients.remove(clientId);
        // 移除对应的通道映射
        channelIdToClientId.forEach((channelId, id) -> {
            if (id.equals(clientId)) {
                channelIdToClientId.remove(channelId);
            }
        });
        logger.info("Client unregistered: {}", clientId);
    }
    
    /**
     * 获取客户端信息
     */
    public ClientInfo getClient(String clientId) {
        return clients.get(clientId);
    }
    
    /**
     * 获取所有客户端
     */
    public Map<String, ClientInfo> getAllClients() {
        return new ConcurrentHashMap<>(clients);
    }
    
    /**
     * 根据分组获取客户端
     */
    public Map<String, ClientInfo> getClientsByGroup(String group) {
        return clients.values().stream()
                .filter(client -> client.getGroup().equals(group))
                .collect(Collectors.toConcurrentMap(ClientInfo::getClientId, client -> client));
    }
    
    /**
     * 根据通道ID查找客户端ID
     */
    public String findClientIdByChannelId(String channelId) {
        return channelIdToClientId.get(channelId);
    }
    
    /**
     * 根据Channel获取客户端ID
     */
    public String getClientIdByChannel(io.netty.channel.Channel channel) {
        if (channel == null) return null;
        String channelId = channel.id().asShortText();
        return channelIdToClientId.get(channelId);
    }
    
    /**
     * 清理超时客户端
     */
    public void cleanupTimeoutClients() {
        long currentTime = System.currentTimeMillis();
        clients.values().forEach(client -> {
            boolean timeout = currentTime - client.getLastHeartbeatTime() > HEARTBEAT_TIMEOUT;
            if (timeout && client.isOnline()) {
                // 只需要标记为离线，不需要完全移除
                client.setOnline(false);
                logger.warn("Client timed out, marked as offline: {}", client.getClientId());
                // 移除对应的通道映射，因为通道已经不可用
                channelIdToClientId.forEach((channelId, id) -> {
                    if (id.equals(client.getClientId())) {
                        channelIdToClientId.remove(channelId);
                    }
                });
            }
        });
    }
}