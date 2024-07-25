package me.asu.http.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import me.asu.http.Constants;
import me.asu.util.Bytes;
import me.asu.util.JsonUtils;

@Slf4j
public class JsonEntity implements SimpleEntity {

    Object object;

    public JsonEntity(Object object) {
        this.object = object;
    }

    @Override
    public byte[] getContent() {
        return Bytes.toBytes(toContent());
    }

    public String toContent() {
        if (object == null) {
            return "";
        } else {
            try {
                return JsonUtils.stringify(object);
            } catch (JsonProcessingException e) {
                log.error("", e);
                return "";
            }
        }
    }

    @Override
    public String getContentType() {
        return Constants.MIME_JSON_UTF8;
    }
}
