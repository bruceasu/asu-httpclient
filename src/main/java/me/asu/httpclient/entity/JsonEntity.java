package me.asu.httpclient.entity;

import me.asu.httpclient.Constants;
import me.asu.httpclient.StringUtils;
import me.asu.log.Log;
import xyz.calvinwilliams.okjson.OKJSON;

public class JsonEntity implements SimpleEntity {

    Object object;

    public JsonEntity(Object object) {
        this.object = object;
    }

    @Override
    public byte[] getContent() {
        return StringUtils.toBytes(toContent());
    }

    public String toContent() {
        if (object == null) {
            return "";
        } else {
            String jsonString = OKJSON.objectToString(object, 0);
            if (jsonString == null) {
                Log.error("okjson.stringToObject failed[" + OKJSON.getErrorCode() + "][" + OKJSON.getErrorDesc() + "]");
                return null;
            }
            return jsonString;
        }
    }

    @Override
    public String getContentType() {
        return Constants.MIME_JSON_UTF8;
    }
}
