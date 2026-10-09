package com.ruoyi.studyroom.domain.bo;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

/**
 * 把"字符串 / 数组 / 对象"统一反序列化成一个字符串(取第一个非空值)。
 *
 * <p><b>为什么需要它</b>: 后台的图片上传组件 {@code components/ImageUpload} 的 v-model
 * <b>永远 emit 数组</b>({@code this.$emit("input", this.fileList)}), 而库里的字段是
 * {@code varchar}(Java 侧 String)。默认反序列化会直接抛
 * {@code Cannot deserialize value of type java.lang.String from Array value (token START_ARRAY)},
 * 表现就是后台保存"热门活动"时报 JSON parse error —— 有图报、没图(空数组)也报。</p>
 *
 * <p>兼容的形状: {@code "url"} / {@code ["url", ...]}(取第一个非空) /
 * {@code [{"name":"a.png","url":"https://..."}]}(取 url|path|file|name)。
 * 空数组与空串都归一成 {@code null}, 不会往库里写空串。</p>
 *
 * <p>该组件同时被其它页面使用(那些页面的字段本身就是数组), 所以只能在这里做兼容,
 * 不能去改组件。</p>
 */
public class FirstStringDeserializer extends JsonDeserializer<String> {

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        return first(p.getCodec().readTree(p));
    }

    /** 递归取第一个非空字符串 */
    private static String first(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isTextual() || node.isNumber() || node.isBoolean()) {
            String s = node.asText();
            return (s == null || s.isEmpty()) ? null : s;
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                String s = first(child);
                if (s != null) {
                    return s;
                }
            }
            return null;
        }
        if (node.isObject()) {
            for (String key : new String[] {"url", "path", "file", "name"}) {
                String s = first(node.get(key));
                if (s != null) {
                    return s;
                }
            }
            return null;
        }
        return null;
    }
}