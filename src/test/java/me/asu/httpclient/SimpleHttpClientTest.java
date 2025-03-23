package me.asu.httpclient;


import me.asu.log.Log;

import java.io.IOException;

class SimpleHttpClientTest {

    public static void main(String[] args) {
        SimpleHttpClient simpleHttpClient = SimpleHttpClient.create("https://www.google.com");
        SimpleHttpResponse response = simpleHttpClient.get().send();
        String content = response.getContent();
        Log.info(content);
    }

}