package com.ruoyi.studyroom.util;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.util.Date;

/**
 * 宽松日期反序列化: 兼容小程序不同入口传来的日期写法
 * <p>
 * 支持: "yyyy-MM-dd HH:mm:ss" / "yyyy-MM-dd" / "yyyy/MM/dd" / 毫秒或秒级时间戳,
 * 以及被错误拼成 "1789603200000 00:00:00" 的时间戳(取数字部分).
 * <p>
 * 背景: 2026-09-16 "预约今天"入口把时间戳当日期下发, 下单接口直接 500 无法预约,
 * 这里做兜底解析, 避免因前端格式差异导致用户"卡券用不了".
 *
 * @author xuan0ye
 */
public class FlexibleDateDeserializer extends JsonDeserializer<Date> {

    @Override
    public Date deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.currentToken() == JsonToken.VALUE_NUMBER_INT) {
            return fromEpoch(p.getLongValue());
        }
        String raw = p.getValueAsString();
        if (StrUtil.isBlank(raw)) {
            return null;
        }
        String s = raw.trim();
        String head = s.split("\\s+")[0];
        // 纯数字: 时间戳(兼容 "1789603200000 00:00:00" 这类被拼上时间后缀的写法)
        if (head.matches("\\d{10,14}")) {
            return fromEpoch(Long.parseLong(head));
        }
        String t = s.replace('/', '-').replace('T', ' ');
        try {
            return t.length() <= 10 ? DateUtil.parse(t + " 00:00:00") : DateUtil.parse(t);
        } catch (Exception e) {
            throw new IOException("日期格式无法解析: " + raw);
        }
    }

    private Date fromEpoch(long v) {
        // 10 位按秒, 13 位按毫秒
        return new Date(v < 100000000000L ? v * 1000L : v);
    }
}
