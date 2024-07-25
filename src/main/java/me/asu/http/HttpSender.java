package me.asu.http;

import lombok.extern.slf4j.Slf4j;
import me.asu.http.entity.ByteArrayEntity;
import me.asu.http.entity.SimpleEntity;
import me.asu.http.util.StringUtils;
import me.asu.text.CharsetDetect;
import me.asu.util.Bytes;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;

@Slf4j
public class HttpSender implements SimpleHttpClient {

    protected static Set<String> SEND_DATA_METHOD = new HashSet<>(
            Arrays.asList(me.asu.http.Constants.METHOD_POST, me.asu.http.Constants.METHOD_PUT, me.asu.http.Constants.METHOD_PATCH));

    protected RequestOptions options = null;

    @Override
    public synchronized RequestOptions getOptions() {
        if (options == null) {
            options = new RequestOptions();
        }
        return options;
    }

    @Override
    public void setOptions(RequestOptions opts) {
        this.options = opts;
    }

    @Override
    public SimpleHttpResponse sendFile(Map<String, Object> formData) throws IOException {
        SimpleEntity entity = createSendFileContent(formData);
        RequestOptions options = getOptions();
        options.setMethod(me.asu.http.Constants.METHOD_POST);
        options.setData(entity);

        return send();
    }

