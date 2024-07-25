package me.asu.http.util;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class ObjectToMap {

    public static Map objectToMap(Object obj) {
        if (obj == null) {
            return new HashMap();
        }
        try {
            Class type = obj.getClass();
            Map returnMap = new HashMap();
            BeanInfo beanInfo = Introspector.getBeanInfo(type);

            PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
            for (int i = 0; i < propertyDescriptors.length; i++) {
                PropertyDescriptor descriptor = propertyDescriptors[i];
                String propertyName = descriptor.getName();
                if (!propertyName.equals("class")) {
                    Method readMethod = descriptor.getReadMethod();
                    Object result = readMethod.invoke(obj);
                    if (result == null) {
                        continue;
                    }
                    //判断是否为 基础类型 String,Boolean,Byte,Short,Integer,Long,Float,Double
                    //判断是否集合类，COLLECTION,MAP              
                    if (result instanceof String
                            || result instanceof Boolean
                            || result instanceof Byte
                            || result instanceof Short
                            || result instanceof Integer
                            || result instanceof Long
                            || result instanceof Float
                            || result instanceof Double
                            || result instanceof Enum
                    ) {
                        if (result != null) {
                            returnMap.put(propertyName, result);
                        }
                    } else if (result instanceof Collection) {
                        Collection<?> lstObj = arrayToMap((Collection<?>) result);
                        returnMap.put(propertyName, lstObj);

                    } else if (result instanceof Map) {
                        Map<Object, Object> lstObj = mapToMap((Map<Object, Object>) result);
                        returnMap.put(propertyName, lstObj);
                    } else {
                        Map mapResult = objectToMap(result);
                        returnMap.put(propertyName, mapResult);
                    }

                }
            }
            return returnMap;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private static Map<Object, Object> mapToMap(Map<Object, Object> orignMap) {
        Map<Object, Object> resultMap = new HashMap<Object, Object>();
        for (Entry<Object, Object> entry : orignMap.entrySet()) {
            Object key = entry.getKey();
            Object resultKey = null;
            if (key instanceof Collection) {
                resultKey = arrayToMap((Collection) key);
            } else if (key instanceof Map) {
                resultKey = mapToMap((Map) key);
            } else {
                if (key instanceof String
                        || key instanceof Boolean
                        || key instanceof Byte
                        || key instanceof Short
                        || key instanceof Integer
                        || key instanceof Long
                        || key instanceof Float
                        || key instanceof Double
                        || key instanceof Enum
                ) {
                    if (key != null) {
                        resultKey = key;
                    }
                } else {
                    resultKey = objectToMap(key);
                }
            }

            Object value = entry.getValue();
            Object resultValue = null;
            if (value instanceof Collection) {
                resultValue = arrayToMap((Collection) value);
            } else if (value instanceof Map) {
                resultValue = mapToMap((Map) value);
            } else {
                if (value instanceof String
                        || value instanceof Boolean
                        || value instanceof Byte
                        || value instanceof Short
                        || value instanceof Integer
                        || value instanceof Long
                        || value instanceof Float
                        || value instanceof Double
                        || value instanceof Enum
                ) {
                    if (value != null) {
                        resultValue = value;
                    }
                } else {
                    resultValue = objectToMap(value);
                }
            }

            resultMap.put(resultKey, resultValue);
        }
        return resultMap;
    }


    private static Collection arrayToMap(Collection lstObj) {
        ArrayList arrayList = new ArrayList();

        for (Object t : lstObj) {
            if (t instanceof Collection) {
                Collection result = arrayToMap((Collection) t);
                arrayList.add(result);
            } else if (t instanceof Map) {
                Map result = mapToMap((Map) t);
                arrayList.add(result);
            } else {
                if (t instanceof String
                        || t instanceof Boolean
                        || t instanceof Byte
                        || t instanceof Short
                        || t instanceof Integer
                        || t instanceof Long
                        || t instanceof Float
                        || t instanceof Double
                        || t instanceof Enum
                ) {
                    if (t != null) {
                        arrayList.add(t);
                    }
                } else {
                    Object result = objectToMap(t);
                    arrayList.add(result);
                }
            }
        }
        return arrayList;
    }

}