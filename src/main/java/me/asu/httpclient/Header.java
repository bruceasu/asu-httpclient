package me.asu.httpclient;

import me.asu.httpclient.map.CaseInsensitiveMap;
import me.asu.httpclient.map.CustomKeyMap;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class Header {

    private final CustomKeyMap<String, String> items;

    private Header() {
        items = new CaseInsensitiveMap<String, String>();
    }

    public static Header create(Map<String, String> properties) {
        return new Header().addAll(properties);
    }

    public static Header create() {
        Header header = new Header();
        header.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36");
        header.set("Accept-Encoding", "gzip,deflate");
        header.set("Accept", "text/xml,application/xml,application/xhtml+xml,text/html;"
                + "q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5");
        header.set("Accept-Language", "en-US,en");
        header.set("Accept-Charset", "ISO-8859-1,*,utf-8");
        header.set("Connection", "keep-alive");
        header.set("Cache-Control", "max-age=0");
        return header;
    }

    public Header addAll(Map<String, String> map) {
        if (null != map) {
            items.putAll(map);
        }
        return this;
    }

    public Header set(String key, String value) {
        if (null != key) {
            items.put(key, value);
        }
        return this;
    }

    public Collection<String> keys() {
        return items.keySet();
    }

    public Header remove(String key) {
        items.remove(key);
        return this;
    }

    public Header clear() {
        items.clear();
        return this;
    }

    public Set<Entry<String, String>> getAll() {
        return items.entrySet();
    }

    public Header setAll(Map<String, String> map) {
        if (map != null) {
            items.putAll(map);
        }
        return this;
    }

    public String get(String key, String defaultValue) {
        String value = get(key);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    public String get(String key) {
        return items.get(key);
    }

    public int getInt(String key, int defaultValue) {
        String value = get(key);
        if (value == null) {
            return defaultValue;
        }
        return Integer.parseInt(value);
    }

    public boolean has(String key) {
        return items.containsKey(key);
    }

    public boolean hasItems() {
        return items.isEmpty();
    }
}
