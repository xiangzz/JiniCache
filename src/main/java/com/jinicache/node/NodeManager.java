package com.jinicache.node;

import com.jinicache.cache.CacheManager;
import com.jinicache.hash.ConsistentHash;
import com.jinicache.http.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 分布式节点管理器
 */
public class NodeManager {
    private static final Logger logger = LoggerFactory.getLogger(NodeManager.class);
    private final String selfAddress;
    private final ConsistentHash<String> hashRing;
    private final ConcurrentHashMap<String, HttpClient> clients;
    /**
     * 构造函数
     * @param selfAddress 本节点地址
     * @param cacheManager 缓存管理器
     */
    public NodeManager(String selfAddress, CacheManager cacheManager) {
        this.selfAddress = selfAddress;
        this.hashRing = new ConsistentHash<>(100);
        this.clients = new ConcurrentHashMap<>();
        this.hashRing.add(selfAddress);
    }

    /**
     * 添加节点
     * @param address 节点地址
     */
    public void addNode(String address) {
        if (!address.equals(selfAddress)) {
            hashRing.add(address);
            clients.putIfAbsent(address, new HttpClient());
            logger.info("Added node: {}", address);
        }
    }

    /**
     * 移除节点
     * @param address 节点地址
     */
    public void removeNode(String address) {
        if (!address.equals(selfAddress)) {
            hashRing.remove(address);
            HttpClient client = clients.remove(address);
            if (client != null) {
                client.shutdown();
            }
            logger.info("Removed node: {}", address);
        }
    }

    /**
     * 获取键对应的节点
     * @param key 键
     * @return 节点地址
     */
    public String getNode(String key) {
        return hashRing.get(key);
    }

    /**
     * 判断键是否属于本节点
     * @param key 键
     * @return 如果属于本节点返回true
     */
    public boolean isLocalNode(String key) {
        return selfAddress.equals(getNode(key));
    }

    /**
     * 获取所有节点
     * @return 节点地址列表
     */
    public List<String> getAllNodes() {
        List<String> nodes = new ArrayList<>();
        nodes.add(selfAddress);
        nodes.addAll(clients.keySet());
        return nodes;
    }

    /**
     * 获取HTTP客户端
     * @param address 节点地址
     * @return HTTP客户端
     */
    public HttpClient getClient(String address) {
        return clients.get(address);
    }

    /**
     * 关闭所有连接
     */
    public void shutdown() {
        clients.values().forEach(HttpClient::shutdown);
    }
} 