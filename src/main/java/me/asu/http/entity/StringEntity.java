package me.asu.http.entity;

import me.asu.http.Constants;
import me.asu.util.Bytes;

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
        return str == null ? new byte[0] : Bytes.toBytes(str);
    }

    @Override
    public String getContentType() {
        return mimeType;
    }
}
