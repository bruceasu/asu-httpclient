package me.asu.http.entity;

import me.asu.http.Constants;
import me.asu.http.util.ObjectToMap;
import me.asu.http.util.StringUtils;
import me.asu.util.Bytes;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class FormEntity extends HashMap<String, Object> implements SimpleEntity {

    public FormEntity() {

    }

    public FormEntity(Object obj) {
        if (obj instanceof Map) {
            this.putAll((Map) obj);
        } else if (obj != null) {
            this.putAll(ObjectToMap.objectToMap(obj));
        }
    }

    public FormEntity(Map map) {
        if (map != null) {
            this.putAll(map);
        }
    }

    @Override
    public byte[] getContent() {
        return Bytes.toBytes(toContent());
    }

    public String toContent() {
        try {
            return StringUtils.encodeFormData(this);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getContentType() {
        return Constants.MIME_FORM_URLENCODED_UTF8;
    }
}
