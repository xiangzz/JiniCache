package com.jinicache.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * SingleFlight实现
 * 防止缓存击穿，确保同一个key只有一个请求在执行
 * 修复了内存泄漏问题，实现了正确的Call对象清理机制
 */
public class SingleFlight<T> {
    private static final Logger logger = LoggerFactory.getLogger(SingleFlight.class);
    private final ConcurrentHashMap<String, Call<T>> calls = new ConcurrentHashMap<>();
    private static final long DEFAULT_TIMEOUT_MS = 30000; // 30秒超时

    /**
     * 执行函数，确保相同key的并发请求只执行一次
     * @param key 键
     * @param fn 要执行的函数
     * @return 执行结果
     */
    public CompletableFuture<T> doCall(String key, Supplier<T> fn) {
        return calls.compute(key, (k, existingCall) -> {
            // 如果存在调用且未完成，返回现有调用
            if (existingCall != null && !existingCall.isCompleted() && !existingCall.isExpired(DEFAULT_TIMEOUT_MS)) {
                return existingCall;
            }
            
            // 创建新的调用
            CompletableFuture<T> future = CompletableFuture.supplyAsync(fn)
                    .orTimeout(DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            
            Call<T> newCall = new Call<>(key, future);
            
            // 当调用完成时清理
            future.whenComplete((result, error) -> {
                if (newCall.markCompleted()) {
                    calls.remove(key, newCall);
                }
            });
            
            return newCall;
        }).getFuture();
    }
    
    /**
     * 清理过期的调用
     * 定期调用此方法可以防止内存泄漏
     */
    public void cleanupExpiredCalls() {
        long currentTime = System.currentTimeMillis();
        calls.entrySet().removeIf(entry -> {
            Call<T> call = entry.getValue();
            if (call.isExpired(DEFAULT_TIMEOUT_MS) || call.isCompleted()) {
                logger.debug("Removing expired/completed call for key: {}", entry.getKey());
                return true;
            }
            return false;
        });
    }
    
    /**
     * 获取当前活跃调用数量
     * @return 活跃调用数量
     */
    public int getActiveCallsCount() {
        cleanupExpiredCalls();
        return calls.size();
    }
    
    /**
     * 检查指定key是否有活跃调用
     * @param key 要检查的key
     * @return 如果有活跃调用返回true
     */
    public boolean hasActiveCall(String key) {
        Call<T> call = calls.get(key);
        return call != null && !call.isCompleted() && !call.isExpired(DEFAULT_TIMEOUT_MS);
    }
    
    /**
     * 强制取消指定key的调用
     * @param key 要取消的key
     * @return 如果成功取消返回true
     */
    public boolean cancelCall(String key) {
        Call<T> call = calls.remove(key);
        if (call != null) {
            call.getFuture().cancel(true);
            call.markCompleted();
            logger.debug("Cancelled call for key: {}", key);
            return true;
        }
        return false;
    }

    /**
     * 内部Call类，表示一个正在进行的调用
     */
    private static class Call<T> {
        private final CompletableFuture<T> future;
        private final long startTime;
        private final AtomicBoolean completed = new AtomicBoolean(false);
        private final String key;

        public Call(String key, CompletableFuture<T> future) {
            this.key = key;
            this.future = future;
            this.startTime = System.currentTimeMillis();
        }

        public CompletableFuture<T> getFuture() {
            return future;
        }

        public long getStartTime() {
            return startTime;
        }
        
        public String getKey() {
            return key;
        }
        
        public boolean markCompleted() {
            return completed.compareAndSet(false, true);
        }
        
        public boolean isCompleted() {
            return completed.get();
        }
        
        public boolean isExpired(long timeoutMs) {
            return System.currentTimeMillis() - startTime > timeoutMs;
        }
    }
}