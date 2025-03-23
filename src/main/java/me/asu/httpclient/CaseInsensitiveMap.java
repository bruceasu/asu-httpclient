package me.asu.httpclient;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 忽略大小写的Map<br>
 * 对KEY忽略大小写，get("Value")和get("value")获得的值相同，put进入的值也会被覆盖
 *
 * @param <V> 值类型
 * @author Looly
 * @since 3.0.2
 */
public class CaseInsensitiveMap<V> {
    private static final long serialVersionUID = 4043263744224569870L;
    protected Map<String, V> map;

    /**
     * 构造
     */
    public CaseInsensitiveMap() {
        map = new HashMap<>();
    }

    /**
     * 构造
     *
     * @param initialCapacity 初始大小
     * @param loadFactor      加载因子
     */
    public CaseInsensitiveMap(int initialCapacity, float loadFactor) {
        map = new HashMap<>(initialCapacity, loadFactor);
    }

    /**
     * 构造
     *
     * @param initialCapacity 初始大小
     */
    public CaseInsensitiveMap(int initialCapacity) {
        this(initialCapacity, 0.75f);
    }

    /**
     * 构造
     *
     * @param m Map
     */
    public CaseInsensitiveMap(Map<String, ? extends V> m) {
        map = new HashMap<>((int) (m.size() / 0.75));
        putAll(m);
    }

    /**
     * 构造
     *
     * @param loadFactor 加载因子
     * @param m          Map
     * @since 3.1.2
     */
    public CaseInsensitiveMap(float loadFactor, Map<String, ? extends V> m) {
        map = new HashMap<>(m.size(), loadFactor);
        putAll(m);
    }

    /**
     * 将Key转为小写
     *
     * @param key KEY
     * @return 小写KEY
     */
    protected String customKey(Object key) {
        if (null != key) {
            return key.toString().toLowerCase();
        }
        return "";
    }

    public void putAll(Map<String, ? extends V> m) {
        for (Map.Entry<String, ? extends V> entry : m.entrySet()) {
            map.put(entry.getKey(), entry.getValue());
        }
    }

    public V get(Object key) {
        return map.get(customKey(key));
    }

    @SuppressWarnings("unchecked")
    public V put(String key, V value) {
        return map.put(customKey(key), value);
    }

    public boolean containsKey(Object key) {
        return map.containsKey(customKey(key));
    }

    public Collection<String> keySet() {
        return map.keySet();
    }

    public void remove(String key) {
        map.remove(customKey(key));
    }

    public void clear() {
        map.clear();
    }

    public Set<Map.Entry<String, V>> entrySet() {
        return map.entrySet();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }


}
