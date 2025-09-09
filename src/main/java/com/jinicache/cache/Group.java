package com.jinicache.cache;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 缓存组实现
 * 用于管理不同类型的缓存，每个组可以有自己的缓存策略
 */
public class Group {
    private final String name;
    private final Cache<String, byte[]> cache;
    private final ConcurrentHashMap<String, Loader> loaders;
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
        this.singleFlight = new SingleFlight<>(5, TimeUnit.SECONDS); // 默认5秒超时
    }

    /**
     * 构造函数（自定义超时时间）
     * @param name 组名
     * @param cache 底层缓存实现
     * @param timeout 超时时间
     * @param timeUnit 时间单位
     */
    public Group(String name, Cache<String, byte[]> cache, long timeout, TimeUnit timeUnit) {
        this.name = name;
        this.cache = cache;
        this.loaders = new ConcurrentHashMap<>();
        this.singleFlight = new SingleFlight<>(timeout, timeUnit);
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
     * 加载缓存值（使用SingleFlight防止缓存击穿）
     * @param key 键
     * @return 加载的值
     */
    private byte[] load(String key) {
        try {
            return singleFlight.doCall(key, () -> {
                // 再次检查缓存，可能在等待期间已被其他线程加载
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
            }).get(); // 同步等待结果
        } catch (Exception e) {
            // 如果SingleFlight调用失败，回退到直接加载
            Loader loader = loaders.get(key);
            if (loader == null) {
                return null;
            }
            
            byte[] value = loader.load(key);
            if (value != null) {
                cache.put(key, value);
            }
            return value;
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

    /**
     * 移除缓存加载器
     * @param key 键
     * @return 被移除的加载器，如果不存在则返回null
     */
    public Loader removeLoader(String key) {
        return loaders.remove(key);
    }

    /**
     * 检查是否存在指定键的加载器
     * @param key 键
     * @return 如果存在返回true，否则返回false
     */
    public boolean hasLoader(String key) {
        return loaders.containsKey(key);
    }

    /**
     * 获取已注册的加载器数量
     * @return 加载器数量
     */
    public int getLoaderCount() {
        return loaders.size();
    }

    /**
     * 清理过期的SingleFlight调用
     */
    public void cleanupExpiredCalls() {
        singleFlight.cleanupExpiredCalls();
    }

    /**
     * 获取当前活跃的SingleFlight调用数量
     * @return 活跃调用数量
     */
    public int getActiveCallsCount() {
        return singleFlight.getActiveCallsCount();
    }

    /**
     * 检查指定键是否有活跃的调用
     * @param key 键
     * @return 如果有活跃调用返回true，否则返回false
     */
    public boolean hasActiveCall(String key) {
        return singleFlight.hasActiveCall(key);
    }

    /**
     * 取消指定键的调用
     * @param key 键
     * @return 如果成功取消返回true，否则返回false
     */
    public boolean cancelCall(String key) {
        return singleFlight.cancelCall(key);
    }

    /**
     * 清理所有资源
     */
    public void shutdown() {
        // 清理所有活跃的调用
        singleFlight.cleanupExpiredCalls();
        // 清空加载器
        loaders.clear();
    }
}