package com.jinicache;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 分布式缓存测试类
 */
public class DistributedTest {
    
    @Test
    public void testDistributedCache() throws Exception {
        // 创建三个节点
        List<Integer> ports = Arrays.asList(8001, 8002, 8003);
        List<JiniCache> nodes = ports.stream()
                .map(port -> {
                    List<String> peers = ports.stream()
                            .filter(p -> p != port)
                            .map(p -> "localhost:" + p)
                            .toList();
                    return new JiniCache(port, peers);
                })
                .toList();
        
        // 启动所有节点
        nodes.forEach(JiniCache::start);
        
        // 等待节点启动
        Thread.sleep(1000);
        
        try {
            // 测试分布式缓存操作
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                // 这里可以添加具体的测试逻辑
                // 例如：在不同节点上添加和获取缓存
            });
            
            // 等待测试完成
            future.get(10, TimeUnit.SECONDS);
        } finally {
            // 停止所有节点
            nodes.forEach(JiniCache::stop);
        }
    }
    
    @Test
    public void testNodeFailure() throws Exception {
        // 创建两个节点
        JiniCache node1 = new JiniCache(8001, Arrays.asList("localhost:8002"));
        JiniCache node2 = new JiniCache(8002, Arrays.asList("localhost:8001"));
        
        // 启动节点
        node1.start();
        node2.start();
        
        // 等待节点启动
        Thread.sleep(1000);
        
        try {
            // 模拟节点故障
            node2.stop();
            
            // 等待故障检测
            Thread.sleep(2000);
            
            // 验证节点1能够正确处理节点2的故障
            // 这里可以添加具体的验证逻辑
        } finally {
            // 停止所有节点
            node1.stop();
        }
    }
} 