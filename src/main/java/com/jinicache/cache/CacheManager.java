package com.jinicache.cache;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 缓存管理器
 * 用于管理多个缓存组
 */
public class CacheManager {
    private final ConcurrentHashMap<String, Group> groups;

    /**
     * 构造函数
     */
    public CacheManager() {
        this.groups = new ConcurrentHashMap<>();
    }

    /**
     * 创建新的缓存组
     * @param name 组名
     * @param cache 底层缓存实现
     * @return 新创建的缓存组
     */
    public Group createGroup(String name, Cache<String, byte[]> cache) {
        Group group = new Group(name, cache);
        Group existingGroup = groups.putIfAbsent(name, group);
        return existingGroup != null ? existingGroup : group;
    }

    /**
     * 获取缓存组
     * @param name 组名
     * @return 缓存组，如果不存在返回null
     */
    public Group getGroup(String name) {
        return groups.get(name);
    }

    /**
     * 删除缓存组
     * @param name 组名
     */
    public void removeGroup(String name) {
        groups.remove(name);
    }

    /**
     * 获取所有缓存组
     * @return 缓存组集合
     */
    public ConcurrentHashMap<String, Group> getGroups() {
        return groups;
    }
} 