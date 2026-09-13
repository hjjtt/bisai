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
 * 上限防御：数组最多 10 个元素、结果最长 512 字符（对齐 DB varchar(512)），超出抛异常交由全局处理器返回 400。
 */
public class FlexibleStringDeserializer extends StdDeserializer<String> {

    private static final int MAX_ARRAY_ITEMS = 10;
    private static final int MAX_RESULT_LENGTH = 512;

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
                if (items.size() > MAX_ARRAY_ITEMS) {
                    throw new IOException("数组元素过多（上限 " + MAX_ARRAY_ITEMS + "）");
                }
            }
            if (items.isEmpty()) {
                return null;
            }
            String joined = String.join(",", items);
            if (joined.length() > MAX_RESULT_LENGTH) {
                throw new IOException("字段值过长（上限 " + MAX_RESULT_LENGTH + " 字符）");
            }
            return joined;
        }
        String value = p.getValueAsString();
        if (value != null && value.length() > MAX_RESULT_LENGTH) {
            throw new IOException("字段值过长（上限 " + MAX_RESULT_LENGTH + " 字符）");
        }
        return value;
    }
}
