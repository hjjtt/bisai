package com.bisai.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class JsonUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        MAPPER.registerModule(new JavaTimeModule());
    }

    public static <T> T convert(Object obj, Class<T> clazz) {
        return MAPPER.convertValue(obj, clazz);
    }

    public static <T> java.util.List<T> convertList(Object obj, Class<T> clazz) {
        return MAPPER.convertValue(obj,
                MAPPER.getTypeFactory().constructCollectionType(java.util.ArrayList.class, clazz));
    }
}
