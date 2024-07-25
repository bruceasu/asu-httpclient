package me.asu.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.type.CollectionType;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static me.asu.util.Strings.isEmpty;


/**
 * Created by Administrator on 2020/5/15.
 */
public class JsonUtils {

    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.configure(DeserializationFeature.USE_JAVA_ARRAY_FOR_JSON_ARRAY, true)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * 假设content是json数据, 如果不是json数据则抛IOException异常。
     */
    public static JsonNode toJson(String content) throws IOException {
        if (isEmpty(content)) {
            return NullNode.getInstance();
        }
        return mapper.readTree(content);
    }

    public static JsonNode toJson(byte[] data) {
        try {
            return mapper.readTree(data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T toJson(String content, TypeReference<T> type) throws IOException {
        return mapper.readValue(content, type);
    }

    public static <T> T toJson(String content, Class<T> clazz) throws IOException {
        return mapper.readValue(content, clazz);
    }


    public static <T> TypeReference<T> createTypeReference(Class<T> cls) {
        return new TypeReference<T>() {
            @Override
            public Type getType() {
                return cls;
            }
        };
    }

    public static String stringify(Object data) throws JsonProcessingException {
        return mapper.writeValueAsString(data);
    }

    public static byte[] stringifyAsBytes(Object data) {
        try {
            return mapper.writeValueAsBytes(data);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String stringifyPretty(Object data) throws JsonProcessingException {
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
    }


//    // 使用 TypeReference 反序列化含有泛型的对象
//    public static <T> Response<T> fromJson(String json, Class<T> clazz) throws IOException {
//        return objectMapper.readValue(json, objectMapper.getTypeFactory().constructParametricType(Response.class, clazz));
//    }

//    // 反序列化泛型集合
//    public static <T> List<T> listFromJson(String json, Class<T> clazz) throws IOException {
//        return objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, clazz));
//    }


    /**
     * to Map
     *
     * @param data json String
     * @return a Map
     */
    public static Map toMap(String data) {
        try {
            return mapper.readValue(data, Map.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Map toMap(byte[] data) {
        try {
            return mapper.readValue(data, Map.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> List<T> toList(String data, Class<T> cls) {
        CollectionType listType = mapper.getTypeFactory()
                .constructCollectionType(ArrayList.class, cls);
        try {
            return mapper.readValue(data, listType);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static List<Map> toMapList(String data) {
        return toList(data, Map.class);
    }

    public static Map toMap(Object object) {
        //用jackson将bean转换为map
        return mapper.convertValue(object, new TypeReference<Map>() {
        });
    }


    public static List<Map> convertToListMap(List<Object> list) {
        //用jackson将bean转换为List<Map>
        return mapper.convertValue(list, new TypeReference<List<Map>>() {
        });
    }

    public static <T> T toObject(Object data, Class<T> cls) {
        if (data == null) return null;
        if (data.getClass() == JsonNode.class) {
            return (T) data;
        } else {
            try {
                return mapper.convertValue(data, createTypeReference(cls));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
    public static <T> T toObject(Object data,TypeReference<T> cls) {
        if (data == null) return null;
        if (data.getClass() == JsonNode.class) {
            return (T) data;
        } else {
            try {
                return mapper.convertValue(data, cls);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
    public static <T> T stringToObject(String data, TypeReference<T> type) {
        try {
            return mapper.readValue(data, type);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public static String asText(JsonNode dtNode, String item) {
        return asText(dtNode, item, null);
    }

    public static String asText(JsonNode dtNode, String item, String defaultValue) {
        if (dtNode == null) {
            return defaultValue;
        }

        JsonNode node = dtNode.get(item);
        if (node == null) {
            return defaultValue;
        }
        return node.asText();
    }

    public static JsonNode at(JsonNode dtNode, String path) {
        if (dtNode == null || isEmpty(path)) {
            return null;
        }

        return dtNode.at(path);
    }

    public static String atAsText(JsonNode dtNode, String path) {
        if (dtNode == null || isEmpty(path)) {
            return null;
        }

        return dtNode.at(path).asText();
    }

    public static ObjectNode createObject() {
        return mapper.createObjectNode();

    }

    public static ArrayNode createArray() {
        return mapper.createArrayNode();
    }
}
