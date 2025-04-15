package com.jinicache.cache;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * SingleFlight实现
 * 用于防止缓存击穿的并发控制机制
 */
public class SingleFlight<T> {
    private final ConcurrentHashMap<String, Call<T>> calls;
    private final long timeout;
    private final TimeUnit timeoutUnit;

    /**
     * 构造函数
     * @param timeout 超时时间
     * @param timeoutUnit 超时单位
     */
    public SingleFlight(long timeout, TimeUnit timeoutUnit) {
        this.calls = new ConcurrentHashMap<>();
        this.timeout = timeout;
        this.timeoutUnit = timeoutUnit;
    }

    /**
     * 执行函数，确保相同key的并发请求只执行一次
     * @param key 键
     * @param fn 要执行的函数
     * @return 执行结果
     */
    public CompletableFuture<T> doCall(String key, Supplier<T> fn) {
        Call<T> call = calls.computeIfAbsent(key, k -> new Call<>());
        return call.doCall(fn, timeout, timeoutUnit);
    }

    /**
     * 内部Call类，用于管理单个key的调用状态
     */
    private static class Call<T> {
        private CompletableFuture<T> future;
        private long startTime;

        public synchronized CompletableFuture<T> doCall(Supplier<T> fn, long timeout, TimeUnit timeoutUnit) {
            if (future != null) {
                return future;
            }

            startTime = System.currentTimeMillis();
            future = CompletableFuture.supplyAsync(fn)
                    .orTimeout(timeout, timeoutUnit)
                    .whenComplete((result, error) -> {
                        if (System.currentTimeMillis() - startTime >= timeoutUnit.toMillis(timeout)) {
                            future = null;
                        }
                    });

            return future;
        }
    }
} 