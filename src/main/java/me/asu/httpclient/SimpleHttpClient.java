package me.asu.httpclient;

import lombok.extern.slf4j.Slf4j;
import me.asu.httpclient.entity.ByteArrayEntity;
import me.asu.httpclient.entity.SimpleEntity;
import me.asu.httpclient.text.CharsetDetect;
import me.asu.httpclient.util.Bytes;
import me.asu.httpclient.util.StringUtils;

import javax.net.ssl.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.zip.GZIPInputStream;

import static me.asu.httpclient.Constants.*;

@Slf4j
public class SimpleHttpClient {

    public static SimpleHttpClient create(String url) {
        RequestOptions opts = new RequestOptions();
        opts.setUrl(url);
        return create(opts);
    }

    public static SimpleHttpClient create(RequestOptions options) {
        Objects.requireNonNull(options);
        Objects.requireNonNull(options.getUrl());
        SimpleHttpClient client = null;
        if (isHttps(options.getUrl())) {
            client = new SimpleHttpsClient();
        } else {
            client = new SimpleHttpClient();
        }
        client.setOptions(options);
        return client;
    }

    /**
     * Is it an HTTPS address?
     *
     * @param url address
     * @return true or false.
     */
    protected static boolean isHttps(String url) {
        return url.startsWith("https") || url.startsWith("HTTPS");
    }

    protected static Set<String> SEND_DATA_METHOD = new HashSet<>(Arrays.asList(METHOD_POST, METHOD_PUT, METHOD_PATCH));

    protected RequestOptions options = null;

    public synchronized RequestOptions getOptions() {
        if (options == null) {
            options = new RequestOptions();
        }
        return options;
    }

    public void setOptions(RequestOptions opts) {
        this.options = opts;
    }

    public SimpleHttpResponse sendFile(Map<String, Object> formData) throws IOException {
        SimpleEntity entity = createSendFileContent(formData);
        RequestOptions options = getOptions();
        options.setMethod(METHOD_POST);
        options.setData(entity);

        return send();
    }

