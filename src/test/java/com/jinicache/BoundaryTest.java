package com.jinicache;

import com.jinicache.cache.LRUCache;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 边界条件测试类
 */
public class BoundaryTest {
    
    @Test
    public void testZeroCapacity() {
        assertThrows(IllegalArgumentException.class, () -> {
            new LRUCache<String, String>(0);
        });
    }
    
    @Test
    public void testNegativeCapacity() {
        assertThrows(IllegalArgumentException.class, () -> {
            new LRUCache<String, String>(-1);
        });
    }
    
    @Test
    public void testNullKey() {
        LRUCache<String, String> cache = new LRUCache<>(10);
        assertThrows(NullPointerException.class, () -> {
            cache.put(null, "value");
        });
    }
    
    @Test
    public void testNullValue() {
        LRUCache<String, String> cache = new LRUCache<>(10);
        assertThrows(NullPointerException.class, () -> {
            cache.put("key", null);
        });
    }
    
    @Test
    public void testEmptyCache() {
        LRUCache<String, String> cache = new LRUCache<>(10);
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());
        assertNull(cache.get("nonExistentKey"));
    }
    
    @Test
    public void testFullCache() {
        LRUCache<String, String> cache = new LRUCache<>(2);
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        assertEquals(2, cache.size());
        assertFalse(cache.isEmpty());
    }
} 