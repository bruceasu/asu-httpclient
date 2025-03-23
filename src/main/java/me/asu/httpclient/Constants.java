package me.asu.httpclient;

import java.util.regex.Pattern;

public class Constants {

    public static final String MIME_OCTET_STREAM = "application/octet-stream";
    public static final String MIME_MULTIPART = "multipart/form-data;";
    public static final String MIME_FORM_URLENCODED_UTF8 = "application/x-www-form-urlencoded;charset=UTF-8";
    public static final String MIME_JSON_UTF8 = "application/json;charset=UTF-8";
    public static final String MIME_XML_UTF8 = "application/xml;charset=UTF-8";
    public static final String MIME_TEXT_UTF8 = "text/plain;charset=UTF-8";


    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_CONTENT_LENGTH = "Content-Length";
    public static final String HEADER_CONTENT_ENCODING = "Content-Encoding";


    public static final String METHOD_POST = "POST";
    public static final String METHOD_GET = "GET";
    public static final String METHOD_DELETE = "DELETE";
    public static final String METHOD_PUT = "PUT";
    public static final String METHOD_PATCH = "PATCH";
    public static final String METHOD_HEAD = "HEAD";

    public static final String DATA_TYPE_FORM = "form";
    public static final String DATA_TYPE_JSON = "json";
    public static final String DATA_TYPE_MULTIPART = "multipart";
    public static final byte[] SEPARATOR = "\r\n".getBytes();

    public static final int DEFAULT_CONNECT_TIMEOUT_IN_MILLS = 5000;
    public static final int DEFAULT_READ_TIMEOUT_IN_MILLS = 15000;
    public static final String DEFAULT_CHARSET = "ISO-8859-1";
    public static final String UTF_8_CHARSET = "UTF-8";
    public static final String CHINA_CHARSET = "GB18030";
    public static final String HK_CHARSET = "BIG5-HKSCS";

    public static final Pattern ENCODE_IN_CONENT_TYPE = Pattern
            .compile(".+;\\s*charset\\s*=\\s*([\\w_\\-\\d]+)");

}