    /**
     * Send the request.
     *
     * @return Response {@link SimpleHttpResponse}
     */
    public SimpleHttpResponse send() {
        String url            = options.getUrl();
        Header headers        = options.getHeaders();
        int    connectTimeout = options.getConnectTimeout();
        int    readTimeout    = options.getReadTimeout();
        String method         = options.getMethod();

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
    public CompletableFuture<SimpleHttpResponse> asyncSend() {return CompletableFuture.supplyAsync(this::send);}

    public CompletableFuture<SimpleHttpResponse> asyncSendFile(Map<String, Object> formData) {
        return CompletableFuture.supplyAsync((() -> {
            try {
                return sendFile(formData);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }));
    }

    public SimpleHttpResponse sendFile(File... files) throws IOException {
        Map<String, Object> map = new HashMap<>();
        for (File file : files) {
            map.put(file.getName(), file);
        }

        return sendFile(map);
    }

    public SimpleHttpClient post() {
        getOptions().setMethod(METHOD_POST);
        return this;
    }

    public SimpleHttpClient put() {
        getOptions().setMethod(METHOD_PUT);
        return this;
    }

    public SimpleHttpClient patch() {
        getOptions().setMethod(METHOD_PATCH);
        return this;
    }

    public SimpleHttpClient delete() {
        getOptions().setMethod(METHOD_DELETE);
        return this;
    }

    public SimpleHttpClient head() {
        getOptions().setMethod(METHOD_HEAD);
        return this;
    }

    public SimpleHttpClient get() {
        getOptions().setMethod(METHOD_GET);
        return this;
    }

    public SimpleHttpClient largeResp() {
        getOptions().setLargeResp(true);
        return this;
    }

    public SimpleHttpClient smallResp() {
        getOptions().setLargeResp(false);
        return this;
    }

    public SimpleHttpClient headers(Map<String, String> headers) {
        if (headers == null) {
            return this;
        }
        Header headersOpts = getOptions().getHeaders();
        headersOpts.setAll(headers);
        return this;
    }

    public SimpleHttpClient setHeader(String k, String v) {
        Header headersOpts = getOptions().getHeaders();
        headersOpts.set(k, v);
        return this;
    }

    public SimpleHttpClient params(Map<String, Object> params) {
        getOptions().setParams(params);
        return this;
    }

    public SimpleHttpClient connectionTimeout(int timeout) {
        getOptions().setConnectTimeout(timeout);
        return this;
    }

    public SimpleHttpClient readTimeout(int timeout) {
        getOptions().setReadTimeout(timeout);
        return this;
    }

    public SimpleHttpClient responseEncoding(String encoding) {
        getOptions().setEncoding(encoding);
        return this;
    }

    public SimpleHttpClient data(SimpleEntity entity) {
        getOptions().setData(entity);
        return this;
    }

    public SimpleHttpClient cookie(Cookie cookie) {
        getOptions().setCookie(cookie);
        return this;
    }

    public SimpleHttpClient withHttpProxy(String host, int port) {
        final Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
        return withProxy(proxy);
    }

    public SimpleHttpClient withProxy(final Proxy proxy) {
        getOptions().setProxy(proxy);
        return this;
    }

    public SimpleHttpClient withSockProxy(String host, int port) {
        final Proxy proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(host, port));
        return withProxy(proxy);
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

    protected SimpleEntity createSendFileContent(Map<String, Object> formData) throws IOException {

        UUID uuid = UUID.randomUUID();
        String boundary = "------FormBoundary" + uuid;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        for (Map.Entry<String, Object> entry : formData.entrySet()) {
            final String key = entry.getKey();
            Object val = entry.getValue();
            if (val == null) {
                val = "";
            }

            baos.write(Bytes.toBytes("--" + boundary));
            baos.write(SEPARATOR);

            if (val instanceof File) {
                readFile((File) val, key, baos);
            } else {
                String namePart = "Content-Disposition: form-data; name=\"" + key + "\"";
                baos.write(Bytes.toBytes(namePart));
                baos.write(SEPARATOR);
                baos.write(SEPARATOR);

                baos.write(Bytes.toBytes(String.valueOf(val)));
                baos.write(SEPARATOR);
            }
        }

        baos.write(Bytes.toBytes("--" + boundary + "--"));
        baos.write(SEPARATOR);

        String mimeType = MIME_MULTIPART + ";boundary=" + boundary;
        return new ByteArrayEntity(baos.toByteArray(), mimeType);
    }

    protected void readFile(File f, String key, OutputStream out) throws IOException {
        String fileNamePart =
                "Content-Disposition: form-data; name=\"" + key + "\";filename=\"" + f.getName()
                        + "\"";
        out.write(Bytes.toBytes(fileNamePart));
        out.write(SEPARATOR);
        String contentTypePart = "Content-Type: application/octet-stream";
        out.write(Bytes.toBytes(contentTypePart));
        out.write(SEPARATOR);
        out.write(SEPARATOR);

        byte[] bytes = Files.readAllBytes(f.toPath());
        out.write(bytes);
        out.write(SEPARATOR);
    }

    protected String appendParams(String url) throws UnsupportedEncodingException {
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

    protected SimpleHttpResponse doSend(HttpURLConnection conn) throws IOException {
        Header headers = options.getHeaders();
        String method = options.getMethod();
        if (SEND_DATA_METHOD.contains(method)) {
            SimpleEntity entity = options.getData();
            byte[] content;
            if (entity == null) {
                content = new byte[0];
            } else {
                content = entity.getContent();
                String contentType = entity.getContentType();
                if (!headers.has(HEADER_CONTENT_TYPE)) {
                    headers.set(HEADER_CONTENT_TYPE, contentType);
                }
            }
            headers.set(HEADER_CONTENT_LENGTH, String.valueOf(content.length));
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

    protected void setHeaders(Header headers, HttpURLConnection conn) {
        if (!headers.hasItems()) {
            for (Map.Entry<String, String> entry : headers.getAll()) {
                conn.setRequestProperty(entry.getKey(), entry.getValue());
            }
        }
    }

    protected SimpleHttpResponse fillResponse(HttpURLConnection conn) throws IOException {
        SimpleHttpResponse httpResponse = new SimpleHttpResponse();

        httpResponse.setStatusCode(conn.getResponseCode());
        httpResponse.setHeaders(conn.getHeaderFields());

        if (!getOptions().isLargeResp()) {
            byte[] bytes = readResponseToBytes(conn);
            httpResponse.setBodyBytes(bytes);
            String ct = conn.getHeaderField(HEADER_CONTENT_TYPE);
            String charset = getResponseCharset(ct);
            if (StringUtils.isEmpty(charset)) charset = getOptions().getEncoding();
            if (StringUtils.isEmpty(charset)) charset = CharsetDetect.detect(bytes);
            if (StringUtils.isEmpty(charset)) charset = DEFAULT_CHARSET;
            httpResponse.setCharset(charset);
        } else {
            File f = readResponseToFile(conn);
            httpResponse.setStoreContentWithFile(true);
            httpResponse.setTmpFile(f);
        }
        return httpResponse;
    }

    protected File readResponseToFile(HttpURLConnection conn) throws IOException {

        File f = File.createTempFile("simple-http-client-", ".tmp");
        InputStream inputStream = conn.getInputStream();
        InputStream es = conn.getErrorStream();
        if (inputStream == null) {
            inputStream = es;
        }
        String ce = conn.getHeaderField(me.asu.httpclient.Constants.HEADER_CONTENT_ENCODING);
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

    protected void inputStreamToFile(InputStream is, File f) throws IOException {
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

    protected byte[] readResponseToBytes(HttpURLConnection conn) throws IOException {
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

    protected byte[] readStreamToBytes(InputStream stream) throws IOException {
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
     * Please retrieve the encoding of the stream.
     *
     * First, check the header information, and if it is not available,
     * please use the default value.
     */
    protected String getResponseCharset(String contentType) {
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

    public static class SimpleHttpsClient extends SimpleHttpClient {

        @Override
        protected HttpURLConnection getHttpConnection(URL url, String method)
                throws Exception {
            HttpsURLConnection conn = (HttpsURLConnection) super.getHttpConnection(url, method);
            initSSL(conn);
            return conn;
        }


        private void initSSL(HttpsURLConnection conn) throws Exception {
            SSLContext     ctx    = SSLContext.getInstance("TLS");
            KeyManager[]   kms    = new KeyManager[0];
            TrustManager[] tms    = new TrustManager[]{new DefaultTrustManager()};
            SecureRandom   random = new SecureRandom();
            ctx.init(kms, tms, random);
            SSLContext.setDefault(ctx);
            conn.setHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });
        }

        private static class DefaultTrustManager implements X509TrustManager {

            @Override
            public void checkClientTrusted(X509Certificate[] arg0, String arg1)
                    throws CertificateException {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] arg0, String arg1)
                    throws CertificateException {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        }

    }
}
