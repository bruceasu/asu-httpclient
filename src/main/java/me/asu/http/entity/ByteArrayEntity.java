package me.asu.http.entity;

import me.asu.http.Constants;

public class ByteArrayEntity implements SimpleEntity {

    final byte[] content;
    final String mimeType;

    public ByteArrayEntity(byte[] content) {
        this.content = content;
        this.mimeType = Constants.MIME_OCTET_STREAM;
    }

    public ByteArrayEntity(byte[] content, String mimeType) {
        this.content = content;
        this.mimeType = mimeType;
    }


    @Override
    public byte[] getContent() {
        return content == null ? new byte[0] : content;
    }

    @Override
    public String getContentType() {
        return mimeType;
    }
}
