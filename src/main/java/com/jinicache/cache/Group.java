package com.jinicache.cache;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 缓存组实现
 * 用于管理不同类型的缓存，每个组可以有自己的缓存策略
 */
public class Group {
    private final String name;
    private final Cache<String, byte[]> cache;
    private final ConcurrentHashMap<String, Loader> loaders;
    private final Lock lock;
    private final SingleFlight<byte[]> singleFlight;

    /**
     * 缓存加载器接口
     */
    public interface Loader {
        byte[] load(String key);
    }

    /**
     * 构造函数
     * @param name 组名
     * @param cache 底层缓存实现
     */
    public Group(String name, Cache<String, byte[]> cache) {
        this.name = name;
        this.cache = cache;
        this.loaders = new ConcurrentHashMap<>();
        this.lock = new ReentrantLock();
        this.singleFlight = new SingleFlight<>(5, TimeUnit.SECONDS); // 默认5秒超时
    }

    /**
     * 注册缓存加载器
     * @param key 键
     * @param loader 加载器
     */
    public void registerLoader(String key, Loader loader) {
        loaders.put(key, loader);
    }

    /**
     * 获取缓存值
     * @param key 键
     * @return 缓存的值
     */
    public byte[] get(String key) {
        byte[] value = cache.get(key);
        if (value == null) {
            return load(key);
        }
        return value;
    }

    /**
     * 异步获取缓存值
     * @param key 键
     * @return 缓存值的Future
     */
    public CompletableFuture<byte[]> getAsync(String key) {
        byte[] value = cache.get(key);
        if (value != null) {
            return CompletableFuture.completedFuture(value);
        }
        return loadAsync(key);
    }

    /**
     * 加载缓存值
     * @param key 键
     * @return 加载的值
     */
    private byte[] load(String key) {
        lock.lock();
        try {
            // 双重检查锁定
            byte[] value = cache.get(key);
            if (value != null) {
                return value;
            }

            Loader loader = loaders.get(key);
            if (loader == null) {
                return null;
            }

            value = loader.load(key);
            if (value != null) {
                cache.put(key, value);
            }
            return value;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 异步加载缓存值
     * @param key 键
     * @return 加载值的Future
     */
    private CompletableFuture<byte[]> loadAsync(String key) {
        return singleFlight.doCall(key, () -> {
            Loader loader = loaders.get(key);
            if (loader == null) {
                return null;
            }
            byte[] value = loader.load(key);
            if (value != null) {
                cache.put(key, value);
            }
            return value;
        });
    }

    /**
     * 获取组名
     * @return 组名
     */
    public String getName() {
        return name;
    }

    /**
     * 获取底层缓存实现
     * @return 缓存实现
     */
    public Cache<String, byte[]> getCache() {
        return cache;
    }
} 