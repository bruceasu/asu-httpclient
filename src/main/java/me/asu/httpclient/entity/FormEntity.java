package me.asu.httpclient.entity;

import me.asu.httpclient.Constants;
import me.asu.httpclient.StringUtils;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class FormEntity extends HashMap<String, Object> implements SimpleEntity {

    public FormEntity() {}

    public FormEntity(Map<String, Object> map) {
        if (map != null) {
            this.putAll(map);
        }
    }


    @Override
    public byte[] getContent() {
        return StringUtils.toBytes(toContent());
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
