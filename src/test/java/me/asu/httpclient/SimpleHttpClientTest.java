package me.asu.httpclient;


import me.asu.log.Log;

import java.io.IOException;

class SimpleHttpClientTest {

    public static void main(String[] args) {
        String content =  SimpleHttpClient.get("https://www.google.com");
        Log.info(content);
    }

}