package com.bisai.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class JsonUtil {

    private static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        JavaTimeModule timeModule = new JavaTimeModule();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATETIME_FORMAT);
        timeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(formatter));
        timeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(formatter));
        MAPPER.registerModule(timeModule);
    }

    public static <T> T convert(Object obj, Class<T> clazz) {
        return MAPPER.convertValue(obj, clazz);
    }

    public static <T> java.util.List<T> convertList(Object obj, Class<T> clazz) {
        return MAPPER.convertValue(obj,
                MAPPER.getTypeFactory().constructCollectionType(java.util.ArrayList.class, clazz));
    }
}
