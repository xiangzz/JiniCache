package com.jinicache.example;

import com.jinicache.JiniCache;
import com.jinicache.cache.CacheManager;
import com.jinicache.cache.Group;
import com.jinicache.cache.LRUCache;
import java.util.Arrays;

/**
 * JiniCache使用示例
 */
public class Example {
    public static void main(String[] args) {
        // 创建缓存管理器
        CacheManager cacheManager = new CacheManager();
        
        // 创建缓存组
        Group group = cacheManager.createGroup("example", new LRUCache<>(1000));
        
        // 注册缓存加载器
        group.registerLoader("key1", key -> "value1".getBytes());
        
        // 获取缓存值
        byte[] value = group.get("key1");
        System.out.println("Value: " + new String(value));
        
        // 创建分布式缓存实例
        JiniCache jiniCache = new JiniCache(8001, Arrays.asList("localhost:8002"));
        jiniCache.start();
        
        // 等待服务启动
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // 停止服务
        jiniCache.stop();
    }
} 