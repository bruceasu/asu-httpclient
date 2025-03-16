package me.asu.httpclient.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public class StringUtils {


    private static final Pattern XML_PATTERN = Pattern.compile("\\<(.+?)\\>.*?\\</\\1\\>");

    public static String joinPath(String pre, String post) {
        if (pre.endsWith("/") && post.startsWith("/")) {
            return pre + post.substring(1);
        } else if (pre.endsWith("/") && !post.startsWith("/")) {
            return pre + post;
        } else if (!pre.endsWith("/") && post.startsWith("/")) {
            return pre + post;
        } else {
            return pre + "/" + post;
        }

    }

    /**
     * 快速判断是否是空串
     *
     * @param str 文本
     * @return true or false
     */
    public static boolean isEmpty(Object str) {
        return (str == null || "".equals(str.toString().trim()));
    }

    /**
     * 快速判断是否是formData
     *
     * @param content 文本
     * @return true or false
     */
    public static boolean isFormData(String content) {
        if (Objects.isNull(content)) {
            return false;
        }
        char[] chars = content.toCharArray();
        return (count(chars, '&') > 0 && count(chars, '\n') == 0) || (count(chars, '=') == 1
                && count(chars, '\n') == 0);
    }

    public static int count(char[] chars, char ch) {
        if (chars == null || chars.length == 0) {
            return 0;
        }
        int cnt = 0;
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == ch) {
                cnt++;
            }
        }

        return cnt;
    }

    /**
     * 快速判断是否是xml
     *
     * @param content 文本
     * @return true or false
     */
    public static boolean isXml(String content) {
        if (Objects.isNull(content)) {
            return false;
        }
        // it seems a xml string
        return XML_PATTERN.matcher(content).find();
    }

    /**
     * 快速判断是否是json
     *
     * @param content 文本
     * @return true or false
     */
    public static boolean isJson(String content) {
        if (Objects.isNull(content)) {
            return false;
        }

        byte[] bytes = content.getBytes();
        int i = 0;
        while (i < bytes.length) {
            if (bytes[i] == ' ' || bytes[i] == '\t' || bytes[i] == '\n' || bytes[i] == '\r') {
                i++;
                continue;
            }
            break;
        }
        if (i == bytes.length) {
            return false;
        }
        int jsonStart = 0;
        if (bytes[i] == '{') {
            jsonStart = 1;
        } else if (bytes[i] == '[') {
            jsonStart = 2;
        } else {
            return false;
        }
        int j = bytes.length - 1;
        while (j >= 0) {
            if (bytes[j] == ' ' || bytes[j] == '\t' || bytes[j] == '\n' || bytes[j] == '\r') {
                j--;
                continue;
            }
            break;
        }
        if (j == -1) {
            return false;
        } else if (j <= i) {
            return false;
        }

        return (bytes[j] != '}' || jsonStart == 1) && (bytes[j] != ']' || jsonStart == 2);
        // ok, it seems a json.
    }

    public static String removeCRLF(String str) {
        if (isEmpty(str)) {
            return "";
        }
        return str.replace('\r', ' ').replace('\n', ' ');

    }

    private static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    public static String safeString(Object src) {
        if (Objects.isNull(src)) {
            return "";
        } else if (src instanceof String) {
            return (String) src;
        } else {
            return src.toString();
        }
    }

    public static String encodeFormData(Map<String, Object> params)
            throws UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder();
        if (params != null && !params.isEmpty()) {
            for (String s : params.keySet()) {
                //@formatter:off
                Object o = params.get(s);
                if (o == null) {
                    sb.append(s).append('=').append('&');
                } else {
                    String value = encode(o.toString());
                    sb.append(s).append('=').append(value).append('&');
                }
                //@formatter:on
            }
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    public static String encode(String str) {
        if (StringUtils.isEmpty(str)) {
            return "";
        }

        try {
            return URLEncoder.encode(str, "utf-8");
        } catch (UnsupportedEncodingException e) {
            return str;
        }
    }
}
