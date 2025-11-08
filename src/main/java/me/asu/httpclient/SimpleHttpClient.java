package me.asu.httpclient;

import me.asu.httpclient.entity.*;
import me.asu.httpclient.util.StringUtils;
import me.asu.log.Log;

import javax.net.ssl.*;
import java.io.*;
import java.net.*;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.zip.GZIPInputStream;

import static me.asu.httpclient.Constants.*;

public class SimpleHttpClient {
    public static HttpSenderBuilder builder() {
        return new HttpSenderBuilder();
    }

    public static Set<String> SEND_DATA_METHOD = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(METHOD_POST, METHOD_PUT, METHOD_PATCH)));

    /**
     * Send the request.
     *
     * @return Response {@link SimpleHttpResponse}
     */

    public SimpleHttpResponse send(RequestOptions options) {
        HttpSender sender = isHttps(options.getUrl()) ? new HttpsSender(options): new HttpSender(options);
        return sender.send();
    }

    public CompletableFuture<SimpleHttpResponse> asyncSend(final RequestOptions opts) {
        return CompletableFuture.supplyAsync(()->send(opts));
    }

    public static String post(String url, Map<String, Object> data) {
        return post(String.class, url,  data);
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

    static <T> T toResult(Class<T> type, SimpleHttpResponse resp) {
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

        public String getMethod() {return options.getMethod();}

        public Map<String, Object> getParams() {return options.getParams();}

        public Header getHeaders() {return options.getHeaders();}

        public void setHeaders(Header headers) {options.setHeaders(headers);}

        public boolean canEqual(Object other) {return options.canEqual(other);}

        public void setConnectTimeout(int connectTimeout) {options.setConnectTimeout(connectTimeout);}

        public void setEncoding(String encoding) {options.setEncoding(encoding);}

        public void setReadTimeout(int readTimeout) {options.setReadTimeout(readTimeout);}

        public SimpleEntity getData() {return options.getData();}

        public void setProxy(Proxy proxy) {options.setProxy(proxy);}

        public void setParams(Map<String, Object> params) {options.setParams(params);}

        public int getConnectTimeout() {return options.getConnectTimeout();}

        public int getReadTimeout() {return options.getReadTimeout();}

        public String getEncoding() {return options.getEncoding();}

        public void setData(SimpleEntity data) {options.setData(data);}

        public String getUrl() {return options.getUrl();}

        public void setUrl(String url) {options.setUrl(url);}

        public Proxy getProxy() {return options.getProxy();}

        public boolean isLargeResp() {return options.isLargeResp();}

        public void setMethod(String method) {options.setMethod(method);}

        public void setLargeResp(boolean largeResp) {options.setLargeResp(largeResp);}

        public Cookie getCookie() {return options.getCookie();}

        public RequestOptions setCookie(Cookie cookie) {return options.setCookie(cookie);}

        // ===================================================
        // If you're sure the length of response is large or small.
        // Default is small.
        // ===================================================
        public HttpSender largeResp() {
            options.setLargeResp(true);
            return this;
        }

        public HttpSender smallResp() {
            options.setLargeResp(false);
            return this;
        }

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
            if (name != null) opts.getParams().put(name, value);
            return this;
        }

        public HttpSenderBuilder params(Map<String, Object> params) {
            if (params != null) opts.getParams().putAll(params);
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
            if (cookie != null) opts.setCookie(cookie);
            return create(opts);
        }

        public static HttpSender create(String url) {
            return builder().url(url).build();
        }

        public static HttpSender create(RequestOptions options) {
            if (options == null || options.getUrl() == null) {
                return new HttpSender(options);
            }
            return isHttps(options.getUrl()) ? new HttpsSender(options): new HttpSender(options);
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