package me.asu.httpclient.entity;

import me.asu.httpclient.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Map;
import java.util.UUID;

import static me.asu.httpclient.Constants.*;

public class MultipartEntity implements SimpleEntity {

    byte[] content;
    String mimeType;

    public MultipartEntity(Map<String, Object> map) {
        try {
            init(map);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public byte[] getContent() {
        return content == null ? new byte[0] : content;
    }

    @Override
    public String getContentType() {
        return mimeType;
    }

    protected void init(Map<String, Object> formData) throws IOException {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String boundary = "------FormBoundary" + uuid;

        ByteArrayOutputStream os = new ByteArrayOutputStream();

        for (Map.Entry<String, Object> entry : formData.entrySet()) {
            final String key = entry.getKey();
            Object val = entry.getValue();
            if (val == null) {
                val = "";
            }

            os.write(StringUtils.toBytes("--" + boundary));
            os.write(SEPARATOR);

            if (val instanceof File) {
                readFile((File) val, key, os);
            } else {
                String namePart = "Content-Disposition: form-data; name=\"" + key + "\"";
                os.write(StringUtils.toBytes(namePart));
                os.write(SEPARATOR);
                os.write(SEPARATOR);

                os.write(StringUtils.toBytes(String.valueOf(val)));
                os.write(SEPARATOR);
            }
        }

        os.write(StringUtils.toBytes("--" + boundary + "--"));
        os.write(SEPARATOR);

        this.mimeType = MIME_MULTIPART + ";boundary=" + boundary;
        this.content = os.toByteArray();
    }

    protected void readFile(File f, String key, OutputStream out) throws IOException {
        String fileNamePart = String.format("Content-Disposition: form-data; name=\"%s\";filename=\"%s\"", key, f.getName());
        out.write(StringUtils.toBytes(fileNamePart));
        out.write(SEPARATOR);
        out.write(StringUtils.toBytes("Content-Type: " + MIME_OCTET_STREAM));
        out.write(SEPARATOR);
        out.write(SEPARATOR);

        byte[] bytes = Files.readAllBytes(f.toPath());
        out.write(bytes);
        out.write(SEPARATOR);
    }
}
