package me.asu.httpclient;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import me.asu.httpclient.util.StringUtils;
import me.asu.httpclient.text.CharsetDetect;
import me.asu.httpclient.util.Strings;
import xyz.calvinwilliams.okjson.OKJSON;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import static me.asu.httpclient.Constants.UTF_8_CHARSET;

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
        T t;
        if (storeContentWithFile) {
            if (tmpFile != null) {
                throw new IOException("Not a json file");
            }
            t = OKJSON.fileToObject(tmpFile.getAbsolutePath().toLowerCase(), klass, 0);
        } else {
            String content = getContent(UTF_8_CHARSET);
            if (StringUtils.isEmpty(content)) {
                return null;
            }
            t = OKJSON.stringToObject(content, klass, 0);
        }

        if ( t == null) {
            int code = OKJSON.getErrorCode();
            String message = OKJSON.getErrorDesc();
            throw new IOException(code + ":" + message);
        }
        return t;
    }

    public <T> T getAsJsonQuietly(Class<T> klass) {
        T t;
        if (storeContentWithFile) {
            if (tmpFile != null) {
               return null;
            }
            return OKJSON.fileToObject(tmpFile.getAbsolutePath().toLowerCase(), klass, 0);
        } else {
            String content = getContent(UTF_8_CHARSET);
            if (StringUtils.isEmpty(content)) {
                return null;
            }
            return OKJSON.stringToObject(content, klass, 0);
        }
    }

    public String getContent() {
        if (charset == null) {
            return getContent(null);
        }
        return getContent(charset);
    }

    /**
     * It is typically used to return a shorter duration, with the results stored in memory.
     *
     * If you anticipate returning a relatively large file,
     * it would be preferable to use <code>InputStream getInputStream()</code>.
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
     * It is generally used to store the results in a temporary file
     * when the return value is relatively large.
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