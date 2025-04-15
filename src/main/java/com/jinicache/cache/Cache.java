package com.jinicache.cache;

/**
 * 缓存接口定义
 * @param <K> 键的类型
 * @param <V> 值的类型
 */
public interface Cache<K, V> {
    /**
     * 获取缓存值
     * @param key 键
     * @return 缓存的值，如果不存在返回null
     */
    V get(K key);

    /**
     * 添加或更新缓存
     * @param key 键
     * @param value 值
     */
    void put(K key, V value);

    /**
     * 删除缓存
     * @param key 键
     */
    void remove(K key);

    /**
     * 清空缓存
     */
    void clear();

    /**
     * 获取缓存大小
     * @return 缓存中的键值对数量
     */
    int size();

    /**
     * 判断缓存是否为空
     * @return 如果缓存为空返回true
     */
    boolean isEmpty();
} 