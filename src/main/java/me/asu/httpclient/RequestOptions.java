package me.asu.httpclient;

import me.asu.httpclient.entity.SimpleEntity;

import java.net.Proxy;
import java.util.HashMap;
import java.util.Map;

import static me.asu.httpclient.Constants.*;

/**
 * @author Bruce
 */
public class RequestOptions {
    String method = METHOD_GET;
    Header headers = Header.create();
    Map<String, Object> queries = new HashMap<>();
    SimpleEntity data = null;
    int connectTimeout = DEFAULT_CONNECT_TIMEOUT_IN_MILLS;
    int readTimeout = DEFAULT_READ_TIMEOUT_IN_MILLS;
    String encoding = null;
    String url = null;
    Proxy proxy;
    boolean largeResp = false;

    public String getMethod() {return method;}
    public void setMethod(String method) {this.method = method;}
    public Header getHeaders() {return headers;}
    public void setHeaders(Header headers) {this.headers = headers;}
    public Map<String, Object> getQueries() {return queries;}
    public void setQueries(Map<String, Object> queries) {this.queries = queries;}
    public SimpleEntity getData() {return data;}
    public void setData(SimpleEntity data) {this.data = data;}
    public int getConnectTimeout() {return connectTimeout;}
    public void setConnectTimeout(int connectTimeout) {this.connectTimeout = connectTimeout;}
    public int getReadTimeout() {return readTimeout;}
    public void setReadTimeout(int readTimeout) {this.readTimeout = readTimeout;}
    public String getEncoding() {return encoding;}
    public void setEncoding(String encoding) {this.encoding = encoding;}
    public String getUrl() {return url;}
    public void setUrl(String url) {this.url = url;}
    public Proxy getProxy() {return proxy;}
    public void setProxy(Proxy proxy) {this.proxy = proxy;}
    public boolean isLargeResp() {return largeResp;}
    public void setLargeResp(boolean largeResp) {this.largeResp = largeResp;}

    public Cookie cookie() {
        String s = headers.get("Cookie");
        if (null == s) {
            return new Cookie();
        } else {
            return  new Cookie(s);
        }
    }

    public void cookie(Cookie cookie) {
        headers.set("Cookie", cookie.toString());
    }
}
