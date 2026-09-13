package com.ruoyi.studyroom.wx.douyin;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.redis.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

/**
 * 抖音生活服务 SPI 回调 - 「抖音请求三方系统退款审核」
 * (Action: fulfilment.order.refund_apply，控制台场景名: 抖音请求三方系统退款审核)
 *
 * 官方规范(2026-09 摘录):
 * 1) URL 有效性验证: 抖音 POST 带 CHALLENGE 的 JSON, 必须原样回显 CHALLENGE;
 * 2) 响应报文必须为 {"data":{"error_code":0,"description":"success","result":X,"reason":"..."}}
 *    result: 0=待确认(WAIT) 1=允许(ALLOW) 2=拒绝(REJECT); result=2 时 reason 必填;
 * 3) 同一请求不支持"部分拒绝部分通过": 任一券已使用则整单拒绝;
 * 4) 必须按券维度幂等: 同一券多次请求返回相同结果;
 * 5) 验签: Header x-life-sign = SHA-256(client_secret + 排序URL参数 + &http_body=原始字节),
 *    十六进制小写; body 必须用**原始字节**参与计算(不能反序列化再序列化);
 * 6) 响应需快(超 8s 视为未响应), 抖音报错会每小时重试。
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/goodlife/v1/fulfilment")
public class DouyinSpiController {

    /** 退款审核结果幂等缓存(按 after_sale_id / 券集合) */
    private static final String AUDIT_CACHE_PREFIX = "douyin:spi:refund:audit:";
    private static final long AUDIT_CACHE_HOURS = 48L;

    private final DouyinVerifyService douyinVerifyService;
    private final DouyinConfigService configService;

    @PostMapping({"/refund/audit", "/refund/audit/"})
    public Object refundAudit(HttpServletRequest request) throws Exception {
        // 读取原始 body 字节(验签必须用原始字节, 不能被框架反序列化过)
        byte[] rawBody = readRawBody(request);
        String bodyText = new String(rawBody, StandardCharsets.UTF_8);
        String logId = request.getHeader("X-Bytedance-Logid");

        // 1) URL 有效性验证: 原样回显 CHALLENGE
        if (bodyText.contains("CHALLENGE")) {
            try {
                JSONObject obj = JSONUtil.parseObj(bodyText);
                Object challenge = obj.get("CHALLENGE");
                if (challenge != null) {
                    log.info("[抖音SPI] CHALLENGE 校验请求, 回显: {}", challenge);
                    return String.valueOf(challenge);
                }
            } catch (Exception ignore) { }
        }

        // 2) 验签
        boolean signOk = verifySignature(request, rawBody);
        // spi_verify_enabled: 1=严格(验签失败直接拒绝) 0=告警模式(记录日志但放行, 便于上线观察)
        String verifyStrict = configService.get("spi_verify_enabled");
        if (!signOk && "1".equals(verifyStrict)) {
            log.warn("[抖音SPI] 验签失败, 拒绝请求 logId={}", logId);
            return failBody("签名校验失败");
        }
        if (!signOk) {
            log.warn("[抖音SPI] 验签未通过(当前为告警模式, 放行) logId={} x-life-clientkey={}",
                logId, request.getHeader("x-life-clientkey"));
        }

        // 3) 解析报文(官方字段: after_sale_id / order_id / certificates[] )
        JSONObject body;
        try {
            body = JSONUtil.parseObj(bodyText);
        } catch (Exception e) {
            log.error("[抖音SPI] 报文解析失败 logId={} body={}", logId, bodyText, e);
            return okBody(1, "报文解析异常，按允许退款兜底");
        }
        String afterSaleId = body.getStr("after_sale_id");
        String orderId = body.getStr("order_id");
        List<String> certificateIds = new ArrayList<>();
        JSONArray certs = body.getJSONArray("certificates");
        if (certs != null) {
            for (int i = 0; i < certs.size(); i++) {
                JSONObject c = certs.getJSONObject(i);
                if (c != null && StringUtils.isNotBlank(c.getStr("certificate_id"))) {
                    certificateIds.add(c.getStr("certificate_id"));
                }
            }
        }
        log.info("[抖音SPI] 退款审核请求 logId={} afterSaleId={} orderId={} 券数={}",
            logId, afterSaleId, orderId, certificateIds.size());

        // 4) 幂等: 同一售后单/同一批券返回相同结果
        String idemKey = buildIdempotentKey(afterSaleId, orderId, certificateIds);
        if (StringUtils.isNotBlank(idemKey)) {
            Object cached = RedisUtils.getCacheObject(AUDIT_CACHE_PREFIX + idemKey);
            if (cached instanceof Map) {
                log.info("[抖音SPI] 命中幂等缓存 logId={} key={}", logId, idemKey);
                @SuppressWarnings("unchecked")
                Map<String, Object> c = (Map<String, Object>) cached;
                return okBody((Integer) c.get("result"), (String) c.get("reason"));
            }
        }

        // 5) 审核判定(两阶段: 先判定后执行, 不支持部分通过)
        Map<String, Object> decision;
        try {
            decision = douyinVerifyService.auditRefundBatch(certificateIds);
        } catch (Exception e) {
            log.error("[抖音SPI] 审核判定异常 logId={}", logId, e);
            decision = new HashMap<>();
            decision.put("result", 1);
            decision.put("reason", "系统异常，按允许退款兜底");
        }
        int result = decision.get("result") == null ? 1 : ((Number) decision.get("result")).intValue();
        String reason = decision.get("reason") == null ? "" : String.valueOf(decision.get("reason"));
        if (result == 2 && StringUtils.isBlank(reason)) {
            reason = "该团购券已使用，不支持退款，如有疑问请联系门店";
        }
        if (StringUtils.isNotBlank(idemKey)) {
            Map<String, Object> cacheVal = new HashMap<>();
            cacheVal.put("result", result);
            cacheVal.put("reason", reason);
            try {
                RedisUtils.setCacheObject(AUDIT_CACHE_PREFIX + idemKey, cacheVal, AUDIT_CACHE_HOURS, TimeUnit.HOURS);
            } catch (Exception e) {
                log.warn("[抖音SPI] 幂等缓存写入失败", e);
            }
        }
        log.info("[抖音SPI] 审核结果 logId={} result={} reason={}", logId, result, reason);
        return okBody(result, reason);
    }

    /* ==================== 报文组装 ==================== */

    /** 官方成功报文 */
    private Map<String, Object> okBody(int result, String reason) {
        Map<String, Object> data = new HashMap<>();
        data.put("error_code", 0);
        data.put("description", "success");
        data.put("result", result);
        data.put("reason", reason == null ? "" : reason);
        Map<String, Object> resp = new HashMap<>();
        resp.put("data", data);
        return resp;
    }

    /** 校验失败(错误码 300001 系统错误) */
    private Map<String, Object> failBody(String message) {
        Map<String, Object> data = new HashMap<>();
        data.put("error_code", 300001);
        data.put("description", message);
        Map<String, Object> resp = new HashMap<>();
        resp.put("data", data);
        return resp;
    }

    /* ==================== 验签 ==================== */

    /**
     * 官方新签名算法:
     *   str1 = client_secret
     *        + 逐个 URL 参数(key 升序; 同 key 多值按值升序) -> "&key=value"
     *        + (POST) "&http_body=" + 原始body字节(UTF-8)
     *   sign = lowerHex(SHA-256(str1)), 与 Header x-life-sign 比对
     */
    private boolean verifySignature(HttpServletRequest request, byte[] rawBody) {
        try {
            String clientSecret = configService.get("client_secret");
            if (StringUtils.isBlank(clientSecret)) {
                log.warn("[抖音SPI] 未配置 client_secret, 跳过验签");
                return true;
            }
            String expect = request.getHeader("x-life-sign");
            if (StringUtils.isBlank(expect)) {
                // 无签名头(例如 CHALLENGE 校验请求): 视为不通过, 交由调用方按模式决定
                return false;
            }
            // URL 参数(忽略 sign) 按 key 升序, 同 key 多值按值升序
            TreeMap<String, List<String>> params = new TreeMap<>();
            String qs = request.getQueryString();
            if (StringUtils.isNotBlank(qs)) {
                for (String pair : qs.split("&")) {
                    if (StringUtils.isBlank(pair)) {
                        continue;
                    }
                    int idx = pair.indexOf('=');
                    String k = idx > 0 ? urlDecode(pair.substring(0, idx)) : urlDecode(pair);
                    String v = idx > 0 ? urlDecode(pair.substring(idx + 1)) : "";
                    if ("sign".equals(k)) {
                        continue;
                    }
                    params.computeIfAbsent(k, x -> new ArrayList<>()).add(v);
                }
            }
            StringBuilder sb = new StringBuilder(clientSecret);
            for (Map.Entry<String, List<String>> e : params.entrySet()) {
                List<String> values = e.getValue();
                values.sort(String::compareTo);
                for (String v : values) {
                    sb.append('&').append(e.getKey()).append('=').append(v);
                }
            }
            if (rawBody != null && rawBody.length > 0) {
                sb.append("&http_body=").append(new String(rawBody, StandardCharsets.UTF_8));
            }
            String calc = sha256Hex(sb.toString());
            boolean ok = calc.equalsIgnoreCase(expect);
            if (!ok) {
                log.warn("[抖音SPI] 验签不一致 expect={} calc={} str1={}", expect, calc, sb);
            }
            return ok;
        } catch (Exception e) {
            log.error("[抖音SPI] 验签异常", e);
            return false;
        }
    }

    private String sha256Hex(String text) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(text.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private String urlDecode(String s) {
        try {
            return java.net.URLDecoder.decode(s, "UTF-8");
        } catch (Exception e) {
            return s;
        }
    }

    private byte[] readRawBody(HttpServletRequest request) throws Exception {
        try (InputStream in = request.getInputStream();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        }
    }

    /** 幂等键: 优先售后单号+券集合 */
    private String buildIdempotentKey(String afterSaleId, String orderId, List<String> certificateIds) {
        String certs = certificateIds == null ? "" : String.join(",", certificateIds);
        if (StringUtils.isNotBlank(afterSaleId)) {
            return "afterSale:" + afterSaleId + ":" + certs;
        }
        if (StringUtils.isNotBlank(orderId) && StringUtils.isNotBlank(certs)) {
            return "order:" + orderId + ":" + certs;
        }
        return StringUtils.isNotBlank(certs) ? "certs:" + certs : null;
    }
}
