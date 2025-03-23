package me.asu.httpclient.entity;

import me.asu.httpclient.Constants;
import me.asu.httpclient.StringUtils;

public class StringEntity implements SimpleEntity {

    final String str;
    final String mimeType;

    public StringEntity(String str) {
        this.str = str;
        this.mimeType = Constants.MIME_TEXT_UTF8;
    }

    public StringEntity(String str, String mimeType) {
        this.str = str;
        this.mimeType = mimeType;
    }

    @Override
    public byte[] getContent() {
        return str == null ? new byte[0] : StringUtils.toBytes(str);
    }

    @Override
    public String getContentType() {
        return mimeType;
    }
}
