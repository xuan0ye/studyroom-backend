package com.ruoyi.studyroom.wx.douyin;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.studyroom.domain.bo.CardBo;
import com.ruoyi.studyroom.domain.vo.CardVo;
import com.ruoyi.studyroom.mapper.DouyinMapper;
import com.ruoyi.studyroom.service.ICardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理后台 - 抖音团购核销管理
 *   GET  /studyroom/douyin/config        查看配置
 *   POST /studyroom/douyin/config        保存配置
 *   GET  /studyroom/douyin/verify/list   核销记录
 *   POST /studyroom/douyin/verify/cancel 撤销核销(1小时内)
 *   GET  /studyroom/douyin/productMap/list / save / delete  商品-卡映射
 *   GET  /studyroom/douyin/cards         本地卡券下拉
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/studyroom/douyin")
public class DouyinAdminController extends BaseController {

    private final DouyinConfigService configService;
    private final DouyinVerifyService douyinVerifyService;
    private final DouyinMapper douyinMapper;
    private final ICardService cardService;

    @GetMapping("/config")
    public R<Map<String, Object>> config() {
        Map<String, String> cfg = configService.loadAll();
        Map<String, Object> out = new HashMap<>();
        out.put("clientKey", cfg.get("client_key"));
        String secret = cfg.get("client_secret");
        out.put("clientSecretMask", configService.isBlank(secret) ? ""
            : secret.substring(0, Math.min(6, secret.length())) + "******");
        out.put("hasSecret", !configService.isBlank(secret));
        out.put("poiId", cfg.get("poi_id"));
        out.put("enabled", cfg.get("enabled"));
        out.put("spiVerifyEnabled", cfg.get("spi_verify_enabled"));
        return R.ok(out);
    }

    @PostMapping("/config")
    public R<Void> saveConfig(@RequestBody Map<String, Object> body) {
        saveIfPresent(body, "clientKey", "client_key");
        // 密钥留空表示不修改
        String secret = str(body.get("clientSecret"));
        if (!configService.isBlank(secret) && !secret.contains("******")) {
            configService.save("client_secret", secret);
        }
        saveIfPresent(body, "poiId", "poi_id");
        saveIfPresent(body, "enabled", "enabled");
        saveIfPresent(body, "spiVerifyEnabled", "spi_verify_enabled");
        return R.ok();
    }

    @GetMapping("/verify/list")
    public R<Map<String, Object>> verifyList(@RequestParam(defaultValue = "1") Integer pageNum,
                                             @RequestParam(defaultValue = "20") Integer pageSize,
                                             @RequestParam(required = false) Long userId,
                                             @RequestParam(required = false) Integer status) {
        return R.ok(douyinVerifyService.listRecords(null, userId, status,
            pageNum == null ? 1 : pageNum, pageSize == null ? 20 : pageSize));
    }

    @PostMapping("/verify/cancel")
    public R<String> cancelVerify(@RequestBody Map<String, Object> body) {
        Long id = body.get("id") == null ? null : Long.valueOf(String.valueOf(body.get("id")));
        if (id == null) {
            return R.fail("缺少核销记录ID");
        }
        return R.ok(douyinVerifyService.cancel(id, str(body.get("remark"))));
    }

    @GetMapping("/productMap/list")
    public R<List<Map<String, Object>>> productMapList() {
        List<Map<String, Object>> list = douyinMapper.selectProductMaps();
        return R.ok(list == null ? new ArrayList<>() : list);
    }

    @PostMapping("/productMap/save")
    public R<Void> productMapSave(@RequestBody Map<String, Object> body) {
        Map<String, Object> p = new HashMap<>();
        p.put("id", body.get("id") == null ? null : Long.valueOf(String.valueOf(body.get("id"))));
        p.put("productId", str(body.get("productId")));
        p.put("productTitle", str(body.get("productTitle")));
        p.put("cardId", body.get("cardId") == null ? null : Long.valueOf(String.valueOf(body.get("cardId"))));
        String grantType = configService.isBlank(str(body.get("grantType"))) ? "card" : str(body.get("grantType"));
        p.put("grantType", grantType);
        p.put("balanceAmount", body.get("balanceAmount") == null || configService.isBlank(str(body.get("balanceAmount")))
            ? null : Integer.valueOf(String.valueOf(body.get("balanceAmount"))));
        p.put("oncePerUser", body.get("oncePerUser") == null || configService.isBlank(str(body.get("oncePerUser")))
            ? 0 : Integer.valueOf(String.valueOf(body.get("oncePerUser"))));
        p.put("lockMode", configService.isBlank(str(body.get("lockMode"))) ? "day" : str(body.get("lockMode")));
        p.put("remark", str(body.get("remark")));
        if (!"balance".equals(grantType) && p.get("cardId") == null) {
            return R.fail("发放类型为发卡时必须选择本地卡券");
        }
        if (p.get("id") == null) {
            douyinMapper.insertProductMap(p);
        } else {
            douyinMapper.updateProductMap(p);
        }
        return R.ok();
    }

    @DeleteMapping("/productMap/{id}")
    public R<Void> productMapDelete(@PathVariable Long id) {
        douyinMapper.deleteProductMap(id);
        return R.ok();
    }

    /** 本地卡券下拉(用于商品映射) */
    @GetMapping("/cards")
    public R<List<CardVo>> cards() {
        CardBo bo = new CardBo();
        bo.setStatus(1);
        List<CardVo> cards = cardService.queryList(bo);
        return R.ok(cards == null ? new ArrayList<>() : cards);
    }

    private void saveIfPresent(Map<String, Object> body, String jsonKey, String configKey) {
        Object v = body.get(jsonKey);
        if (v != null) {
            configService.save(configKey, String.valueOf(v));
        }
    }

    private String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}
