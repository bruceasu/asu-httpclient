package me.asu.http;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import me.asu.http.util.StringUtils;
import me.asu.text.CharsetDetect;
import me.asu.util.JsonUtils;
import me.asu.util.Strings;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

@Data
@Slf4j
public class SimpleHttpResponse {


    int statusCode;
    Map<String, List<String>> headers;
    byte[] bodyBytes;
    File tmpFile;
    boolean storeContentWithFile = false;
    String charset;

    /**
     * 假设content是json数据, 如果不是json数据则抛JSONException异常。
     */
    public <T> T getAsJson(Class<T> klass) throws IOException {
        String content = getContent("UTF-8");
        if (StringUtils.isEmpty(content)) {
            return null;
        }
        return JsonUtils.toJson(content, klass);
    }

    public String getContent() {
        if (charset == null) {
            return getContent(null);
        }
        return getContent(charset);
    }

    /**
     * 通常用于返回比较小时，结果保存在内存中。
     * 如果预期返回比较大的文件，使用<code>InputStream getInputStream()</code>
     * 比较好。
     */
    public String getContent(String charset) {
        try {
            if (storeContentWithFile) {
                if (tmpFile == null || !tmpFile.exists()) {
                    return "";
                }
                byte[] bytes = Files.readAllBytes(tmpFile.toPath());
                if (bytes == null || bytes.length == 0) {
                    return "";
                }
                if (Strings.isEmpty(charset)) {
                    charset = CharsetDetect.detect(bytes);
                }
                if (Strings.isEmpty(charset)) {
                    return new String(bytes, Charset.defaultCharset());
                } else {
                    return new String(bytes, charset);
                }

            } else {
                if (bodyBytes == null) {
                    return "";
                }
                if (Strings.isEmpty(charset)) {
                    charset = CharsetDetect.detect(bodyBytes);
                }
                if (Strings.isEmpty(charset)) {
                    return new String(bodyBytes, Charset.defaultCharset());
                } else {
                    return new String(bodyBytes, charset);
                }
            }
        } catch (Exception e) {
            log.error("", e);
            return null;
        }

    }

    /**
     * 通常用于返回比较大时，结果保存在临时文件中。
     *
     * @return
     * @throws IOException
     */
    public InputStream getInputStream() throws IOException {
        if (storeContentWithFile) {
            return new FileInputStream(tmpFile);
        } else {
            return new ByteArrayInputStream(bodyBytes);
        }
    }
}