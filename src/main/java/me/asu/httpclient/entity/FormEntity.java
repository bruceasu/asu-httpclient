package me.asu.httpclient.entity;

import me.asu.httpclient.Constants;
import me.asu.httpclient.util.ObjectToMap;
import me.asu.httpclient.util.StringUtils;
import me.asu.httpclient.util.Bytes;

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
