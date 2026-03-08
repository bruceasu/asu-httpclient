package me.asu.httpclient;

import me.asu.httpclient.entity.MultipartEntity;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SimpleHttpClientFixesTest {

    @Test
    void senderShouldUseJdkHttpClientOnJdk11Plus() {
        RequestOptions options = new RequestOptions();
        options.setUrl("http://localhost");
        SimpleHttpClient.HttpSender sender = SimpleHttpClient.createSender(options);
        assertTrue(sender instanceof SimpleHttpClient.JdkHttpClientSender);
    }

    @Test
    void cookieParseShouldSplitByEquals() {
        Cookie cookie = new Cookie("a=1; b=2; c");
        assertEquals("1", cookie.get("a"));
        assertEquals("2", cookie.get("b"));
        assertEquals("", cookie.get("c"));
    }

    @Test
    void multipartFileFieldShouldNotBeWrittenTwice() throws IOException {
        File tempFile = File.createTempFile("httpclient-", ".txt");
        Files.writeString(tempFile.toPath(), "hello", StandardCharsets.UTF_8);
        tempFile.deleteOnExit();

        Map<String, Object> formData = new LinkedHashMap<>();
        formData.put("file", tempFile);

        MultipartEntity entity = new MultipartEntity(formData);
        String body = new String(entity.getContent(), StandardCharsets.UTF_8);

        assertTrue(body.contains("name=\"file\";filename=\"" + tempFile.getName() + "\""));
        assertFalse(body.contains("Content-Disposition: form-data; name=\"file\"\r\n\r\n"));
    }

    @Test
    void responseJsonFromTempFileShouldBeParsed() throws IOException {
        File tempFile = File.createTempFile("httpclient-", ".json");
        Files.writeString(tempFile.toPath(), "{\"name\":\"ok\"}", StandardCharsets.UTF_8);
        tempFile.deleteOnExit();

        SimpleHttpResponse response = new SimpleHttpResponse();
        response.setStoreContentWithFile(true);
        response.setTmpFile(tempFile);

        @SuppressWarnings("unchecked")
        Map<String, Object> map = response.getAsJson(Map.class);
        assertEquals("ok", map.get("name"));
    }

    @Test
    void proxyShouldBeAppliedWhenOpeningConnection() throws Exception {
        RequestOptions options = new RequestOptions();
        options.setProxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 8080)));

        TrackingHandler handler = new TrackingHandler();
        URL url = new URL(null, "mock://example", handler);

        TestHttpSender sender = new TestHttpSender(options);
        HttpURLConnection connection = sender.open(url, "GET");

        assertNotNull(connection);
        assertTrue(handler.openedWithProxy);
    }

    @Test
    void patchAndGetWithBodyShouldWorkUsingJdkHttpClientSender() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/echo", SimpleHttpClientFixesTest::echoMethodAndBody);
        server.start();
        try {
            int port = server.getAddress().getPort();
            String url = "http://127.0.0.1:" + port + "/echo";

            SimpleHttpResponse patchResp = SimpleHttpClient.builder()
                    .url(url)
                    .method("PATCH")
                    .data("hello")
                    .build()
                    .send();
            assertEquals(200, patchResp.getStatusCode());
            assertEquals("PATCH|hello", patchResp.getContent(StandardCharsets.UTF_8.name()));

            RequestOptions getOpts = new RequestOptions();
            getOpts.setUrl(url);
            getOpts.setMethod("GET");
            getOpts.setData(new me.asu.httpclient.entity.StringEntity("body-for-get"));
            SimpleHttpResponse getResp = SimpleHttpClient.send(getOpts);
            assertEquals(200, getResp.getStatusCode());
            assertEquals("GET|body-for-get", getResp.getContent(StandardCharsets.UTF_8.name()));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void asyncSendShouldReturnSameMethodAndBody() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/echo", SimpleHttpClientFixesTest::echoMethodAndBody);
        server.start();
        try {
            int port = server.getAddress().getPort();
            String url = "http://127.0.0.1:" + port + "/echo";

            RequestOptions options = new RequestOptions();
            options.setUrl(url);
            options.setMethod("POST");
            options.setData(new me.asu.httpclient.entity.StringEntity("async-body"));

            SimpleHttpResponse response = SimpleHttpClient.asyncSend(options).get();
            assertEquals(200, response.getStatusCode());
            assertEquals("POST|async-body", response.getContent(StandardCharsets.UTF_8.name()));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void fetchWithConsumerShouldSupportFunctionalConfig() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/echo", exchange -> {
            String rawQuery = exchange.getRequestURI().getRawQuery();
            byte[] body = exchange.getRequestBody().readAllBytes();
            String result = exchange.getRequestMethod() + "|" + rawQuery + "|" + new String(body, StandardCharsets.UTF_8);
            byte[] payload = result.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, payload.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(payload);
            }
        });
        server.start();
        try {
            int port = server.getAddress().getPort();
            String url = "http://127.0.0.1:" + port + "/echo";
            SimpleHttpResponse response = SimpleHttpClient.fetch(url, opts -> {
                opts.setMethod("POST");
                opts.getQueries().put("k", "v");
                opts.setData(new me.asu.httpclient.entity.StringEntity("hello"));
            });
            assertEquals(200, response.getStatusCode());
            assertEquals("POST|k=v|hello", response.getContent(StandardCharsets.UTF_8.name()));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void fetchWithMapInitShouldSupportAsyncAndJsonBody() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/echo", SimpleHttpClientFixesTest::echoMethodAndBody);
        server.start();
        try {
            int port = server.getAddress().getPort();
            String url = "http://127.0.0.1:" + port + "/echo";
            Map<String, Object> init = new HashMap<>();
            init.put("method", "PATCH");
            init.put("json", Map.of("name", "codex"));

            SimpleHttpResponse response = SimpleHttpClient.fetchAsync(url, init).get();
            assertEquals(200, response.getStatusCode());
            assertTrue(response.getContent(StandardCharsets.UTF_8.name()).startsWith("PATCH|"));
            assertTrue(response.getContent(StandardCharsets.UTF_8.name()).contains("\"name\":\"codex\""));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void patchShouldWorkUsingSocketFallbackSender() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/echo", SimpleHttpClientFixesTest::echoMethodAndBody);
        server.start();
        try {
            int port = server.getAddress().getPort();
            String url = "http://127.0.0.1:" + port + "/echo";

            RequestOptions options = new RequestOptions();
            options.setUrl(url);
            options.setMethod("PATCH");
            options.setData(new me.asu.httpclient.entity.StringEntity("socket-body"));
            options.getHeaders().set("Connection", "close");

            SocketFallbackSender sender = new SocketFallbackSender(options);
            SimpleHttpResponse response = sender.send();

            assertEquals(200, response.getStatusCode());
            assertEquals("PATCH|socket-body", response.getContent(StandardCharsets.UTF_8.name()));
        } finally {
            server.stop(0);
        }
    }

    private static void echoMethodAndBody(HttpExchange exchange) throws IOException {
        byte[] body = exchange.getRequestBody().readAllBytes();
        String result = exchange.getRequestMethod() + "|" + new String(body, StandardCharsets.UTF_8);
        byte[] payload = result.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/plain;charset=UTF-8");
        exchange.sendResponseHeaders(200, payload.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(payload);
        }
    }

    private static class TestHttpSender extends SimpleHttpClient.HttpSender {
        TestHttpSender(RequestOptions options) {
            super(options);
        }

        HttpURLConnection open(URL url, String method) throws Exception {
            return getHttpConnection(url, method);
        }
    }

    private static class SocketFallbackSender extends SimpleHttpClient.HttpSender {
        SocketFallbackSender(RequestOptions options) {
            super(options);
        }

        @Override
        protected boolean shouldUseSocketPatchFallback(String url, String method) {
            return true;
        }
    }

    private static class TrackingHandler extends URLStreamHandler {
        boolean openedWithProxy = false;

        @Override
        protected URLConnection openConnection(URL u) {
            return new FakeHttpURLConnection(u);
        }

        @Override
        protected URLConnection openConnection(URL u, Proxy p) {
            openedWithProxy = true;
            return new FakeHttpURLConnection(u);
        }
    }

    private static class FakeHttpURLConnection extends HttpURLConnection {
        FakeHttpURLConnection(URL url) {
            super(url);
        }

        @Override
        public void disconnect() {}

        @Override
        public boolean usingProxy() {
            return false;
        }

        @Override
        public void connect() {}
    }
}
