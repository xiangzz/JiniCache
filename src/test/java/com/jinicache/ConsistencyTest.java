package com.jinicache;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 分布式一致性测试类
 */
public class ConsistencyTest {
    
    @Test
    public void testDataReplication() throws Exception {
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
            // 在节点1上写入数据
            nodes.get(0).getCacheManager().getGroup("default").getCache().put("key1", "value1".getBytes());
            
            // 等待数据同步
            Thread.sleep(1000);
            
            // 验证所有节点都有相同的数据
            for (JiniCache node : nodes) {
                assertArrayEquals("value1".getBytes(), node.getCacheManager().getGroup("default").get("key1"));
            }
        } finally {
            // 停止所有节点
            nodes.forEach(JiniCache::stop);
        }
    }
    
    @Test
    public void testNetworkPartition() throws Exception {
        // 创建四个节点，形成两个分区
        List<Integer> ports = Arrays.asList(8001, 8002, 8003, 8004);
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
            // 在节点1上写入数据
            nodes.get(0).getCacheManager().getGroup("default").getCache().put("key1", "value1".getBytes());
            
            // 等待数据同步
            Thread.sleep(1000);
            
            // 模拟网络分区：停止节点3和4
            nodes.get(2).stop();
            nodes.get(3).stop();
            
            // 等待分区检测
            Thread.sleep(2000);
            
            // 在节点1上更新数据
            nodes.get(0).getCacheManager().getGroup("default").getCache().put("key1", "value2".getBytes());
            
            // 等待数据同步
            Thread.sleep(1000);
            
            // 验证节点1和2有更新后的数据
            assertArrayEquals("value2".getBytes(), nodes.get(0).getCacheManager().getGroup("default").get("key1"));
            assertArrayEquals("value2".getBytes(), nodes.get(1).getCacheManager().getGroup("default").get("key1"));
            
            // 重启节点3和4
            nodes.get(2).start();
            nodes.get(3).start();
            
            // 等待节点恢复和数据同步
            Thread.sleep(2000);
            
            // 验证所有节点最终一致
            for (JiniCache node : nodes) {
                assertArrayEquals("value2".getBytes(), node.getCacheManager().getGroup("default").get("key1"));
            }
        } finally {
            // 停止所有节点
            nodes.forEach(JiniCache::stop);
        }
    }
    
    @Test
    public void testConcurrentWrites() throws Exception {
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
            // 在不同节点上并发写入相同key
            CompletableFuture<Void> future1 = CompletableFuture.runAsync(() -> {
                nodes.get(0).getCacheManager().getGroup("default").getCache().put("key1", "value1".getBytes());
            });
            
            CompletableFuture<Void> future2 = CompletableFuture.runAsync(() -> {
                nodes.get(1).getCacheManager().getGroup("default").getCache().put("key1", "value2".getBytes());
            });
            
            CompletableFuture<Void> future3 = CompletableFuture.runAsync(() -> {
                nodes.get(2).getCacheManager().getGroup("default").getCache().put("key1", "value3".getBytes());
            });
            
            // 等待所有写入完成
            CompletableFuture.allOf(future1, future2, future3).get(10, TimeUnit.SECONDS);
            
            // 等待数据同步
            Thread.sleep(2000);
            
            // 验证所有节点最终一致
            byte[] finalValue = nodes.get(0).getCacheManager().getGroup("default").get("key1");
            assertNotNull(finalValue);
            for (JiniCache node : nodes) {
                assertArrayEquals(finalValue, node.getCacheManager().getGroup("default").get("key1"));
            }
        } finally {
            // 停止所有节点
            nodes.forEach(JiniCache::stop);
        }
    }
} 