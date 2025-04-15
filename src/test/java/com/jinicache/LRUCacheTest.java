package com.jinicache;

import com.jinicache.cache.LRUCache;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * LRU缓存测试类
 */
public class LRUCacheTest {
    
    @Test
    public void testBasicOperations() {
        LRUCache<String, String> cache = new LRUCache<>(2);
        
        // 测试添加和获取
        cache.put("key1", "value1");
        assertEquals("value1", cache.get("key1"));
        
        // 测试更新
        cache.put("key1", "value2");
        assertEquals("value2", cache.get("key1"));
        
        // 测试删除
        cache.remove("key1");
        assertNull(cache.get("key1"));
        
        // 测试清空
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        cache.clear();
        assertTrue(cache.isEmpty());
    }
    
    @Test
    public void testLRUEviction() {
        LRUCache<String, String> cache = new LRUCache<>(2);
        
        // 添加两个元素
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        
        // 访问key1，使其成为最近使用的
        cache.get("key1");
        
        // 添加新元素，应该淘汰key2
        cache.put("key3", "value3");
        assertNull(cache.get("key2"));
        assertEquals("value1", cache.get("key1"));
        assertEquals("value3", cache.get("key3"));
    }
    
    @Test
    public void testConcurrentAccess() throws InterruptedException {
        LRUCache<String, String> cache = new LRUCache<>(100);
        
        // 创建多个线程同时访问缓存
        Thread[] threads = new Thread[10];
        for (int i = 0; i < threads.length; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 100; j++) {
                    String key = "key" + threadId + "_" + j;
                    String value = "value" + threadId + "_" + j;
                    cache.put(key, value);
                    assertEquals(value, cache.get(key));
                }
            });
            threads[i].start();
        }
        
        // 等待所有线程完成
        for (Thread thread : threads) {
            thread.join();
        }
        
        // 验证缓存大小
        assertTrue(cache.size() <= 100);
    }
} 