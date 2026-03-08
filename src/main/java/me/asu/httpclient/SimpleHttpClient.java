package me.asu.httpclient;

import me.asu.httpclient.entity.*;
import me.asu.httpclient.util.StringUtils;
import me.asu.log.Log;

import javax.net.ssl.*;
import java.io.*;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;

import static me.asu.httpclient.Constants.*;

public class SimpleHttpClient {
    private static final boolean JDK_HTTP_CLIENT_AVAILABLE = detectJdkHttpClientAvailable();

    public static HttpSenderBuilder builder() {
        return new HttpSenderBuilder();
    }

    public static Set<String> SEND_DATA_METHOD = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(METHOD_POST, METHOD_PUT, METHOD_PATCH)));

    /**
     * Send the request.
     *
     * @return Response {@link SimpleHttpResponse}
     */

    public static SimpleHttpResponse send(RequestOptions options) {
        return createSender(options).send();
    }

    public static CompletableFuture<SimpleHttpResponse> asyncSend(final RequestOptions opts) {
        return createSender(opts).asyncSend();
    }

    public static String post(String url, Map<String, Object> data) {
        return post(String.class, url,  data);
    }

    public static SimpleHttpResponse fetch(String url) {
        Objects.requireNonNull(url);
        RequestOptions options = new RequestOptions();
        options.setUrl(url);
        options.setMethod(METHOD_GET);
        return send(options);
    }

    public static SimpleHttpResponse fetch(String url, Consumer<RequestOptions> init) {
        Objects.requireNonNull(url);
        RequestOptions options = new RequestOptions();
        options.setUrl(url);
        options.setMethod(METHOD_GET);
        if (init != null) {
            init.accept(options);
        }
        return send(options);
    }

    public static SimpleHttpResponse fetch(String url, Map<String, Object> init) {
        Objects.requireNonNull(url);
        RequestOptions options = new RequestOptions();
        options.setUrl(url);
        options.setMethod(METHOD_GET);
        applyFetchInit(options, init);
        return send(options);
    }

    public static CompletableFuture<SimpleHttpResponse> fetchAsync(String url) {
        Objects.requireNonNull(url);
        return fetchAsync(url, (Consumer<RequestOptions>) null);
    }

    public static CompletableFuture<SimpleHttpResponse> fetchAsync(String url, Consumer<RequestOptions> init) {
        Objects.requireNonNull(url);
        RequestOptions options = new RequestOptions();
        options.setUrl(url);
        options.setMethod(METHOD_GET);
        if (init != null) {
            init.accept(options);
        }
        return asyncSend(options);
    }

    public static CompletableFuture<SimpleHttpResponse> fetchAsync(String url, Map<String, Object> init) {
        Objects.requireNonNull(url);
        RequestOptions options = new RequestOptions();
        options.setUrl(url);
        options.setMethod(METHOD_GET);
        applyFetchInit(options, init);
        return asyncSend(options);
    }

    public static String fetchText(String url) {
        return fetch(url).getContent();
    }

    public static String fetchText(String url, Consumer<RequestOptions> init) {
        return fetch(url, init).getContent();
    }

    public static String fetchText(String url, Map<String, Object> init) {
        return fetch(url, init).getContent();
    }

    public static <T> T fetchJson(Class<T> type, String url) {
        return toResult(type, fetch(url));
    }

    public static <T> T fetchJson(Class<T> type, String url, Consumer<RequestOptions> init) {
        return toResult(type, fetch(url, init));
    }

    public static <T> T fetchJson(Class<T> type, String url, Map<String, Object> init) {
        return toResult(type, fetch(url, init));
    }

    private static void applyFetchInit(RequestOptions options, Map<String, Object> init) {
        if (options == null || init == null || init.isEmpty()) {
            return;
        }
        Object method = init.get("method");
        if (method != null) {
            options.setMethod(String.valueOf(method));
        }
        Object headers = init.get("headers");
        if (headers instanceof Header) {
            options.setHeaders((Header) headers);
        } else if (headers instanceof Map<?, ?>) {
            Header header = Header.create();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) headers).entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    header.set(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
                }
            }
            options.setHeaders(header);
        }
        Object params = init.get("params");
        if (params instanceof Map<?, ?>) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) params).entrySet()) {
                if (entry.getKey() != null) {
                    options.getQueries().put(String.valueOf(entry.getKey()), entry.getValue());
                }
            }
        }
        Object query = init.get("query");
        if (query instanceof Map<?, ?>) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) query).entrySet()) {
                if (entry.getKey() != null) {
                    options.getQueries().put(String.valueOf(entry.getKey()), entry.getValue());
                }
            }
        }
        Object data = init.get("data");
        if (data instanceof SimpleEntity) {
            options.setData((SimpleEntity) data);
        } else if (data instanceof byte[]) {
            options.setData(new ByteArrayEntity((byte[]) data));
        } else if (data instanceof Map<?, ?>) {
            Map<String, Object> map = new HashMap<>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) data).entrySet()) {
                if (entry.getKey() != null) {
                    map.put(String.valueOf(entry.getKey()), entry.getValue());
                }
            }
            boolean hasFile = map.values().stream().anyMatch(v -> v instanceof File || v instanceof Path);
            options.setData(hasFile ? new MultipartEntity(map) : new FormEntity(map));
        } else if (data != null) {
            options.setData(new StringEntity(String.valueOf(data)));
        }
        Object json = init.get("json");
        if (json != null) {
            options.setData(new JsonEntity(json));
        }
        Object connectTimeout = init.get("connectTimeout");
        if (connectTimeout instanceof Number) {
            options.setConnectTimeout(((Number) connectTimeout).intValue());
        }
        Object readTimeout = init.get("readTimeout");
        if (readTimeout instanceof Number) {
            options.setReadTimeout(((Number) readTimeout).intValue());
        }
        Object encoding = init.get("encoding");
        if (encoding != null) {
            options.setEncoding(String.valueOf(encoding));
        }
        Object proxy = init.get("proxy");
        if (proxy instanceof Proxy) {
            options.setProxy((Proxy) proxy);
        }
        Object largeResp = init.get("largeResp");
        if (largeResp instanceof Boolean) {
            options.setLargeResp((Boolean) largeResp);
        }
    }
    public static  <T> T post( Class<T> type, String url,Map<String, Object> data) {
        Objects.requireNonNull(url);
        SimpleHttpResponse resp = builder().url(url).method(METHOD_POST).data(data).build().send();
        return toResult(type, resp);
    }

    public static <T> T post( Class<T> type, String url, Object ... data) {
        Objects.requireNonNull(url);
        SimpleHttpResponse resp = builder().url(url).method(METHOD_POST).data(data).build().send();
        return toResult(type, resp);
    }
    public static String posJson(String url,  Map<String, Object> data) {
        return posJson(url, String.class, data);
    }
    public static  <T> T posJson(String url, Class<T> type, Map<String, Object> data) {
        Objects.requireNonNull(url);
        SimpleHttpResponse resp = builder().url(url).method(METHOD_POST).json(data).build().send();
        return toResult(type, resp);
    }

    public static <T> T posJson(String url, Class<T> type, Object ... data) {
        Objects.requireNonNull(url);
        SimpleHttpResponse resp = builder().url(url).method(METHOD_POST).json(data).build().send();
        return toResult(type, resp);
    }

    public static String put(String url, Map<String, Object> data) {
        return put(url, String.class , data);
    }

    public static <T> T put(String url, Class<T> type, Map<String, Object> data) {
        Objects.requireNonNull(url);
        SimpleHttpResponse resp = builder().url(url).method(METHOD_PUT).data(data).build().send();
        return toResult(type, resp);
    }

    public static <T> T put(String url, Class<T> type, Object ... data) {
        Objects.requireNonNull(url);
        SimpleHttpResponse resp = builder().url(url).method(METHOD_PUT).data(data).build().send();
        return toResult(type, resp);
    }

    public static String putJson(String url, Map<String, Object> data) {
        return putJson(url, String.class , data);
    }

    public static <T> T putJson(String url, Class<T> type, Map<String, Object> data) {
        Objects.requireNonNull(url);
        SimpleHttpResponse resp = builder().url(url).method(METHOD_PUT).json(data).build().send();
        return toResult(type, resp);
    }

    public static <T> T putJson(String url, Class<T> type, Object ... data) {
        Objects.requireNonNull(url);
        SimpleHttpResponse resp = builder().url(url).method(METHOD_PUT).json(data).build().send();
        return toResult(type, resp);
    }

    public static String patch(String url, Map<String, Object> data) {
        return patch(url, String.class, data);
    }

    public static <T> T patch(String url, Class<T> type, Map<String, Object> data) {
        Objects.requireNonNull(url);
        SimpleHttpResponse resp = builder().url(url).method(METHOD_PATCH).data(data).build().send();
        return toResult(type, resp);
    }

    public static <T> T patch(String url, Class<T> type, Object ... data) {
        Objects.requireNonNull(url);
        SimpleHttpResponse resp = builder().url(url).method(METHOD_PATCH).data(data).build().send();
        return toResult(type, resp);
    }

    public static String patchJson(String url, Map<String, Object> data) {
        return patchJson(url, String.class, data);
    }

    public static <T> T patchJson(String url, Class<T> type, Map<String, Object> data) {
        Objects.requireNonNull(url);
        SimpleHttpResponse resp = builder().url(url).method(METHOD_PATCH).json(data).build().send();
        return toResult(type, resp);
    }

    public static <T> T patchJson(String url, Class<T> type, Object ... data) {
        Objects.requireNonNull(url);
        SimpleHttpResponse resp = builder().url(url).method(METHOD_PATCH).json(data).build().send();
        return toResult(type, resp);
    }

    public static String delete(String url, Object... args) {
        return delete(url, String.class, args);
    }

    public static <T> T delete(String url,  Class<T> type, Object... args) {
        Objects.requireNonNull(url);
        SimpleHttpResponse resp = builder().url(url).method(METHOD_DELETE).params(args).build().send();
        return toResult(type, resp);
    }

    public static <T> T delete(String url,  Class<T> type, Map<String, Object> args) {
        Objects.requireNonNull(url);
        SimpleHttpResponse resp = builder().url(url).method(METHOD_DELETE).params(args).build().send();
        return toResult(type, resp);
    }

    public static Map<String, List<String>> head(String url, Object... args) {
        Objects.requireNonNull(url);
        return builder().url(url).method(METHOD_HEAD).params(args).build().send().getHeaders();
    }

    public static Map<String, List<String>>  head(String url, Map<String, Object> args) {
        Objects.requireNonNull(url);
        return builder().url(url).method(METHOD_HEAD).params(args).build().send().getHeaders();
    }

    public static String get(String url, Object... args) {
       return get(url, String.class, args);
    }

    public static <T> T get(String url, Class<T> type, Map<String, Object> args) {
        Objects.requireNonNull(url);
        SimpleHttpResponse resp = builder().url(url).method(METHOD_GET).params(args).build().send();
        return toResult(type, resp);
    }

    public static <T> T get(String url, Class<T> type, Object... args) {
        Objects.requireNonNull(url);
        SimpleHttpResponse resp = builder().url(url).method(METHOD_GET).params(args).build().send();
        return toResult(type, resp);
    }

    public static String getJson(String url, Object... args) {
        Objects.requireNonNull(url);
        return getJson(url, String.class, args);
    }

    public static <T> T getJson(String url, Class<T> type, Map<String, Object> args) {
        Objects.requireNonNull(url);
        SimpleHttpResponse resp = builder().url(url).method(METHOD_GET).json(args).build().send();
        return toResult(type, resp);
    }

    public static <T> T getJson(String url, Class<T> type, Object... args) {
        Objects.requireNonNull(url);
        SimpleHttpResponse resp = builder().url(url).method(METHOD_GET).json(args).build().send();
        return toResult(type, resp);
    }

    public static SimpleHttpResponse sendFile(String url, File... files) {
        Objects.requireNonNull(url);
        Objects.requireNonNull(files);
        Map<String, Object> map = new HashMap<>();
        for (File file : files) {
            map.put(file.getName(), file);
        }
        return builder().url(url).data(map).build().send();
    }

    public static SimpleHttpResponse sendFile(String url, Map<String, Object> formData, File... files) throws IOException {
        Objects.requireNonNull(url);
        if (formData == null) return  sendFile(url, files);
        for (File file : files) {
            formData.put(file.getName(), file);
        }
        return  builder().url(url).data(formData).build().send();
    }

    public static String sendFile(String url, String field, File file) {
        Objects.requireNonNull(url);
        Objects.requireNonNull(file);
        return builder().url(url).method(METHOD_POST).data(field, file).build().send().getContent();
    }
    public static String sendFile(String url, File file) {
        return sendFile(url, "file", file);
    }

    public static String sendFile(String url, Path file) {
        Objects.requireNonNull(url);
        Objects.requireNonNull(file);
        return builder().url(url).method(METHOD_POST).data(file).build().send().getContent();
    }

    /**
     * Is it an HTTPS address?
     *
     * @param url address
     * @return true or false.
     */
    static boolean isHttps(String url) {
        return url != null && url.trim().toLowerCase().startsWith("https");
    }

    static HttpSender createSender(RequestOptions options) {
        RequestOptions opts = options == null ? new RequestOptions() : options;
        if (shouldUseJdkHttpClient()) {
            return new JdkHttpClientSender(opts);
        }
        return isHttps(opts.getUrl()) ? new HttpsSender(opts): new HttpSender(opts);
    }

    static boolean shouldUseJdkHttpClient() {
        return JDK_HTTP_CLIENT_AVAILABLE && detectJavaFeatureVersion() >= 11;
    }

    private static boolean detectJdkHttpClientAvailable() {
        try {
            Class.forName("java.net.http.HttpClient");
            return true;
        } catch (Throwable ignore) {
            return false;
        }
    }

    private static int detectJavaFeatureVersion() {
        String version = System.getProperty("java.specification.version");
        if (version == null || version.trim().isEmpty()) {
            return 0;
        }
        version = version.trim();
        if (version.startsWith("1.")) {
            version = version.substring(2);
        }
        int dot = version.indexOf('.');
        if (dot > 0) {
            version = version.substring(0, dot);
        }
        try {
            return Integer.parseInt(version);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static <T> T toResult(Class<T> type, SimpleHttpResponse resp) {
        if (type == SimpleHttpResponse.class) {
            return (T) resp;
        }
        if (type == byte[].class) {
            return (T) resp.getBody();
        }
        if (type == String.class) {
            return (T) resp.getContent();
        }
        if (type == null) {
            return (T) resp.getContent();
        }
        return resp.getAsJsonQuietly(type);
    }

    public static class HttpSender {
        protected RequestOptions options;

        HttpSender(RequestOptions options) {
            this.options = options == null ? new RequestOptions() : options;
        }

        public String method() {return options.getMethod();}

        public void method(String method) {options.setMethod(method);}

        public Map<String, Object> queries() {return options.getQueries();}

        public void queries(Map<String, Object> params) {options.setQueries(params);}

        public void query(String name, Object value) {options.getQueries().put(name, value);}

        public Header headers() {return options.getHeaders();}

        public void headers(Header headers) {options.setHeaders(headers);}

        public void connectTimeout(int connectTimeout) {options.setConnectTimeout(connectTimeout);}

        public int connectTimeout() {return options.getConnectTimeout();}

        public void encoding(String encoding) {options.setEncoding(encoding);}

        public String encoding() {return options.getEncoding();}

        public void readTimeout(int readTimeout) {options.setReadTimeout(readTimeout);}

        public int readTimeout() {return options.getReadTimeout();}

        public void data(SimpleEntity data) {options.setData(data);}

        public SimpleEntity data() {return options.getData();}

        public void proxy(Proxy proxy) {options.setProxy(proxy);}

        public Proxy proxy() {return options.getProxy();}

        public String url() {return options.getUrl();}

        public void url(String url) {options.setUrl(url);}

        public boolean largeResp() {return options.isLargeResp();}

        public void largeResp(boolean largeResp) {options.setLargeResp(largeResp);}

        public Cookie cookie() {return options.cookie();}

        public void cookie(Cookie cookie) { options.cookie(cookie);}

        public SimpleHttpResponse sendFile(File... files) throws IOException {
            Map<String, Object> map = new HashMap<>();
            for (File file : files) {
                map.put(file.getName(), file);
            }

            return sendFile(map);
        }

        public SimpleHttpResponse sendFile(Map<String, Object> formData) throws IOException {
            SimpleEntity entity = new MultipartEntity(formData);
            if (!SEND_DATA_METHOD.contains(options.getMethod())) options.setMethod(METHOD_POST);
            options.setData(entity);

            return send();
        }

        /**
         * Send the request.
         *
         * @return Response {@link SimpleHttpResponse}
         */
        public SimpleHttpResponse send() {
            String url = options.getUrl();
            int connectTimeout = options.getConnectTimeout();
            int readTimeout = options.getReadTimeout();
            String method = options.getMethod();

            HttpURLConnection conn = null;
            try {
                url = appendParams(url);
                if (shouldUseSocketPatchFallback(url, method)) {
                    return sendPatchWithSocket(url, connectTimeout, readTimeout);
                }
                conn = getHttpConnection(new URL(url), method);
                conn.setConnectTimeout(connectTimeout);
                conn.setReadTimeout(readTimeout);

                return doSend(conn);
            } catch (Throwable e) {
                Log.error(e.getMessage(), e);
                throw new RuntimeException(e);
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }

        protected boolean shouldUseSocketPatchFallback(String url, String method) {
            return METHOD_PATCH.equalsIgnoreCase(method)
                    && !shouldUseJdkHttpClient()
                    && !isHttps(url)
                    && options.getProxy() == null;
        }

        protected SimpleHttpResponse sendPatchWithSocket(String url, int connectTimeout, int readTimeout) throws IOException {
            URL u = new URL(url);
            String host = u.getHost();
            int port = u.getPort() > 0 ? u.getPort() : 80;
            String path = u.getFile();
            if (StringUtils.isEmpty(path)) {
                path = "/";
            }

            Header headers = options.getHeaders();
            SimpleEntity entity = options.getData();
            byte[] content = entity == null ? new byte[0] : entity.getContent();
            if (entity != null && !headers.has(HEADER_CONTENT_TYPE)) {
                headers.set(HEADER_CONTENT_TYPE, entity.getContentType());
            }
            if (!headers.has("Host")) {
                headers.set("Host", port == 80 ? host : host + ":" + port);
            }
            if (!headers.has("Connection")) {
                headers.set("Connection", "close");
            }
            headers.set(HEADER_CONTENT_LENGTH, String.valueOf(content.length));

            StringBuilder request = new StringBuilder();
            request.append("PATCH ").append(path).append(" HTTP/1.1").append("\r\n");
            if (!headers.hasItems()) {
                for (Map.Entry<String, String> entry : headers.getAll()) {
                    request.append(entry.getKey()).append(": ").append(entry.getValue()).append("\r\n");
                }
            }
            request.append("\r\n");

            byte[] responseBytes;
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, port), connectTimeout);
                socket.setSoTimeout(readTimeout);
                OutputStream outputStream = socket.getOutputStream();
                outputStream.write(request.toString().getBytes(StandardCharsets.ISO_8859_1));
                if (content.length > 0) {
                    outputStream.write(content);
                }
                outputStream.flush();
                responseBytes = readStreamToBytes(socket.getInputStream());
            }
            return parseSocketResponse(responseBytes);
        }

        protected SimpleHttpResponse parseSocketResponse(byte[] rawResponseBytes) throws IOException {
            int headerEnd = indexOfHeaderEnd(rawResponseBytes);
            if (headerEnd < 0) {
                throw new IOException("Invalid HTTP response");
            }

            String head = new String(rawResponseBytes, 0, headerEnd, StandardCharsets.ISO_8859_1);
            String[] lines = head.split("\r\n");
            if (lines.length == 0) {
                throw new IOException("Invalid HTTP status line");
            }

            int statusCode = 0;
            String[] statusParts = lines[0].split(" ");
            if (statusParts.length >= 2) {
                statusCode = Integer.parseInt(statusParts[1]);
            }

            Map<String, List<String>> responseHeaders = new LinkedHashMap<>();
            responseHeaders.put(null, Collections.singletonList(lines[0]));
            for (int i = 1; i < lines.length; i++) {
                String line = lines[i];
                int sep = line.indexOf(':');
                if (sep <= 0) {
                    continue;
                }
                String key = line.substring(0, sep).trim();
                String value = line.substring(sep + 1).trim();
                responseHeaders.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
            }

            byte[] body = Arrays.copyOfRange(rawResponseBytes, headerEnd + 4, rawResponseBytes.length);
            String transferEncoding = firstHeaderValue(responseHeaders, "Transfer-Encoding");
            if (!StringUtils.isEmpty(transferEncoding) && transferEncoding.toLowerCase(Locale.ROOT).contains("chunked")) {
                body = decodeChunkedBody(body);
            }

            String contentEncoding = firstHeaderValue(responseHeaders, HEADER_CONTENT_ENCODING);
            if (!StringUtils.isEmpty(contentEncoding) && contentEncoding.toLowerCase(Locale.ROOT).contains("gzip")) {
                body = readStreamToBytes(new GZIPInputStream(new ByteArrayInputStream(body)));
            }

            SimpleHttpResponse httpResponse = new SimpleHttpResponse();
            httpResponse.setStatusCode(statusCode);
            httpResponse.setHeaders(responseHeaders);

            if (!options.isLargeResp()) {
                httpResponse.setBodyBytes(body);
                String contentType = firstHeaderValue(responseHeaders, HEADER_CONTENT_TYPE);
                String charset = getResponseCharset(contentType);
                if (StringUtils.isEmpty(charset)) charset = options.getEncoding();
                if (StringUtils.isEmpty(charset)) charset = DEFAULT_CHARSET;
                httpResponse.setCharset(charset);
            } else {
                File f = File.createTempFile("simple-http-client-", ".tmp");
                Files.write(f.toPath(), body);
                httpResponse.setStoreContentWithFile(true);
                httpResponse.setTmpFile(f);
            }
            return httpResponse;
        }

        protected int indexOfHeaderEnd(byte[] raw) {
            if (raw == null || raw.length < 4) {
                return -1;
            }
            for (int i = 0; i <= raw.length - 4; i++) {
                if (raw[i] == '\r' && raw[i + 1] == '\n' && raw[i + 2] == '\r' && raw[i + 3] == '\n') {
                    return i;
                }
            }
            return -1;
        }

        protected String firstHeaderValue(Map<String, List<String>> headers, String key) {
            if (headers == null || StringUtils.isEmpty(key)) {
                return null;
            }
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(key)) {
                    List<String> values = entry.getValue();
                    return (values == null || values.isEmpty()) ? null : values.get(0);
                }
            }
            return null;
        }

        protected byte[] decodeChunkedBody(byte[] chunked) throws IOException {
            ByteArrayInputStream in = new ByteArrayInputStream(chunked);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            while (true) {
                String sizeLine = readLine(in);
                if (sizeLine == null) {
                    break;
                }
                int semicolon = sizeLine.indexOf(';');
                String hex = semicolon > 0 ? sizeLine.substring(0, semicolon) : sizeLine;
                int size = Integer.parseInt(hex.trim(), 16);
                if (size == 0) {
                    readLine(in);
                    break;
                }
                byte[] buffer = new byte[size];
                int offset = 0;
                while (offset < size) {
                    int read = in.read(buffer, offset, size - offset);
                    if (read < 0) {
                        throw new EOFException("Unexpected EOF in chunked body");
                    }
                    offset += read;
                }
                out.write(buffer);
                readLine(in);
            }
            return out.toByteArray();
        }

        protected String readLine(InputStream in) throws IOException {
            ByteArrayOutputStream line = new ByteArrayOutputStream();
            int b;
            boolean seenCR = false;
            while ((b = in.read()) != -1) {
                if (seenCR && b == '\n') {
                    break;
                }
                if (seenCR) {
                    line.write('\r');
                    seenCR = false;
                }
                if (b == '\r') {
                    seenCR = true;
                } else {
                    line.write(b);
                }
            }
            if (b == -1 && line.size() == 0 && !seenCR) {
                return null;
            }
            return line.toString(StandardCharsets.ISO_8859_1.name());
        }

        public CompletableFuture<SimpleHttpResponse> asyncSend() {
            return CompletableFuture.supplyAsync(this::send);
        }

        protected HttpURLConnection getHttpConnection(URL url, String method) throws Exception {
            Proxy proxy = options.getProxy();
            HttpURLConnection conn = proxy == null
                    ? (HttpURLConnection) url.openConnection()
                    : (HttpURLConnection) url.openConnection(proxy);
            conn.setRequestMethod(method);
            conn.setDoInput(true);
            if (SEND_DATA_METHOD.contains(method)) {
                conn.setDoOutput(true);
            }
            conn.setRequestProperty("Accept", "*/*");
            return conn;
        }

        protected String appendParams(String url) throws UnsupportedEncodingException {
            Map<String, Object> params = options.getQueries();
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


        // ===================================================
        // 处理返回
        // ===================================================
        protected SimpleHttpResponse fillResponse(HttpURLConnection conn) throws IOException {
            SimpleHttpResponse httpResponse = new SimpleHttpResponse();

            httpResponse.setStatusCode(conn.getResponseCode());
            httpResponse.setHeaders(conn.getHeaderFields());

            if (!options.isLargeResp()) {
                byte[] bytes = readResponseToBytes(conn);
                httpResponse.setBodyBytes(bytes);
                String ct = conn.getHeaderField(HEADER_CONTENT_TYPE);
                String charset = getResponseCharset(ct);
                if (StringUtils.isEmpty(charset)) charset = options.getEncoding();
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
         * <p>
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
    }

    public static class JdkHttpClientSender extends HttpSender {
        JdkHttpClientSender(RequestOptions options) {
            super(options);
        }

        @Override
        public SimpleHttpResponse send() {
            try {
                HttpClient client = buildClient();
                HttpRequest request = buildRequest();
                HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                return fillResponse(response);
            } catch (Throwable e) {
                Log.error(e.getMessage(), e);
                throw new RuntimeException(e);
            }
        }

        @Override
        public CompletableFuture<SimpleHttpResponse> asyncSend() {
            try {
                HttpClient client = buildClient();
                HttpRequest request = buildRequest();
                return client.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                        .thenApply(this::fillResponse)
                        .exceptionally(e -> {
                            throw new RuntimeException(e);
                        });
            } catch (Throwable e) {
                Log.error(e.getMessage(), e);
                CompletableFuture<SimpleHttpResponse> future = new CompletableFuture<>();
                future.completeExceptionally(e);
                return future;
            }
        }

        private HttpClient buildClient() {
            HttpClient.Builder builder = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(Math.max(1, options.getConnectTimeout())))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .version(HttpClient.Version.HTTP_1_1);

            Proxy proxy = options.getProxy();
            if (proxy != null && proxy.address() instanceof InetSocketAddress) {
                builder.proxy(ProxySelector.of((InetSocketAddress) proxy.address()));
            }
            return builder.build();
        }

        private HttpRequest buildRequest() throws Exception {
            String url = appendParams(options.getUrl());
            Header headers = options.getHeaders();
            String method = options.getMethod();
            if (method == null) {
                method = METHOD_GET;
            }
            SimpleEntity entity = options.getData();
            byte[] content = entity == null ? new byte[0] : entity.getContent();
            if (entity != null && !headers.has(HEADER_CONTENT_TYPE)) {
                headers.set(HEADER_CONTENT_TYPE, entity.getContentType());
            }
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(Math.max(1, options.getReadTimeout())));

            if (!headers.hasItems()) {
                for (Map.Entry<String, String> entry : headers.getAll()) {
                    if (!isRestrictedHeaderForJdkClient(entry.getKey())) {
                        requestBuilder.header(entry.getKey(), entry.getValue());
                    }
                }
            }

            HttpRequest.BodyPublisher body = content.length == 0
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofByteArray(content);
            requestBuilder.method(method, body);
            return requestBuilder.build();
        }

        private boolean isRestrictedHeaderForJdkClient(String headerName) {
            if (StringUtils.isEmpty(headerName)) {
                return false;
            }
            String name = headerName.trim().toLowerCase(Locale.ROOT);
            return "content-length".equals(name)
                    || "host".equals(name)
                    || "connection".equals(name)
                    || "expect".equals(name)
                    || "upgrade".equals(name);
        }

        private SimpleHttpResponse fillResponse(HttpResponse<byte[]> response) {
            SimpleHttpResponse httpResponse = new SimpleHttpResponse();
            httpResponse.setStatusCode(response.statusCode());
            httpResponse.setHeaders(response.headers().map());

            byte[] bytes = response.body() == null ? new byte[0] : response.body();
            String encoding = firstHeader(response.headers().map(), HEADER_CONTENT_ENCODING);
            if (!StringUtils.isEmpty(encoding) && encoding.toLowerCase().contains("gzip")) {
                try {
                    bytes = readStreamToBytes(new GZIPInputStream(new ByteArrayInputStream(bytes)));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            if (!options.isLargeResp()) {
                httpResponse.setBodyBytes(bytes);
                String contentType = firstHeader(response.headers().map(), HEADER_CONTENT_TYPE);
                String charset = getResponseCharset(contentType);
                if (StringUtils.isEmpty(charset)) charset = options.getEncoding();
                if (StringUtils.isEmpty(charset)) charset = DEFAULT_CHARSET;
                httpResponse.setCharset(charset);
            } else {
                try {
                    File f = File.createTempFile("simple-http-client-", ".tmp");
                    Files.write(f.toPath(), bytes);
                    httpResponse.setStoreContentWithFile(true);
                    httpResponse.setTmpFile(f);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            return httpResponse;
        }

        private String firstHeader(Map<String, List<String>> headers, String key) {
            if (headers == null || headers.isEmpty() || StringUtils.isEmpty(key)) {
                return null;
            }
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(key)) {
                    List<String> values = entry.getValue();
                    if (values != null && !values.isEmpty()) {
                        return values.get(0);
                    }
                    return null;
                }
            }
            return null;
        }
    }

    public static class HttpsSender extends HttpSender {
        HttpsSender(RequestOptions options) {
            super(options);
        }

        @Override
        protected HttpURLConnection getHttpConnection(URL url, String method)
                throws Exception {
            HttpsURLConnection conn = (HttpsURLConnection) super.getHttpConnection(url, method);
            initSSL(conn);
            return conn;
        }


        private void initSSL(HttpsURLConnection conn) throws Exception {
            SSLContext ctx = SSLContext.getInstance("TLS");
            KeyManager[] kms = new KeyManager[0];
            TrustManager[] tms = new TrustManager[]{new DefaultTrustManager()};
            SecureRandom random = new SecureRandom();
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

    public static class HttpSenderBuilder {
        RequestOptions opts = new RequestOptions();
        Cookie cookie;

        public HttpSenderBuilder method(String method) {
            opts.setMethod(method);
            return this;
        }

        public HttpSenderBuilder headers(Header headers) {
            if (headers == null) opts.headers.clear();
            else opts.setHeaders(headers);
            return this;
        }

        public HttpSenderBuilder headers(Map<String, String> headers) {return headers(Header.create(headers));}

        public HttpSenderBuilder param(String name, Object value) {
            if (name != null) opts.getQueries().put(name, value);
            return this;
        }

        public HttpSenderBuilder params(Map<String, Object> params) {
            if (params != null) opts.getQueries().putAll(params);
            return this;
        }

        public HttpSenderBuilder params(Object... args) {
            Map<String, Object> params = new HashMap<>();
            for (int i = 0; i < args.length; i = i + 2) {
                if (i + 1 < args.length) {
                    params.put(args[i].toString(), args[i + 1]);
                } else {
                    params.put(args[i].toString(), null);
                }
            }
            return params(params);
        }

        public HttpSenderBuilder data(SimpleEntity data) {
            opts.setData(data);
            return this;
        }

        public HttpSenderBuilder data(byte[] data) {return data(new ByteArrayEntity(data));}

        public HttpSenderBuilder data(String data) {return data(new StringEntity(data));}

        public HttpSenderBuilder data(File data) {return data("file", data);}

        public HttpSenderBuilder data(Path data) {return data("file", data);}

        public HttpSenderBuilder data(Map<String, Object> data) {return toEntity(data);}

        public HttpSenderBuilder data(Object... args) {
            Map<String, Object> map = new HashMap<>();
            for (int i = 0; i < args.length; i = i + 2) {
                if (i + 1 < args.length) {
                    map.put(args[i].toString(), args[i + 1]);
                } else {
                    map.put(args[i].toString(), null);
                }
            }
            return data(map);
        }

        public HttpSenderBuilder json(Object... args) {
            Map<String, Object> map = new HashMap<>();
            for (int i = 0; i < args.length; i = i + 2) {
                if (i + 1 < args.length) {
                    map.put(args[i].toString(), args[i + 1]);
                } else {
                    map.put(args[i].toString(), null);
                }
            }
            return json(map);
        }

        public HttpSenderBuilder json(Object data) {
            opts.setData(new JsonEntity(data));
            return this;
        }

        public HttpSenderBuilder connectTimeout(int connectTimeout) {
            opts.setConnectTimeout(connectTimeout);
            return this;
        }

        public HttpSenderBuilder readTimeout(int readTimeout) {
            opts.setReadTimeout(readTimeout);
            return this;
        }

        public HttpSenderBuilder encoding(String encoding) {
            opts.setEncoding(encoding);
            return this;
        }

        public HttpSenderBuilder url(String url) {
            opts.setUrl(url);
            return this;
        }

        public HttpSenderBuilder proxy(Proxy proxy) {
            opts.setProxy(proxy);
            return this;
        }

        public HttpSenderBuilder noProxy() {
            opts.setProxy(Proxy.NO_PROXY);
            return this;
        }

        public HttpSenderBuilder httpProxy(String host, int port) {
            opts.setProxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port)));
            return this;
        }

        public HttpSenderBuilder socksProxy(String host, int port) {
            opts.setProxy(new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(host, port)));
            return this;
        }

        public HttpSenderBuilder largeResp(boolean largeResp) {
            opts.setLargeResp(largeResp);
            return this;
        }

        public HttpSenderBuilder cookie(Cookie cookie) {
            this.cookie = cookie;
            return this;
        }

        public HttpSender build() {
            if (cookie != null) opts.cookie(cookie);
            return create(opts);
        }

        public static HttpSender create(String url) {
            return builder().url(url).build();
        }

        public static HttpSender create(RequestOptions options) {
            return createSender(options);
        }

        private HttpSenderBuilder toEntity(Map<String, Object> data) {
            data = data == null ? Collections.emptyMap() : data;
            boolean hasFile = data.values().stream().anyMatch(v -> v instanceof File || v instanceof Path);
            if (hasFile) {
                return data(new MultipartEntity(data));
            } else {
                return data(new FormEntity(data));
            }
        }
    }
}
