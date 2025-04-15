package com.jinicache;

import com.jinicache.cache.CacheManager;
import com.jinicache.cache.LRUCache;
import com.jinicache.http.HttpServer;
import com.jinicache.node.NodeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * JiniCache主程序入口
 */
public class JiniCache {
    private static final Logger logger = LoggerFactory.getLogger(JiniCache.class);
    private final int port;
    private final String selfAddress;
    private final List<String> peers;
    private final CacheManager cacheManager;
    private final NodeManager nodeManager;
    private HttpServer httpServer;

    /**
     * 构造函数
     * @param port 服务器端口
     * @param peers 其他节点地址列表
     */
    public JiniCache(int port, List<String> peers) {
        this.port = port;
        this.selfAddress = "localhost:" + port;
        this.peers = peers;
        this.cacheManager = new CacheManager();
        this.nodeManager = new NodeManager(selfAddress, cacheManager);
    }

    /**
     * 获取缓存管理器
     * @return 缓存管理器
     */
    public CacheManager getCacheManager() {
        return cacheManager;
    }

    /**
     * 启动服务
     */
    public void start() {
        // 创建默认缓存组
        cacheManager.createGroup("default", new LRUCache<>(1000));

        // 添加其他节点
        peers.forEach(nodeManager::addNode);

        // 启动HTTP服务器
        httpServer = new HttpServer(port, cacheManager);
        new Thread(() -> httpServer.start()).start();

        logger.info("JiniCache started on {}", selfAddress);
    }

    /**
     * 停止服务
     */
    public void stop() {
        if (httpServer != null) {
            httpServer.shutdown();
        }
        nodeManager.shutdown();
        logger.info("JiniCache stopped");
    }

    /**
     * 主程序入口
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java JiniCache <port> [peer1] [peer2] ...");
            System.exit(1);
        }

        int port = Integer.parseInt(args[0]);
        List<String> peers = Arrays.asList(Arrays.copyOfRange(args, 1, args.length));
        
        JiniCache jiniCache = new JiniCache(port, peers);
        jiniCache.start();

        // 添加关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(jiniCache::stop));
    }
} 