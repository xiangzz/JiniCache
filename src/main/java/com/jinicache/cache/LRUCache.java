package com.jinicache.cache;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * LRU(Least Recently Used)缓存实现
 * 使用LinkedHashMap实现LRU算法，支持并发访问
 * @param <K> 键的类型
 * @param <V> 值的类型
 */
public class LRUCache<K, V> implements Cache<K, V> {
    private final Map<K, V> cache;
    private final int capacity;
    private final Lock lock;

    /**
     * 构造函数
     * @param capacity 缓存容量
     */
    public LRUCache(int capacity) {
        this.capacity = capacity;
        this.lock = new ReentrantLock();
        // 使用LinkedHashMap实现LRU，accessOrder=true表示按访问顺序排序
        this.cache = new LinkedHashMap<K, V>(capacity, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > capacity;
            }
        };
    }

    @Override
    public V get(K key) {
        lock.lock();
        try {
            return cache.get(key);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void put(K key, V value) {
        lock.lock();
        try {
            cache.put(key, value);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void remove(K key) {
        lock.lock();
        try {
            cache.remove(key);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void clear() {
        lock.lock();
        try {
            cache.clear();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int size() {
        lock.lock();
        try {
            return cache.size();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean isEmpty() {
        lock.lock();
        try {
            return cache.isEmpty();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 获取缓存容量
     * @return 缓存容量
     */
    public int getCapacity() {
        return capacity;
    }
} 