    /**
     * 处理实际业务.
     *
     * @return 内容
     * @throws Exception 异常
     */
    @Override
    public SimpleHttpResponse send() {
        String url = options.getUrl();
        me.asu.http.Header headers = options.getHeaders();
        int connectTimeout = options.getConnectTimeout();
        int readTimeout = options.getReadTimeout();
        String method = options.getMethod();

        HttpURLConnection conn = null;
        try {
            url = appendParams(url);

            log.debug("{} {} with headers: {}, connectTimeout: {}, readTimeout:{}.", method, url,
                    headers, connectTimeout, readTimeout);

            conn = getHttpConnection(new URL(url), method);
            conn.setConnectTimeout(connectTimeout);
            conn.setReadTimeout(readTimeout);

            return doSend(conn);
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    protected HttpURLConnection getHttpConnection(URL url, String method) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setDoInput(true);
        if (SEND_DATA_METHOD.contains(method)) {
            conn.setDoOutput(true);
        }
        conn.setRequestProperty("Accept", "*/*");
        return conn;
    }

    private SimpleEntity createSendFileContent(Map<String, Object> formData) throws IOException {

        UUID uuid = UUID.randomUUID();
        String boundary = "------FormBoundary" + uuid;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        for (Entry<String, Object> entry : formData.entrySet()) {
            final String key = entry.getKey();
            Object val = entry.getValue();
            if (val == null) {
                val = "";
            }

            baos.write(Bytes.toBytes("--" + boundary));
            baos.write(me.asu.http.Constants.SEPARATOR);

            if (val instanceof File) {
                readFile((File) val, key, baos);
            } else {
                String namePart = "Content-Disposition: form-data; name=\"" + key + "\"";
                baos.write(Bytes.toBytes(namePart));
                baos.write(me.asu.http.Constants.SEPARATOR);
                baos.write(me.asu.http.Constants.SEPARATOR);

                baos.write(Bytes.toBytes(String.valueOf(val)));
                baos.write(me.asu.http.Constants.SEPARATOR);
            }
        }

        baos.write(Bytes.toBytes("--" + boundary + "--"));
        baos.write(me.asu.http.Constants.SEPARATOR);

        String mimeType = me.asu.http.Constants.MIME_MULTIPART + ";boundary=" + boundary;
        return new ByteArrayEntity(baos.toByteArray(), mimeType);
    }

    private void readFile(File f, String key, OutputStream out) throws IOException {
        String fileNamePart =
                "Content-Disposition: form-data; name=\"" + key + "\";filename=\"" + f.getName()
                        + "\"";
        out.write(Bytes.toBytes(fileNamePart));
        out.write(me.asu.http.Constants.SEPARATOR);
        String contentTypePart = "Content-Type: application/octet-stream";
        out.write(Bytes.toBytes(contentTypePart));
        out.write(me.asu.http.Constants.SEPARATOR);
        out.write(me.asu.http.Constants.SEPARATOR);

        byte[] bytes = Files.readAllBytes(f.toPath());
        out.write(bytes);
        out.write(me.asu.http.Constants.SEPARATOR);
    }

    private String appendParams(String url) throws UnsupportedEncodingException {
        Map<String, Object> params = options.getParams();
        if (params != null && !params.isEmpty()) {
            String padding = StringUtils.encodeFormData(params);
            if (url.contains("?")) {
                url += "&" + padding;
            } else {
                url += "?" + padding;
            }
        }
        return url;
    }

    private SimpleHttpResponse doSend(HttpURLConnection conn) throws IOException {
        me.asu.http.Header headers = options.getHeaders();
        String method = options.getMethod();
        //写入请求参数
        if (SEND_DATA_METHOD.contains(method)) {
            SimpleEntity entity = options.getData();
            byte[] content;
            if (entity == null) {
                content = new byte[0];
            } else {
                content = entity.getContent();
                String contentType = entity.getContentType();
                if (!headers.has(me.asu.http.Constants.HEADER_CONTENT_TYPE)) {
                    headers.set(me.asu.http.Constants.HEADER_CONTENT_TYPE, contentType);
                }
            }
            headers.set(me.asu.http.Constants.HEADER_CONTENT_LENGTH, String.valueOf(content.length));
            setHeaders(headers, conn);

            try (OutputStream out = conn.getOutputStream()) {
                out.write(content);
                out.flush();
            }
        } else {
            setHeaders(headers, conn);
        }

        return fillResponse(conn);
    }

    private void setHeaders(Header headers, HttpURLConnection conn) {
        if (!headers.hasItems()) {
            for (Entry<String, String> entry : headers.getAll()) {
                conn.setRequestProperty(entry.getKey(), entry.getValue());
            }
        }
    }

    private SimpleHttpResponse fillResponse(HttpURLConnection conn) throws IOException {
        SimpleHttpResponse httpResponse = new SimpleHttpResponse();

        httpResponse.setStatusCode(conn.getResponseCode());

        httpResponse.setHeaders(conn.getHeaderFields());
        if (getOptions().isLargeResp()) {

        } else {
            byte[] bytes = readResponseToBytes(conn);
            httpResponse.setBodyBytes(bytes);
            String ct = conn.getHeaderField(me.asu.http.Constants.HEADER_CONTENT_TYPE);
            String charset = getResponseCharset(ct);
            if (StringUtils.isEmpty(charset)) {
                charset = getOptions().getEncoding();
            }
            if (StringUtils.isEmpty(charset)) {
                charset = CharsetDetect.detect(bytes);
            }

            if (StringUtils.isEmpty(charset)) {
                charset = me.asu.http.Constants.DEFAULT_CHARSET;
            }
            httpResponse.setCharset(charset);
        }
        return httpResponse;
    }

    private File readResponseToFile(HttpURLConnection conn) throws IOException {

        File f = File.createTempFile("simple-http-client-", ".tmp");
        InputStream inputStream = conn.getInputStream();
        InputStream es = conn.getErrorStream();
        if (inputStream == null) {
            inputStream = es;
        }
        String ce = conn.getHeaderField(me.asu.http.Constants.HEADER_CONTENT_ENCODING);
        boolean isGzip = false;
        if (!StringUtils.isEmpty(ce)) {
            isGzip = ce.contains("gzip");
        }
        if (isGzip) {
            GZIPInputStream gi = new GZIPInputStream(inputStream);
            inputStreamToFile(gi, f);
        } else {
            inputStreamToFile(inputStream, f);
        }
        return f;
    }

    private void inputStreamToFile(InputStream is, File f) throws IOException {
        byte[] data = new byte[4096];
        try (FileOutputStream out = new FileOutputStream(f)) {
            do {
                int read = is.read(data);
                if (read == -1) {
                    // eof
                    break;
                }
                out.write(data, 0, read);
            } while (true);
        }
    }

    private byte[] readResponseToBytes(HttpURLConnection conn) throws IOException {
        byte[] data;
        InputStream es = conn.getErrorStream();
        if (es == null) {
            InputStream inputStream = null;
            try {
                inputStream = conn.getInputStream();
            } catch (IOException e) {
                return new byte[0];
            }
            data = readStreamToBytes(inputStream);
        } else {
            data = readStreamToBytes(es);
        }

        String ce = conn.getHeaderField(Constants.HEADER_CONTENT_ENCODING);

        boolean isGzip = false;
        if (!StringUtils.isEmpty(ce)) {
            isGzip = ce.contains("gzip");
        }
        if (isGzip) {
            GZIPInputStream gi = new GZIPInputStream(new ByteArrayInputStream(data));
            return readStreamToBytes(gi);
        } else {
            return data;
        }
    }

    private byte[] readStreamToBytes(InputStream stream) throws IOException {
        if (stream == null) {
            return new byte[0];
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int bytesRead = -1;
        while ((bytesRead = stream.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
        }
        return out.toByteArray();
    }

    /**
     * 获取流的编码。先从头信息中查找，如果没有就取默认值。
     */
    private String getResponseCharset(String contentType) {
        String charset = null;
        if (!StringUtils.isEmpty(contentType)) {
            String[] params = contentType.split(";");
            for (String param : params) {
                param = param.trim();
                if (param.startsWith("charset")) {
                    String[] pair = param.split("=", 2);
                    if (pair.length == 2 && !StringUtils.isEmpty(pair[1])) {
                        charset = pair[1].trim();
                    }
                    break;
                }
            }
        }
        return charset;
    }

}
