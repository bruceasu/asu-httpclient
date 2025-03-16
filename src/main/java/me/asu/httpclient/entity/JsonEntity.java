package me.asu.httpclient.entity;

import lombok.extern.slf4j.Slf4j;
import me.asu.httpclient.Constants;
import me.asu.httpclient.util.Bytes;
import xyz.calvinwilliams.okjson.OKJSON;

@Slf4j
public class JsonEntity implements SimpleEntity {

    Object object;

    public JsonEntity(Object object) {
        this.object = object;
    }

    @Override
    public byte[] getContent() {
        return Bytes.toBytes(toContent());
    }

    public String toContent() {
        if (object == null) {
            return "";
        } else {
            String jsonString = OKJSON.objectToString( object, 0 ) ;
            if( jsonString == null ) {
                System.out.println( "okjson.stringToObject failed["+OKJSON.getErrorCode()+"]["+OKJSON.getErrorDesc()+"]" );
                return null;
            }
            return jsonString;
        }
    }

    @Override
    public String getContentType() {
        return Constants.MIME_JSON_UTF8;
    }
}
