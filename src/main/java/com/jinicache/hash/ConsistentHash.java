package com.jinicache.hash;

import java.util.Collection;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 一致性哈希实现
 * 用于在分布式系统中进行节点选择
 * 使用ConcurrentSkipListMap确保线程安全
 */
public class ConsistentHash<T> {
    private final int numberOfReplicas;
    private final SortedMap<Integer, T> circle = new ConcurrentSkipListMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * 构造函数
     * @param numberOfReplicas 每个节点的虚拟节点数量
     */
    public ConsistentHash(int numberOfReplicas) {
        this.numberOfReplicas = numberOfReplicas;
    }

    /**
     * 添加节点
     * @param node 节点
     */
    public void addNode(T node) {
        lock.writeLock().lock();
        try {
            for (int i = 0; i < numberOfReplicas; i++) {
                circle.put(hash(node.toString() + i), node);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 添加多个节点
     * @param nodes 节点集合
     */
    public void addAll(Collection<T> nodes) {
        for (T node : nodes) {
            addNode(node);
        }
    }

    /**
     * 移除节点
     * @param node 节点
     */
    public void removeNode(T node) {
        lock.writeLock().lock();
        try {
            for (int i = 0; i < numberOfReplicas; i++) {
                circle.remove(hash(node.toString() + i));
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 获取key对应的节点
     * @param key 键
     * @return 对应的节点
     */
    public T get(Object key) {
        lock.readLock().lock();
        try {
            if (circle.isEmpty()) {
                return null;
            }
            int hash = hash(key);
            if (!circle.containsKey(hash)) {
                SortedMap<Integer, T> tailMap = circle.tailMap(hash);
                hash = tailMap.isEmpty() ? circle.firstKey() : tailMap.firstKey();
            }
            return circle.get(hash);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 计算哈希值
     * @param key 键
     * @return 哈希值
     */
    private int hash(Object key) {
        return Math.abs(key.hashCode());
    }

    /**
     * 获取节点数量
     * @return 节点数量
     */
    public int size() {
        lock.readLock().lock();
        try {
            return circle.size() / numberOfReplicas;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 检查是否为空
     * @return 如果为空返回true
     */
    public boolean isEmpty() {
        lock.readLock().lock();
        try {
            return circle.isEmpty();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 清空所有节点
     */
    public void clear() {
        lock.writeLock().lock();
        try {
            circle.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 获取虚拟节点总数
     * @return 虚拟节点总数
     */
    public int getVirtualNodeCount() {
        lock.readLock().lock();
        try {
            return circle.size();
        } finally {
            lock.readLock().unlock();
        }
    }
}