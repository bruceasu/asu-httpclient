package me.asu.http;

import lombok.Data;
import me.asu.http.entity.SimpleEntity;

import java.net.Proxy;
import java.util.Map;

import static me.asu.http.Constants.*;

/**
 * @author Administrator
 */
@Data
public class RequestOptions {

    String method = METHOD_GET;
    me.asu.http.Header headers = Header.create();
    Map<String, Object> params = null;
    SimpleEntity data = null;
    int connectTimeout = DEFAULT_CONNECT_TIMEOUT_IN_MILLS;
    int readTimeout = DEFAULT_READ_TIMEOUT_IN_MILLS;
    String encoding = null;
    String url = null;
    me.asu.http.Cookie cookie;
    Proxy proxy;
    boolean largeResp = false;

    public me.asu.http.Cookie getCookie() {
        String s = headers.get("Cookie");
        if (null == s) {
            return new me.asu.http.Cookie();
        }
        return new me.asu.http.Cookie(s);
    }

    public RequestOptions setCookie(Cookie cookie) {
        headers.set("Cookie", cookie.toString());
        return this;
    }
}
