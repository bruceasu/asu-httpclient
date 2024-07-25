package me.asu.http;

import me.asu.http.entity.SimpleEntity;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public interface SimpleHttpClient {

    static SimpleHttpClient create(String url) {
        me.asu.http.RequestOptions opts = new me.asu.http.RequestOptions();
        opts.setUrl(url);
        return create(opts);
    }

    static SimpleHttpClient create(me.asu.http.RequestOptions options) {
        Objects.requireNonNull(options);
        Objects.requireNonNull(options.getUrl());
        me.asu.http.HttpSender client = null;
        if (isHttps(options.getUrl())) {
            client = new HttpsSender();
        } else {
            client = new HttpSender();
        }
        client.setOptions(options);
        return client;
    }

    /**
     * 是否为https地址.
     *
     * @param url 地址
     * @return true or false.
     */
    static boolean isHttps(String url) {
        return url.startsWith("https") || url.startsWith("HTTPS");
    }

    me.asu.http.RequestOptions getOptions();

    void setOptions(RequestOptions opts);

    /**
     * @return 内容
     * @throws IOException 异常
     */
    SimpleHttpResponse send();

    default CompletableFuture<SimpleHttpResponse> asyncSend() {
        return CompletableFuture.supplyAsync(this::send);
    }

    default CompletableFuture<SimpleHttpResponse> asyncSendFile(Map<String, Object> formData) {
        return CompletableFuture.supplyAsync((() -> {
            try {
                return sendFile(formData);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }));
    }

    default SimpleHttpClient post() {
        getOptions().setMethod(me.asu.http.Constants.METHOD_POST);
        return this;
    }

    SimpleHttpResponse sendFile(Map<String, Object> formData) throws IOException;

    default SimpleHttpClient put() {
        getOptions().setMethod(me.asu.http.Constants.METHOD_PUT);
        return this;
    }

    default SimpleHttpClient patch() {
        getOptions().setMethod(me.asu.http.Constants.METHOD_PATCH);
        return this;
    }

    default SimpleHttpClient delete() {
        getOptions().setMethod(me.asu.http.Constants.METHOD_DELETE);
        return this;
    }

    default SimpleHttpClient head() {
        getOptions().setMethod(me.asu.http.Constants.METHOD_HEAD);
        return this;
    }

    default SimpleHttpClient get() {
        getOptions().setMethod(Constants.METHOD_GET);
        return this;
    }

    default SimpleHttpClient largeResp() {
        getOptions().setLargeResp(true);
        return this;
    }

    default SimpleHttpClient smallResp() {
        getOptions().setLargeResp(false);
        return this;
    }

    default SimpleHttpClient headers(Map<String, String> headers) {
        if (headers == null) {
            return this;
        }
        me.asu.http.Header headersOpts = getOptions().getHeaders();
        headersOpts.setAll(headers);
        return this;
    }

    default SimpleHttpClient setHeader(String k, String v) {
        Header headersOpts = getOptions().getHeaders();
        headersOpts.set(k, v);
        return this;
    }

    default SimpleHttpClient params(Map<String, Object> params) {
        getOptions().setParams(params);
        return this;
    }

    default SimpleHttpClient connectionTimeout(int timeout) {
        getOptions().setConnectTimeout(timeout);
        return this;
    }

    default SimpleHttpClient readTimeout(int timeout) {
        getOptions().setReadTimeout(timeout);
        return this;
    }

    default SimpleHttpClient responseEncoding(String encoding) {
        getOptions().setEncoding(encoding);
        return this;
    }

    default SimpleHttpClient data(SimpleEntity entity) {
        getOptions().setData(entity);
        return this;
    }

    default SimpleHttpClient cookie(Cookie cookie) {
        getOptions().setCookie(cookie);
        return this;
    }

    default SimpleHttpClient withHttpProxy(String host, int port) {
        final Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
        return withProxy(proxy);
    }

    default SimpleHttpClient withProxy(final Proxy proxy) {
        getOptions().setProxy(proxy);
        return this;
    }

    default SimpleHttpClient withSockProxy(String host, int port) {
        final Proxy proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(host, port));
        return withProxy(proxy);
    }
}
