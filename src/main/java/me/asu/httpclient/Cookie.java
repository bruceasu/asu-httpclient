package me.asu.httpclient;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public class Cookie {

    protected Map<String, String> map;

    public Cookie(String s) {
        this();
        parse(s);
    }

    public Cookie() {
        map = new HashMap<String, String>();
    }

    public void parse(String str) {
        String[] ss = str.split(";");
        for (String s : ss) {
            String[] p = s.split("=]");
            if (p.length == 1) {
                map.put(p[0], "");
            } else {
                map.put(p[0], p[1]);
            }
        }
    }

    public String get(String name) {
        return map.get(name);
    }

    public Cookie remove(String name) {
        map.remove(name);
        return this;
    }

    public Cookie set(String name, String value) {
        map.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Iterator<Map.Entry<String, String>> it = map.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, String> entry = it.next();
            sb.append(entry.getKey()).append('=').append(entry.getValue());
            if (it.hasNext()) {
                sb.append("; ");
            }
        }
        return sb.toString();
    }

}
