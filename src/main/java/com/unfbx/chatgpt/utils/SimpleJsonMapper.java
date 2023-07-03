package com.unfbx.chatgpt.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.util.Collection;

/**
 * Created by yanglinlin on 2017/11/22.
 */
public class SimpleJsonMapper {
    private static final JsonMapper mapper = JsonMapper.builder().build();

    static {
        mapper.setDefaultPrettyPrinter(null);
    }


    public static String writeValue(Object obj) {
        if (null == obj) {
            return null;
        }

        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    public static <T> T readValue(String jsonStr, Class<T> clazz) {
        try {
            return mapper.readValue(jsonStr, clazz);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    public static <T> T readCollectionValue(String jsonStr, Class<? extends Collection> collectionClass, Class clazz) {
        try {
            return mapper.readValue(jsonStr, mapper.<JavaType>convertValue(collectionClass, clazz));
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
