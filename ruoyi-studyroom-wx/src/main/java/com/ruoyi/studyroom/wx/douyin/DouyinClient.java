package com.ruoyi.studyroom.wx.douyin;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.redis.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.util.concurrent.TimeUnit;

/**
 * 抖音开放平台客户端(到综团购 - 核销三件套)
 * 1) client_token 获取 + Redis 缓存(提前 5 分钟刷新)
 * 2) prepare / verify / cancel 调用
 * 3) 抖音分享短链 v.douyin.com -> object_id(encrypted_data)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DouyinClient {

    private static final String TOKEN_URL = "https://open.douyin.com/oauth/client_token/";
    private static final String API_BASE = "https://open.douyin.com/goodlife/v1/fulfilment/certificate/";
    private static final String TOKEN_CACHE_KEY = "douyin:client_token";

    private final DouyinConfigService configService;

    /* ==================== token ==================== */

    public String getAccessToken() {
        Object cached = RedisUtils.getCacheObject(TOKEN_CACHE_KEY);
        if (cached != null && !String.valueOf(cached).trim().isEmpty()) {
            return String.valueOf(cached);
        }
        String clientKey = configService.get("client_key");
        String clientSecret = configService.get("client_secret");
        if (configService.isBlank(clientKey) || configService.isBlank(clientSecret)) {
            throw new ServiceException("抖音对接参数未配置，请联系门店管理员");
        }
        JSONObject req = new JSONObject();
        req.set("client_key", clientKey);
        req.set("client_secret", clientSecret);
        req.set("grant_type", "client_credential");
        String resp;
        try {
            resp = HttpRequest.post(TOKEN_URL)
                .header("Content-Type", "application/json")
                .body(req.toString())
                .timeout(10000)
                .execute()
                .body();
        } catch (Exception e) {
            log.error("[抖音] 获取client_token请求异常", e);
            throw new ServiceException("抖音服务连接失败，请稍后重试");
        }
        JSONObject json = JSONUtil.parseObj(resp);
        JSONObject data = json.getJSONObject("data");
        int errorCode = data == null ? -1 : data.getInt("error_code", -1);
        if (errorCode != 0 || data == null) {
            log.error("[抖音] 获取client_token失败: {}", resp);
            throw new ServiceException("抖音鉴权失败(" + errorCode + ")，请联系门店管理员");
        }
        String token = data.getStr("access_token");
        int expiresIn = data.getInt("expires_in", 7200);
        long cacheSeconds = Math.max(60L, expiresIn - 300L);
        try {
            RedisUtils.setCacheObject(TOKEN_CACHE_KEY, token, cacheSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("[抖音] token 缓存写入失败, 不影响本次调用", e);
        }
        return token;
    }

    /** token 失效时清理缓存, 供重试用 */
    public void evictToken() {
        try {
            RedisUtils.deleteObject(TOKEN_CACHE_KEY);
        } catch (Exception ignore) { }
    }

    /* ==================== 三件套 ==================== */

    /**
     * 验券(用户输码后查询券信息)
     *
     * @param code          明文券码(手输)
     * @param encryptedData 加密券数据(短链解析所得, 与 code 二选一)
     * @param poiId         门店ID
     */
    public JSONObject prepare(String code, String encryptedData, String poiId) {
        StringBuilder url = new StringBuilder(API_BASE).append("prepare/?poi_id=").append(encode(poiId));
        if (!configService.isBlank(code)) {
            url.append("&code=").append(encode(code));
        }
        if (!configService.isBlank(encryptedData)) {
            url.append("&encrypted_data=").append(encode(encryptedData));
        }
        return call("GET", url.toString(), null, true);
    }

    /** 核销 */
    public JSONObject verify(String verifyToken, String poiId, String encryptedCode) {
        JSONObject body = new JSONObject();
        body.set("verify_token", verifyToken);
        body.set("poi_id", poiId);
        body.set("encrypted_codes", JSONUtil.createArray().put(encryptedCode));
        return call("POST", API_BASE + "verify/", body.toString(), true);
    }

    /** 撤销核销(1 小时内有效) */
    public JSONObject cancel(String certificateId, String verifyId) {
        JSONObject body = new JSONObject();
        body.set("certificate_id", certificateId);
        body.set("verify_id", verifyId);
        return call("POST", API_BASE + "cancel/", body.toString(), true);
    }

    /** 券状态查询(对账/排查) */
    public JSONObject query(String encryptedCode, String poiId) {
        String url = API_BASE + "query/?encrypted_data=" + encode(encryptedCode) + "&poi_id=" + encode(poiId);
        return call("GET", url, null, false);
    }

    /* ==================== 短链解析 ==================== */

    /**
     * 抖音分享短链 -> object_id(作为 encrypted_data)
     * 兼容两种输入: 纯链接, 或抖音分享的**整段文案**(从中提取 https 链接)
     * 例: https://v.douyin.com/xxxx/ -> Location 长链中的 object_id 参数
     */
    public String resolveShortLink(String shareUrl) {
        if (configService.isBlank(shareUrl)) {
            return null;
        }
        String url = extractUrl(shareUrl.trim());
        if (configService.isBlank(url)) {
            return null;
        }
        try {
            HttpResponse resp = HttpRequest.get(url).setFollowRedirects(false).timeout(8000).execute();
            String location = resp.header("Location");
            if (configService.isBlank(location)) {
                // 有些短链会直接 200 返回 html, 尝试从正文里找 object_id
                String bodyText = resp.body();
                String fromBody = extractObjectId(bodyText);
                if (!configService.isBlank(fromBody)) {
                    return fromBody;
                }
                // 跟随重定向再试一次
                String finalUrl = HttpRequest.get(url).timeout(8000).execute().body();
                return extractObjectId(finalUrl);
            }
            return extractObjectId(location);
        } catch (Exception e) {
            log.error("[抖音] 短链解析失败 url={}", url, e);
            throw new ServiceException("抖音链接解析失败，请改为直接输入券码");
        }
    }

    /** 从文本中提取第一个 http(s) 链接(兼容抖音分享的整段文案) */
    private String extractUrl(String text) {
        if (configService.isBlank(text)) {
            return null;
        }
        java.util.regex.Matcher m = java.util.regex.Pattern
            .compile("https?://[A-Za-z0-9\\-._~:/?#\\[\\]@!$&'()*+,;=%]+")
            .matcher(text);
        if (m.find()) {
            return m.group();
        }
        // 不含协议头时, 尝试 dv 短链裸域名
        java.util.regex.Matcher m2 = java.util.regex.Pattern
            .compile("(v\\.douyin\\.com/[A-Za-z0-9\\-_/]+)")
            .matcher(text);
        if (m2.find()) {
            return "https://" + m2.group(1);
        }
        return null;
    }

    private String extractObjectId(String text) {
        if (configService.isBlank(text)) {
            return null;
        }
        int idx = text.indexOf("object_id=");
        if (idx < 0) {
            return null;
        }
        String tail = text.substring(idx + "object_id=".length());
        int end = tail.length();
        for (int i = 0; i < tail.length(); i++) {
            char c = tail.charAt(i);
            if (c == '&' || c == '#' || c == '"' || c == '\'' || c == ' ') {
                end = i;
                break;
            }
        }
        String value = tail.substring(0, end);
        // 长链里的 object_id 可能是 URL 编码的, 解码后交给 prepare 再编码
        try {
            return java.net.URLDecoder.decode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }

    /* ==================== 内部 ==================== */

    private JSONObject call(String method, String url, String body, boolean needToken) {
        String resp;
        try {
            HttpRequest req = "POST".equals(method) ? HttpRequest.post(url) : HttpRequest.get(url);
            if (body != null) {
                req.header("Content-Type", "application/json").body(body);
            }
            if (needToken) {
                req.header("access-token", getAccessToken());
            }
            resp = req.timeout(15000).execute().body();
        } catch (Exception e) {
            log.error("[抖音] 接口调用异常 url={}", url, e);
            throw new ServiceException("抖音服务连接失败，请稍后重试");
        }
        JSONObject json;
        try {
            json = JSONUtil.parseObj(resp);
        } catch (Exception e) {
            log.error("[抖音] 返回报文解析失败: {}", resp);
            throw new ServiceException("抖音返回数据异常，请稍后重试");
        }
        JSONObject data = json.getJSONObject("data");
        int errorCode = data == null ? -1 : data.getInt("error_code", 0);
        if (errorCode == 2190002 || errorCode == 2190008) {
            // token 失效: 清缓存后重试一次
            log.warn("[抖音] token 失效({}), 清理缓存重试", errorCode);
            evictToken();
            try {
                HttpRequest req2 = "POST".equals(method) ? HttpRequest.post(url) : HttpRequest.get(url);
                if (body != null) {
                    req2.header("Content-Type", "application/json").body(body);
                }
                req2.header("access-token", getAccessToken());
                resp = req2.timeout(15000).execute().body();
                json = JSONUtil.parseObj(resp);
            } catch (Exception e) {
                throw new ServiceException("抖音服务连接失败，请稍后重试");
            }
        }
        return json;
    }

    private String encode(String s) {
        try {
            return URLEncoder.encode(s == null ? "" : s, "UTF-8");
        } catch (Exception e) {
            return s;
        }
    }

    /** 从返回中取 error_code(0=成功) */
    public int errorCode(JSONObject json) {
        JSONObject data = json == null ? null : json.getJSONObject("data");
        return data == null ? -1 : data.getInt("error_code", -1);
    }

    /** 便捷读取 data */
    public JSONObject dataOf(JSONObject json) {
        return json == null ? null : json.getJSONObject("data");
    }

    /** 抖音错误码 -> 中文提示 */
    public String friendlyError(int code, String fallback) {
        if (code == 2190002) return "抖音鉴权失效，请稍后重试";
        if (code == 2190004) return "抖音团购核销能力未开通，请联系门店管理员";
        if (code == 2119005) return "门店未授权，请联系门店管理员";
        if (code == 3000003) return "已超过1小时，无法撤销核销";
        if (code == 2190006) return "请求参数错误，请核对券码";
        return configService.isBlank(fallback) ? ("抖音返回异常(" + code + ")") : fallback;
    }
}
