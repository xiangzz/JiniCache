package com.jinicache.hash;

import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * 一致性哈希实现
 * 用于在分布式系统中进行节点选择
 */
public class ConsistentHash<T> {
    private final int numberOfReplicas;
    private final SortedMap<Integer, T> circle = new TreeMap<>();

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
    public void add(T node) {
        for (int i = 0; i < numberOfReplicas; i++) {
            circle.put(hash(node.toString() + i), node);
        }
    }

    /**
     * 添加多个节点
     * @param nodes 节点集合
     */
    public void addAll(Collection<T> nodes) {
        for (T node : nodes) {
            add(node);
        }
    }

    /**
     * 移除节点
     * @param node 节点
     */
    public void remove(T node) {
        for (int i = 0; i < numberOfReplicas; i++) {
            circle.remove(hash(node.toString() + i));
        }
    }

    /**
     * 获取键对应的节点
     * @param key 键
     * @return 节点
     */
    public T get(String key) {
        if (circle.isEmpty()) {
            return null;
        }

        int hash = hash(key);
        if (!circle.containsKey(hash)) {
            SortedMap<Integer, T> tailMap = circle.tailMap(hash);
            hash = tailMap.isEmpty() ? circle.firstKey() : tailMap.firstKey();
        }

        return circle.get(hash);
    }

    /**
     * 计算哈希值
     * @param key 键
     * @return 哈希值
     */
    private int hash(String key) {
        return Math.abs(key.hashCode());
    }

    /**
     * 获取节点数量
     * @return 节点数量
     */
    public int size() {
        return circle.size() / numberOfReplicas;
    }
} 