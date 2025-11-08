package me.asu.httpclient.entity;

import me.asu.httpclient.Constants;
import me.asu.httpclient.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class FormEntity implements SimpleEntity {
    final HashMap<String, Object> map = new HashMap<>();
    public FormEntity() {}

    public FormEntity(Map<String, Object> map) {
        if (map != null) {
            this.map.putAll(map);
        }
    }

    public Object get(Object key) {return map.get(key);}

    public Object put(String key, Object value) {return map.put(key, value);}

    public Object remove(Object key) {return map.remove(key);}

    public void putAll(Map<? extends String, ?> m) {map.putAll(m);}

    public boolean containsValue(Object value) {return map.containsValue(value);}

    public Set<Map.Entry<String, Object>> entrySet() {return map.entrySet();}

    @Override
    public byte[] getContent() {
        return StringUtils.toBytes(toContent());
    }

    public String toContent() {
        try {
            return StringUtils.encodeFormData(this.map);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getContentType() {
        return Constants.MIME_FORM_URLENCODED_UTF8;
    }
}