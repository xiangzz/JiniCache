package com.jinicache;

import com.jinicache.cache.LRUCache;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 性能测试类
 */
public class PerformanceTest {
    
    @Test
    public void testHighConcurrency() throws InterruptedException {
        LRUCache<String, String> cache = new LRUCache<>(1000);
        int threadCount = 100;
        int operationsPerThread = 1000;
        
        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < operationsPerThread; j++) {
                    String key = "key" + threadId + "_" + j;
                    String value = "value" + threadId + "_" + j;
                    cache.put(key, value);
                    cache.get(key);
                }
            });
            threads[i].start();
        }
        
        for (Thread thread : threads) {
            thread.join();
        }
        
        assertTrue(cache.size() <= 1000);
    }
    
    @Test
    public void testLargeDataVolume() {
        LRUCache<String, String> cache = new LRUCache<>(10000);
        int dataSize = 10000;
        
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < dataSize; i++) {
            String key = "key" + i;
            String value = "value" + i;
            cache.put(key, value);
        }
        long endTime = System.currentTimeMillis();
        
        long putTime = endTime - startTime;
        assertTrue(putTime < 5000); // 5秒内完成
        
        startTime = System.currentTimeMillis();
        for (int i = 0; i < dataSize; i++) {
            String key = "key" + i;
            cache.get(key);
        }
        endTime = System.currentTimeMillis();
        
        long getTime = endTime - startTime;
        assertTrue(getTime < 5000); // 5秒内完成
    }
    
    @Test
    public void testLongRunning() throws InterruptedException {
        LRUCache<String, String> cache = new LRUCache<>(1000);
        int duration = 60; // 运行60秒
        int operationsPerSecond = 100;
        
        long startTime = System.currentTimeMillis();
        long endTime = startTime + (duration * 1000);
        
        while (System.currentTimeMillis() < endTime) {
            for (int i = 0; i < operationsPerSecond; i++) {
                String key = "key" + i;
                String value = "value" + i;
                cache.put(key, value);
                cache.get(key);
            }
            Thread.sleep(1000);
        }
        
        assertTrue(cache.size() <= 1000);
    }
} 