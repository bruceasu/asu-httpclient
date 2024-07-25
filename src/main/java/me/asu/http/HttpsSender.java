package me.asu.http;

import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

@Slf4j
public class HttpsSender extends HttpSender {

    @Override
    protected HttpURLConnection getHttpConnection(URL url, String method)
            throws Exception {
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setDoInput(true);
        if (SEND_DATA_METHOD.contains(method)) {
            conn.setDoOutput(true);
        }
        conn.setRequestProperty("Accept", "*/*");

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
