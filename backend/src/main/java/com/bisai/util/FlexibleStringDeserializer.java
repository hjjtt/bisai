package com.bisai.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 兼容字符串或字符串数组的反序列化器：
 * 前端契约是逗号分隔字符串（与后端实体一致），但部分客户端/测试工具会传数组，
 * 数组统一 join(",") 归一化，避免反序列化失败。
 */
public class FlexibleStringDeserializer extends StdDeserializer<String> {

    public FlexibleStringDeserializer() {
        super(String.class);
    }

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.currentToken() == JsonToken.VALUE_NULL) {
            return null;
        }
        if (p.currentToken() == JsonToken.START_ARRAY) {
            List<String> items = new ArrayList<>();
            while (p.nextToken() != JsonToken.END_ARRAY) {
                if (p.currentToken() == JsonToken.VALUE_STRING) {
                    String v = p.getText();
                    if (v != null && !v.isBlank()) {
                        items.add(v.trim());
                    }
                }
            }
            return items.isEmpty() ? null : String.join(",", items);
        }
        return p.getValueAsString();
    }
}
