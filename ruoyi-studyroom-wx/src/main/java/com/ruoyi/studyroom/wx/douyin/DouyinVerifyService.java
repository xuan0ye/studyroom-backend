package com.ruoyi.studyroom.wx.douyin;

import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.redis.RedisUtils;
import com.ruoyi.studyroom.domain.bo.CardBo;
import com.ruoyi.studyroom.domain.bo.CardUserBo;
import com.ruoyi.studyroom.domain.bo.DayCardConsumptionBo;
import com.ruoyi.studyroom.domain.vo.CardUserVo;
import com.ruoyi.studyroom.domain.vo.CardVo;
import com.ruoyi.studyroom.domain.vo.DayCardConsumptionVo;
import com.ruoyi.studyroom.mapper.DouyinMapper;
import com.ruoyi.studyroom.service.IBalanceService;
import com.ruoyi.studyroom.service.ICardService;
import com.ruoyi.studyroom.service.ICardUserService;
import com.ruoyi.studyroom.service.IDayCardConsumptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 抖音团购「顾客自助核销」业务
 *
 * 发放规则(2026-09-11 与门店确认):
 *   1) 代金券类(名称含 代金券/代金/抵扣/现金券, 或后台配置 grant_type=balance) -> 充值余额(1点=1元, 默认按券面额)
 *   2) 其它团购券 -> 发放本地周期卡(默认全天体验卡; 后台可配"商品->卡券"映射)
 * 流程: 验券 prepare -> 顾客确认 -> 核销 verify -> 同事务: 核销记录 + 发卡/充值
 * 撤销: 管理后台 1 小时内 cancel(抖音侧撤销 + 本地作废卡 或 扣回余额)
 * 退款审核: auditRefund —— 未核销同意; 已用(开过灯/余额已花)拒绝; 未用同意并作废卡/扣回余额
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DouyinVerifyService {

    private static final String SESSION_PREFIX = "douyin:verify:session:";
    private static final long SESSION_MINUTES = 10L;
    /** 券状态: 1已核销 2已撤销 3异常 */
    public static final int STATUS_VERIFIED = 1;
    public static final int STATUS_CANCELLED = 2;
    public static final int STATUS_ABNORMAL = 3;
    /** 发放类型 */
    public static final String GRANT_CARD = "card";
    public static final String GRANT_BALANCE = "balance";
    /** 余额动作 */
    private static final int BALANCE_NONE = 0;
    private static final int BALANCE_GRANTED = 1;
    private static final int BALANCE_REVOKED = 2;

    private static final Pattern TITLE_AMOUNT = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*元");

    private final DouyinClient douyinClient;
    private final DouyinConfigService configService;
    private final DouyinMapper douyinMapper;
    private final ICardUserService cardUserService;
    private final ICardService cardService;
    private final IDayCardConsumptionService dayCardConsumptionService;
    private final IBalanceService balanceService;

    /* ==================== 1. 验券 ==================== */

    public Map<String, Object> prepare(Long userId, String code, String shortLink) {
        Map<String, String> cfg = configService.requireConfig();
        String poiId = cfg.get("poi_id");
        String plainCode = code == null ? "" : code.trim();
        String encryptedData = null;
        if (configService.isBlank(plainCode)) {
            if (configService.isBlank(shortLink)) {
                throw new ServiceException("请输入券码或粘贴抖音团购链接");
            }
            encryptedData = douyinClient.resolveShortLink(shortLink);
            if (configService.isBlank(encryptedData)) {
                throw new ServiceException("未从链接中识别到券码，请手动输入券码");
            }
        }
        // 本地重复核销判断(券码哈希): 已核销过的券码直接给出明确提示, 且不消耗抖音调用次数
        if (!configService.isBlank(plainCode)) {
            Map<String, Object> dup = douyinMapper.selectVerifiedByCodeHash(sha256(plainCode));
            if (dup != null) {
                Object vt = dup.get("verifyTime");
                throw new ServiceException("该团购券已于 " + (vt == null ? "" : String.valueOf(vt))
                    + " 核销过，请勿重复使用");
            }
        }

        JSONObject json = douyinClient.prepare(plainCode, encryptedData, poiId);
        int errorCode = douyinClient.errorCode(json);
        JSONObject data = douyinClient.dataOf(json);
        if (errorCode != 0 || data == null) {
            throw new ServiceException(douyinClient.friendlyError(errorCode,
                data == null ? null : data.getStr("description")));
        }
        JSONArray certificates = data.getJSONArray("certificates");
        if (certificates == null || certificates.isEmpty()) {
            certificates = data.getJSONArray("certificates_v2");
        }
        if (certificates == null || certificates.isEmpty()) {
            throw new ServiceException("未查询到可用的团购券：请核对券码；若该券已使用过则不能重复核销");
        }
        JSONObject cert = certificates.getJSONObject(0);
        String verifyToken = data.getStr("verify_token");
        String encryptedCode = cert.getStr("encrypted_code");
        String certificateId = cert.getStr("certificate_id");
        if (configService.isBlank(verifyToken) || configService.isBlank(encryptedCode)) {
            throw new ServiceException("券信息异常，请稍后重试或联系门店");
        }
        // 抖音真实字段: 套餐名在 sku.title, 有效期 start_time/expire_time(秒级), 金额在 amount/sku.market_price
        JSONObject sku = cert.getJSONObject("sku");
        JSONObject amount = cert.getJSONObject("amount");
        String title = sku == null ? null : sku.getStr("title");
        String skuId = sku == null ? null : sku.getStr("sku_id");
        Long marketFen = sku == null ? null : sku.getLong("market_price");
        Long payFen = amount == null ? null : amount.getLong("pay_amount");
        String douyinOrderId = data.getStr("order_id");

        Map<String, Object> exist = douyinMapper.selectVerifiedByCertificateId(certificateId);
        if (exist != null) {
            throw new ServiceException("该团购券已核销，请勿重复使用");
        }

        String sessionId = UUID.randomUUID().toString().replace("-", "");
        JSONObject session = new JSONObject();
        session.set("userId", userId);
        session.set("verifyToken", verifyToken);
        session.set("certificateId", certificateId);
        session.set("encryptedCode", encryptedCode);
        session.set("poiId", poiId);
        session.set("code", plainCode);
        session.set("productId", skuId);
        session.set("productTitle", title);
        session.set("douyinOrderId", douyinOrderId);
        session.set("marketFen", marketFen);
        session.set("payFen", payFen);
        session.set("rawPrepare", truncate(json.toString(), 4000));
        try {
            RedisUtils.setCacheObject(SESSION_PREFIX + sessionId, session.toString(), SESSION_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("[抖音核销] 会话缓存写入失败", e);
            throw new ServiceException("系统繁忙，请稍后重试");
        }

        Map<String, Object> view = new HashMap<>();
        view.put("sessionId", sessionId);
        view.put("productTitle", firstNotBlank(title, "团购券"));
        view.put("productId", skuId);
        view.put("certificateId", certificateId);
        view.put("encryptedCode", encryptedCode);
        view.put("validStart", epochToText(cert.getLong("start_time")));
        view.put("validEnd", epochToText(cert.getLong("expire_time")));
        view.put("status", cert.getStr("status"));
        view.put("grantType", decideGrantType(skuId, title));
        if (marketFen != null || payFen != null) {
            StringBuilder sb = new StringBuilder();
            if (marketFen != null) {
                sb.append("面额 ¥").append(fen2yuan(marketFen));
            }
            if (payFen != null) {
                if (sb.length() > 0) {
                    sb.append("，");
                }
                sb.append("实付 ¥").append(fen2yuan(payFen));
            }
            view.put("amountText", sb.toString());
        }
        return view;
    }

    /* ==================== 2. 核销 ==================== */

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> verify(Long userId, String sessionId, Long seatId) {
        if (configService.isBlank(sessionId)) {
            throw new ServiceException("请先验券");
        }
        Object cached = RedisUtils.getCacheObject(SESSION_PREFIX + sessionId);
        if (cached == null) {
            throw new ServiceException("验券已过期，请重新输入券码");
        }
        JSONObject session = JSONUtil.parseObj(String.valueOf(cached));
        if (!String.valueOf(userId).equals(session.getStr("userId"))) {
            throw new ServiceException("验券信息不属于当前账号");
        }
        String poiId = session.getStr("poiId");
        String encryptedCode = session.getStr("encryptedCode");
        String certificateId = session.getStr("certificateId");
        String verifyToken = session.getStr("verifyToken");
        String sessionProductId = session.getStr("productId");

        // 每用户限核销一次的商品(如双周卡): 防重复薅, 校验放在调抖音之前
        Map<String, Object> onceCfg = douyinMapper.selectProductMapByProductId(sessionProductId);
        if (onceCfg != null && onceCfg.get("oncePerUser") != null
            && Integer.parseInt(String.valueOf(onceCfg.get("oncePerUser"))) == 1) {
            long cnt = douyinMapper.countVerifiedByUserAndProduct(userId, sessionProductId);
            if (cnt > 0) {
                throw new ServiceException("该团购商品每人限核销一次，你已核销过，不能重复使用");
            }
        }

        JSONObject json = douyinClient.verify(verifyToken, poiId, encryptedCode);
        int errorCode = douyinClient.errorCode(json);
        JSONObject data = douyinClient.dataOf(json);
        if (errorCode != 0 || data == null) {
            throw new ServiceException(douyinClient.friendlyError(errorCode,
                data == null ? null : data.getStr("description")));
        }
        JSONArray results = data.getJSONArray("verify_results");
        if (results == null || results.isEmpty()) {
            throw new ServiceException("抖音核销结果异常，请稍后重试或联系门店");
        }
        JSONObject r0 = results.getJSONObject(0);
        int result = r0.getInt("result", -1);
        String verifyId = r0.getStr("verify_id");
        String certId = firstNotBlank(r0.getStr("certificate_id"), certificateId);
        if (result != 0) {
            throw new ServiceException(resultMessage(result));
        }

        String productId = firstNotBlank(r0.getStr("product_id"), session.getStr("productId"));
        String productTitle = firstNotBlank(r0.getStr("product_title"), session.getStr("productTitle"));
        String douyinOrderId = firstNotBlank(r0.getStr("order_id"), session.getStr("douyinOrderId"));
        String rawVerify = truncate(json.toString(), 4000);
        String grantType = decideGrantType(productId, productTitle);
        Map<String, Object> product = douyinMapper.selectProductMapByProductId(productId);
        Map<String, Object> out = new HashMap<>();
        out.put("success", true);

        if (GRANT_BALANCE.equals(grantType)) {
            /* ===== 代金券 -> 充值余额 ===== */
            int points = decideBalancePoints(product, productTitle,
                session.getLong("marketFen"), session.getLong("payFen"));
            if (points <= 0) {
                insertAbnormal(userId, poiId, session, certId, verifyId, douyinOrderId, productId, productTitle,
                    GRANT_BALANCE, 0, "抖音核销成功但无法确定充值金额，需人工处理", rawVerify);
                throw new ServiceException("核销成功但充值失败，请联系门店工作人员处理（已留记录）");
            }
            balanceService.add(userId, points);
            Map<String, Object> rec = baseRecord(userId, poiId, session, certId, verifyId, douyinOrderId,
                productId, productTitle, rawVerify);
            rec.put("grantType", GRANT_BALANCE);
            rec.put("balanceAmount", points);
            rec.put("balanceAction", BALANCE_GRANTED);
            rec.put("status", STATUS_VERIFIED);
            rec.put("seatId", null);
            rec.put("remark", "自助核销成功，已充值余额" + points + "元");
            douyinMapper.insertVerify(rec);
            clearSession(sessionId);
            out.put("grantType", GRANT_BALANCE);
            out.put("balanceAmount", points);
            out.put("message", "核销成功，已充值余额 ¥" + points);
            return out;
        }

        /* ===== 周期卡 -> 发卡(+可选锁座) ===== */
        Long cardId = product == null || product.get("cardId") == null
            ? findDefaultDayCardId() : Long.valueOf(String.valueOf(product.get("cardId")));
        CardVo card = cardId == null ? null : cardService.queryById(cardId);
        if (card == null) {
            insertAbnormal(userId, poiId, session, certId, verifyId, douyinOrderId, productId, productTitle,
                GRANT_CARD, null, "抖音核销成功但本地无可发放卡券，需人工处理", rawVerify);
            throw new ServiceException("核销成功但发卡失败，请联系门店工作人员处理（已留记录）");
        }
        int term = card.getTerm() == null ? 1 : card.getTerm().intValue();
        CardUserBo cu = new CardUserBo();
        cu.setUserId(userId);
        cu.setCardId(card.getId());
        // 发卡不写有效期: 自首次预约使用起算, 核销后暂不使用也不会过期
        cardUserService.insertByBo(cu);
        Long cardUserId = cu.getId();

        Long consumptionId = null;
        if (seatId != null && cardUserId != null) {
            DayCardConsumptionBo bo = new DayCardConsumptionBo();
            bo.setUserId(userId);
            bo.setCardUserId(cardUserId);
            bo.setSeatId(seatId);
            bo.setConsumptionDate(new Date());
            dayCardConsumptionService.bookByDayCard(bo);
            consumptionId = bo.getId();
        }

        Map<String, Object> rec = baseRecord(userId, poiId, session, certId, verifyId, douyinOrderId,
            productId, productTitle, rawVerify);
        rec.put("grantType", GRANT_CARD);
        rec.put("balanceAction", BALANCE_NONE);
        rec.put("status", STATUS_VERIFIED);
        rec.put("cardUserId", cardUserId);
        rec.put("cardId", card.getId());
        rec.put("seatId", seatId);
        rec.put("remark", "自助核销成功");
        douyinMapper.insertVerify(rec);
        clearSession(sessionId);

        // 核销时若已指定座位 => bookByDayCard 已完成"首次使用激活", 有效期为首次使用 + term 天
        String expiryOut = null;
        if (consumptionId != null && cardUserId != null) {
            com.ruoyi.studyroom.domain.vo.CardUserVo issued = cardUserService.queryById(cardUserId);
            if (issued != null && issued.getExpiryTime() != null) {
                expiryOut = DateUtil.formatDateTime(issued.getExpiryTime());
            }
        }
        out.put("grantType", GRANT_CARD);
        out.put("cardId", card.getId());
        out.put("cardTitle", card.getCardTitle());
        out.put("expiryTime", expiryOut);
        out.put("consumptionId", consumptionId);
        out.put("seatId", seatId);
        out.put("message", expiryOut == null
            ? "核销成功，已发放「" + card.getCardTitle() + "」，首次预约使用后 " + term + " 天内有效"
            : "核销成功，已发放「" + card.getCardTitle() + "」");
        return out;
    }

    /* ==================== 3. 撤销核销(管理后台, 1小时内) ==================== */

    /**
     * 注意: 不加全局事务 —— 内部扣余额/解锁抛异常时若共享外层事务会标记 rollback-only,
     * 导致"抖音已撤销但本地回滚"。各步骤各自幂等, 失败记日志并继续。
     */
    public String cancel(Long recordId, String remark) {
        Map<String, Object> rec = douyinMapper.selectVerifyById(recordId);
        if (rec == null) {
            throw new ServiceException("核销记录不存在");
        }
        if (Integer.valueOf(STATUS_CANCELLED).equals(toInt(rec.get("status")))) {
            return "该记录已撤销";
        }
        String certificateId = str(rec.get("certificateId"));
        String verifyId = str(rec.get("verifyId"));
        Object verifyTime = rec.get("verifyTime");
        if (verifyTime instanceof Date && System.currentTimeMillis() - ((Date) verifyTime).getTime() > 3600_000L) {
            throw new ServiceException("已超过1小时，无法撤销核销");
        }
        JSONObject json = douyinClient.cancel(certificateId, verifyId);
        int errorCode = douyinClient.errorCode(json);
        if (errorCode != 0) {
            throw new ServiceException(douyinClient.friendlyError(errorCode, "撤销核销失败"));
        }

        Long userId = rec.get("userId") == null ? null : Long.valueOf(String.valueOf(rec.get("userId")));
        String grantType = str(rec.get("grantType"));
        String extraMsg = "";
        if (GRANT_BALANCE.equals(grantType)) {
            extraMsg = revokeBalance(rec);
        } else {
            extraMsg = revokeCard(rec, userId);
        }
        douyinMapper.updateVerifyStatus(recordId, STATUS_CANCELLED, new Date(),
            (remark == null || remark.trim().isEmpty() ? "管理后台撤销核销" : remark) + extraMsg);
        return "已撤销核销" + extraMsg;
    }

    /* ==================== 4. 退款审核(SPI) ==================== */

    /**
     * 退款审核(官方退款申请SPI, 支持一次请求多张券):
     * 官方规定**同一请求不支持部分拒绝部分通过** -> 任一券已使用则整单拒绝。
     * 采用两阶段: 先判定(无副作用) -> 全部可退才执行作废卡/扣回余额。
     *
     * @param certificateIds 请求中的券ID列表
     * @return {result: 0/1/2, reason: String}  0=待确认 1=允许 2=拒绝
     */
    public Map<String, Object> auditRefundBatch(List<String> certificateIds) {
        Map<String, Object> resp = new HashMap<>();
        if (certificateIds == null || certificateIds.isEmpty()) {
            resp.put("result", 1);
            resp.put("reason", "请求未包含券码，默认允许退款");
            return resp;
        }
        // ---------- 阶段1: 判定 ----------
        List<Map<String, Object>> verifiedRecords = new ArrayList<>();
        for (String certificateId : certificateIds) {
            if (configService.isBlank(certificateId)) {
                continue;
            }
            Map<String, Object> rec;
            try {
                rec = douyinMapper.selectVerifiedByCertificateId(certificateId);
            } catch (Exception e) {
                log.error("[抖音退款审核] 查询核销记录失败 certificateId={}", certificateId, e);
                resp.put("result", 1);
                resp.put("reason", "系统查询异常，按允许退款兜底");
                return resp;
            }
            if (rec == null) {
                continue;   // 未核销 -> 可退
            }
            verifiedRecords.add(rec);
            String grantType = str(rec.get("grantType"));
            if (GRANT_BALANCE.equals(grantType)) {
                Integer action = toInt(rec.get("balanceAction"));
                if (action != null && action == BALANCE_REVOKED) {
                    continue;   // 已扣回过 -> 可退
                }
                int amount = rec.get("balanceAmount") == null ? 0 : Integer.parseInt(String.valueOf(rec.get("balanceAmount")));
                Long userId = rec.get("userId") == null ? null : Long.valueOf(String.valueOf(rec.get("userId")));
                long balance = 0L;
                try {
                    if (userId != null) {
                        com.ruoyi.studyroom.domain.vo.BalanceVo bv = balanceService.queryByUserId(userId);
                        balance = bv == null || bv.getBalance() == null ? 0L : bv.getBalance();
                    }
                } catch (Exception e) {
                    log.warn("[抖音退款审核] 查询余额失败 userId={}", userId, e);
                }
                if (amount > 0 && balance < amount) {
                    resp.put("result", 2);
                    resp.put("reason", "该代金券金额已使用，不支持退款，如有疑问请联系门店");
                    return resp;
                }
            } else {
                if (Boolean.TRUE.equals(isCardUsedByRecord(rec))) {
                    resp.put("result", 2);
                    resp.put("reason", "该团购券已到店核销并使用（已开灯），不支持退款，如有疑问请联系门店");
                    return resp;
                }
            }
        }
        // ---------- 阶段2: 全部可退 -> 执行作废卡/扣回余额 ----------
        for (Map<String, Object> rec : verifiedRecords) {
            try {
                if (GRANT_BALANCE.equals(str(rec.get("grantType")))) {
                    revokeBalance(rec);
                } else {
                    revokeCard(rec, rec.get("userId") == null ? null : Long.valueOf(String.valueOf(rec.get("userId"))));
                }
            } catch (Exception e) {
                log.warn("[抖音退款审核] 执行本地作废/扣回异常 id={}", rec.get("id"), e);
            }
        }
        resp.put("result", 1);
        resp.put("reason", verifiedRecords.isEmpty() ? "该券未核销，允许退款" : "已核销但未使用，允许退款（本地卡已作废/余额已扣回）");
        return resp;
    }

    /** 单券退款审核(兼容旧调用) */
    public Map<String, Object> auditRefund(String certificateId) {
        List<String> ids = new ArrayList<>();
        if (!configService.isBlank(certificateId)) {
            ids.add(certificateId);
        }
        Map<String, Object> r = auditRefundBatch(ids);
        // 兼容旧字段名 agree
        r.put("agree", Integer.valueOf(1).equals(r.get("result")));
        return r;
    }

    /** 兼容保留: 是否已使用(开过灯/签到) */
    public Boolean isCardUsed(String certificateId) {
        if (configService.isBlank(certificateId)) {
            return null;
        }
        Map<String, Object> rec = douyinMapper.selectVerifiedByCertificateId(certificateId);
        if (rec == null) {
            return null;
        }
        return isCardUsedByRecord(rec);
    }

    /** 兼容保留: 作废未使用的本地卡 */
    public void revokeCardByCertificate(String certificateId) {
        if (configService.isBlank(certificateId)) {
            return;
        }
        Map<String, Object> rec = douyinMapper.selectVerifiedByCertificateId(certificateId);
        if (rec == null) {
            return;
        }
        revokeCard(rec, rec.get("userId") == null ? null : Long.valueOf(String.valueOf(rec.get("userId"))));
    }

    /* ==================== 5. 查询 ==================== */

    public Map<String, Object> listRecords(Long userId, Long queryUserId, Integer status, int pageNum, int pageSize) {
        Map<String, Object> q = new HashMap<>();
        q.put("userId", queryUserId != null ? queryUserId : userId);
        q.put("status", status);
        int pn = Math.max(1, pageNum);
        int ps = Math.min(100, Math.max(1, pageSize));
        q.put("offset", (pn - 1) * ps);
        q.put("limit", ps);
        List<Map<String, Object>> rows = douyinMapper.selectVerifyList(q);
        long total = douyinMapper.countVerify(q);
        Map<String, Object> out = new HashMap<>();
        out.put("rows", rows == null ? new ArrayList<>() : rows);
        out.put("total", total);
        return out;
    }

    /* ==================== 内部实现 ==================== */

    /** 判定发放类型: 后台映射优先, 否则按商品名识别代金券 */
    private String decideGrantType(String productId, String productTitle) {
        Map<String, Object> map = douyinMapper.selectProductMapByProductId(productId);
        if (map != null && !configService.isBlank(str(map.get("grantType")))) {
            return str(map.get("grantType"));
        }
        String title = productTitle == null ? "" : productTitle;
        if (title.contains("代金") || title.contains("抵扣") || title.contains("现金券")
            || title.contains("抵用") || title.contains("券包")) {
            return GRANT_BALANCE;
        }
        return GRANT_CARD;
    }

    /** 判定充值点数: 后台配置 > 券名中的"N元" > 券面额(分->元) > 实付金额 */
    private int decideBalancePoints(Map<String, Object> map, String productTitle, Long marketFen, Long payFen) {
        if (map != null && map.get("balanceAmount") != null) {
            try {
                int v = Integer.parseInt(String.valueOf(map.get("balanceAmount")));
                if (v > 0) {
                    return v;
                }
            } catch (Exception ignore) { }
        }
        if (!configService.isBlank(productTitle)) {
            Matcher m = TITLE_AMOUNT.matcher(productTitle);
            if (m.find()) {
                try {
                    return (int) Math.floor(Double.parseDouble(m.group(1)));
                } catch (Exception ignore) { }
            }
        }
        if (marketFen != null && marketFen > 0) {
            return (int) (marketFen / 100);
        }
        if (payFen != null && payFen > 0) {
            return (int) (payFen / 100);
        }
        return 0;
    }

    /** 作废未使用的本地卡; 返回补充说明 */
    private String revokeCard(Map<String, Object> rec, Long userId) {
        Long cardUserId = rec.get("cardUserId") == null ? null : Long.valueOf(String.valueOf(rec.get("cardUserId")));
        if (cardUserId == null) {
            return "";
        }
        // 先释放锁座(无锁座会抛异常, 忽略)
        try {
            dayCardConsumptionService.unlockByCard(cardUserId, userId);
        } catch (Exception e) {
            log.info("[抖音核销] 撤销时释放锁座跳过: {}", e.getMessage());
        }
        try {
            Boolean used = isCardUsedByRecord(rec);
            if (Boolean.TRUE.equals(used)) {
                return "（该卡已使用，未删除）";
            }
            cardUserService.deleteWithValidByIds(java.util.Collections.singleton(cardUserId), false);
            return "，本地使用卡已作废";
        } catch (Exception e) {
            log.warn("[抖音核销] 作废本地卡失败 cardUserId={}", cardUserId, e);
            return "（本地卡作废失败，需人工处理）";
        }
    }

    /** 扣回余额(代金券撤销/退款); 返回补充说明 */
    private String revokeBalance(Map<String, Object> rec) {
        Integer action = toInt(rec.get("balanceAction"));
        if (action != null && action == BALANCE_REVOKED) {
            return "，余额已扣回";
        }
        int amount = rec.get("balanceAmount") == null ? 0 : Integer.parseInt(String.valueOf(rec.get("balanceAmount")));
        Long userId = rec.get("userId") == null ? null : Long.valueOf(String.valueOf(rec.get("userId")));
        if (amount <= 0 || userId == null) {
            return "";
        }
        try {
            balanceService.deduct(BigDecimal.valueOf(amount), userId);
            douyinMapper.updateBalanceAction(Long.valueOf(String.valueOf(rec.get("id"))), BALANCE_REVOKED);
            return "，已扣回余额" + amount + "元";
        } catch (Exception e) {
            log.warn("[抖音核销] 撤销时扣回余额失败(余额可能已被消费) userId={} amount={}", userId, amount);
            return "（余额已消费，未能扣回，需人工处理）";
        }
    }

    /** 卡是否已使用: 有签到/开灯记录(status=1)即视为已使用 */
    private Boolean isCardUsedByRecord(Map<String, Object> rec) {
        Long cardUserId = rec.get("cardUserId") == null ? null : Long.valueOf(String.valueOf(rec.get("cardUserId")));
        if (cardUserId == null) {
            return Boolean.FALSE;
        }
        DayCardConsumptionBo q = new DayCardConsumptionBo();
        q.setCardUserId(cardUserId);
        List<DayCardConsumptionVo> list = dayCardConsumptionService.queryList(q);
        if (list != null) {
            for (DayCardConsumptionVo d : list) {
                if (d.getStatus() == null || d.getStatus() == 1) {
                    return Boolean.TRUE;
                }
            }
        }
        return Boolean.FALSE;
    }

    private Map<String, Object> baseRecord(Long userId, String poiId, JSONObject session, String certId,
                                           String verifyId, String douyinOrderId, String productId,
                                           String productTitle, String rawVerify) {
        Map<String, Object> rec = new HashMap<>();
        rec.put("userId", userId);
        rec.put("poiId", poiId);
        rec.put("code", maskCode(session.getStr("code")));
        rec.put("codeHash", configService.isBlank(session.getStr("code")) ? null : sha256(session.getStr("code")));
        rec.put("encryptedCode", session.getStr("encryptedCode"));
        rec.put("certificateId", certId);
        rec.put("verifyId", verifyId);
        rec.put("douyinOrderId", douyinOrderId);
        rec.put("productId", productId);
        rec.put("productTitle", productTitle);
        rec.put("verifyTime", new Date());
        rec.put("rawPrepare", session.getStr("rawPrepare"));
        rec.put("rawVerify", rawVerify);
        return rec;
    }

    private void insertAbnormal(Long userId, String poiId, JSONObject session, String certId, String verifyId,
                                String douyinOrderId, String productId, String productTitle,
                                String grantType, Integer balanceAmount, String remark, String rawVerify) {
        try {
            Map<String, Object> rec = baseRecord(userId, poiId, session, certId, verifyId, douyinOrderId,
                productId, productTitle, rawVerify);
            rec.put("grantType", grantType);
            rec.put("balanceAmount", balanceAmount);
            rec.put("balanceAction", BALANCE_NONE);
            rec.put("status", STATUS_ABNORMAL);
            rec.put("remark", remark);
            douyinMapper.insertVerify(rec);
        } catch (Exception e) {
            log.error("[抖音核销] 写入异常流水失败 certId={}", certId, e);
        }
    }

    private void clearSession(String sessionId) {
        try {
            RedisUtils.deleteObject(SESSION_PREFIX + sessionId);
        } catch (Exception ignore) { }
    }

    private Long findDefaultDayCardId() {
        try {
            CardBo bo = new CardBo();
            bo.setStatus(1);
            List<CardVo> cards = cardService.queryList(bo);
            if (cards == null || cards.isEmpty()) {
                return null;
            }
            for (CardVo c : cards) {
                if (c.getTerm() != null && c.getTerm().intValue() == 1) {
                    return c.getId();
                }
            }
            return cards.get(0).getId();
        } catch (Exception e) {
            log.warn("[抖音核销] 查询默认卡失败", e);
            return null;
        }
    }

    private String resultMessage(int result) {
        switch (result) {
            case 2:  return "该团购券已核销，请勿重复使用";
            case 3:  return "该团购券正在退款中，无法核销";
            case 4:  return "该团购券已退款，无法核销";
            case 5:  return "该团购券未到可用日期，暂不能核销";
            case 32: return "该团购券已过期退款中，无法核销";
            default: return "核销失败（抖音返回 " + result + "），请联系门店工作人员";
        }
    }

    private String maskCode(String code) {
        if (configService.isBlank(code)) {
            return null;
        }
        String c = code.trim();
        if (c.length() <= 4) {
            return "****";
        }
        return c.substring(0, 2) + "****" + c.substring(c.length() - 2);
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

    private String firstNotBlank(String... arr) {
        if (arr != null) {
            for (String s : arr) {
                if (!configService.isBlank(s)) {
                    return s;
                }
            }
        }
        return null;
    }

    private Integer toInt(Object o) {
        return o == null ? null : Integer.valueOf(String.valueOf(o));
    }

    private String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private String epochToText(Long epochSeconds) {
        if (epochSeconds == null || epochSeconds <= 0) {
            return null;
        }
        return DateUtil.formatDateTime(new Date(epochSeconds * 1000L));
    }

    /** 券码 SHA-256(本地去重用, 不保存明文) */
    private String sha256(String text) {
        if (configService.isBlank(text)) {
            return null;
        }
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(text.trim().getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("[抖音核销] 券码哈希失败", e);
            return null;
        }
    }

    private String fen2yuan(Long fen) {
        if (fen == null) {
            return "0.00";
        }
        return BigDecimal.valueOf(fen).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP).toPlainString();
    }
}
