package com.unfbx.chatgpt.utils;

import com.patsnap.common.jackson.JsonMapper;

import java.util.Collection;

/**
 * Created by yanglinlin on 2017/11/22.
 */
public class SimpleJsonMapper {
    private static final JsonMapper mapper = JsonMapper.defaultMapper();

    static {
        mapper.getMapper().setDefaultPrettyPrinter(null);
    }


    public static String writeValue(Object obj) {
        if (null == obj) {
            return null;
        }

        return mapper.toJson(obj);
    }

    public static <T> T readValue(String jsonStr, Class<T> clazz) {
        return mapper.fromJson(jsonStr, clazz);
    }

    public static <T> T readCollectionValue(String jsonStr, Class<? extends Collection> collectionClass, Class clazz) {
        return mapper.fromJson(jsonStr, mapper.constructCollectionType(collectionClass, clazz));
    }
}
