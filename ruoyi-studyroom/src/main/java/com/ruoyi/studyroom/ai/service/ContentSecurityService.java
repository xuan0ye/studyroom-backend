package com.ruoyi.studyroom.ai.service;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.security.WxMaMsgSecCheckCheckRequest;
import cn.binarywang.wx.miniapp.bean.security.WxMaMsgSecCheckCheckResponse;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.stereotype.Component;

/**
 * 微信内容安全校验（msgSecCheck 2.0）
 * 依赖 weixin-java-miniapp 4.3.0 SDK（WxMaSecCheckService），access_token 由 SDK 托管刷新
 *
 * 用途（AIGC 合规，对应《上线合规检查报告》阻断项 3）：
 * 1. 用户输入过检：拦截违规输入，不进模型、不入库
 * 2. AI 输出过检：模型生成内容违规时替换为兜底文案
 *
 * 失败开放（fail-open）策略：
 * - 微信接口异常/超时时放行并记录日志，保证业务可用性
 * - 微信侧 87014（内容违规）错误码或 suggest=risky/review 时严格拦截
 *
 * 开关：studyroom.ai.content-security.enabled（默认 true）
 */
@Slf4j
@Component
public class ContentSecurityService {

    /** 微信小程序 appid，与 application.yml wx.miniapp.configs 对应 */
    private static final String APPID = "wx75ea31b8c4707d16";

    /** false 时跳过校验（默认 true） */
    @org.springframework.beans.factory.annotation.Value("${studyroom.ai.content-security.enabled:true}")
    private boolean enabled;

    /**
     * 文本内容安全校验
     *
     * @param content 待检文本
     * @param openid  用户 openid（可为空，传入可提升判定准确度）
     * @return true=通过；false=违规，调用方应拦截
     */
    public boolean checkText(String content, String openid) {
        if (!enabled || content == null || content.trim().isEmpty()) {
            return true;
        }
        try {
            WxMaService maService = com.ruoyi.framework.config.WxMaConfiguration.getMaService(APPID);
            // msgSecCheck 2.0：scene 2=评论（输入场景）；输出过检场景同为文本，复用即可
            WxMaMsgSecCheckCheckRequest request = WxMaMsgSecCheckCheckRequest.builder()
                .content(content)
                .version("2")
                .openid(openid)
                .scene(2)
                .build();
            WxMaMsgSecCheckCheckResponse response =
                maService.getSecCheckService().checkMessage(request);
            // 响应体：result.suggest = pass | risky | review；result.label = 命中的风险类别
            WxMaMsgSecCheckCheckResponse.ResultBean result =
                response == null ? null : response.getResult();
            String suggest = result == null ? "pass" : result.getSuggest();
            boolean pass = !"risky".equals(suggest) && !"review".equals(suggest);
            if (!pass) {
                log.warn("[内容安全] 命中风险内容 suggest={} label={} traceId={}",
                    suggest, result == null ? "-" : result.getLabel(),
                    response == null ? "-" : response.getTraceId());
            }
            return pass;
        } catch (WxErrorException e) {
            // 87014: 内容含违法违规信息 —— 严格拦截
            if (e.getError() != null && e.getError().getErrorCode() == 87014) {
                log.warn("[内容安全] 微信判定内容违规(87014)");
                return false;
            }
            // 其他错误（如 access_token 未就绪、频率限制）fail-open，记日志供人工排查
            log.error("[内容安全] 微信接口调用异常, 放行: {}", e.getMessage());
            return true;
        } catch (Exception e) {
            log.error("[内容安全] 校验服务异常, 放行: {}", e.getMessage());
            return true;
        }
    }
}
