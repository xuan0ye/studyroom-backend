package com.ruoyi.studyroom.wx.api;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.github.binarywang.wxpay.bean.notify.WxPayNotifyResponse;
import com.github.binarywang.wxpay.bean.notify.WxPayOrderNotifyV3Result;
import com.github.binarywang.wxpay.bean.request.WxPayOrderQueryV3Request;
import com.github.binarywang.wxpay.bean.result.WxPayOrderQueryV3Result;
import com.github.binarywang.wxpay.bean.request.WxPayUnifiedOrderV3Request;
import com.github.binarywang.wxpay.bean.result.enums.TradeTypeEnum;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.github.binarywang.wxpay.service.WxPayService;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.constant.PayConstants;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.utils.OrderNumUtils;
import com.ruoyi.studyroom.domain.bo.OrderBo;
import com.ruoyi.studyroom.domain.bo.OrderCardBo;
import com.ruoyi.studyroom.domain.bo.OrderRechargeBo;
import com.ruoyi.studyroom.domain.bo.SeatRecordBo;
import com.ruoyi.studyroom.domain.vo.CardVo;
import com.ruoyi.studyroom.domain.vo.OrderCardVo;
import com.ruoyi.studyroom.domain.vo.OrderRechargeVo;
import com.ruoyi.studyroom.service.ICardService;
import com.ruoyi.studyroom.service.IOrderCardService;
import com.ruoyi.studyroom.service.IOrderRechargeService;
import com.ruoyi.studyroom.service.IOrderService;
import com.ruoyi.studyroom.service.ISeatRecordService;
import com.ruoyi.studyroom.service.MiotService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Date;

/**
 * @ProjectName: study-room-vue
 * @Author: WenZhengcheng
 * @Date: 2022-5-9 下午 10:08
 * @Desc:
 */

@Slf4j
@Api(value = "小程序支付控制器",tags = "小程序支付")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/wx/pay")
public class PayController extends BaseController {

    private final WxPayService payService;
    private final IOrderCardService orderCardService;
    private final IOrderRechargeService rechargeService;
    private final IOrderService orderService;
    private final MiotService miotService;
    private final com.ruoyi.studyroom.service.IBalanceService balanceService;
    private final com.ruoyi.studyroom.service.IPointsService pointsService;
    private final ICardService cardService;


    @ApiOperation("购买优惠卡统一下单，并组装所需支付参数")
    @PostMapping("/createOrder/card")
    public R<Object> payCard(@RequestBody OrderCardBo bo) {
        String orderNo = OrderNumUtils.getOrderNum();
        bo.setUserId(LoginHelper.getUserId());
        bo.setOrderId(orderNo);
        bo.setPayStatus(PayConstants.UNPAID);
        try {
            // ===== 服务端定价(禁止信任客户端金额, 防止 0 元/任意价购卡) =====
            if (bo.getCardId() == null) {
                return R.fail("请选择要购买的卡");
            }
            CardVo card = cardService.queryById(bo.getCardId());
            if (card == null || card.getStatus() == null || card.getStatus() != 1) {
                return R.fail("该卡不存在或已下架");
            }
            // 订单金额以服务端卡价为准, 覆盖客户端提交值
            java.math.BigDecimal cardPrice = card.getPrice() == null ? java.math.BigDecimal.ZERO : card.getPrice();
            bo.setAmountTotal(cardPrice);
            // ===== 余额/积分抵扣逻辑 =====
            java.math.BigDecimal balanceDeduct = bo.getBalanceAmount() == null ? java.math.BigDecimal.ZERO : bo.getBalanceAmount();
            int pointsCost = bo.getPointsCost() == null ? 0 : bo.getPointsCost();
            // 积分抵扣: 1000积分 = 1元
            java.math.BigDecimal pointsDeduct = java.math.BigDecimal.valueOf(pointsCost).divide(java.math.BigDecimal.valueOf(1000), 2, java.math.RoundingMode.DOWN);
            if (balanceDeduct.compareTo(java.math.BigDecimal.ZERO) < 0 || pointsCost < 0) {
                return R.fail("抵扣参数错误");
            }
            if (balanceDeduct.add(pointsDeduct).compareTo(cardPrice) > 0) {
                return R.fail("抵扣金额不能超过卡价");
            }
            // 余额按整数点抵扣(向上取整, 与余额扣减一致)
            java.math.BigDecimal balanceDeductInt = balanceDeduct.setScale(0, java.math.RoundingMode.CEILING);
            java.math.BigDecimal cashAmount = cardPrice.subtract(balanceDeductInt).subtract(pointsDeduct);
            if (cashAmount.compareTo(java.math.BigDecimal.ZERO) < 0) {
                cashAmount = java.math.BigDecimal.ZERO;
            }
            // 余额校验并扣减
            if (balanceDeductInt.compareTo(java.math.BigDecimal.ZERO) > 0) {
                balanceService.deduct(balanceDeductInt, bo.getUserId());
            }
            // 积分校验并扣减
            if (pointsCost > 0) {
                pointsService.spend(bo.getUserId(), pointsCost, 4, "购买" + bo.getDescription() + "抵扣");
            }
            bo.setBalanceAmount(balanceDeductInt);
            bo.setPointsAmount(pointsDeduct);
            bo.setPointsCost(pointsCost);
            bo.setCashAmount(cashAmount);

            // 全额抵扣: 无需微信支付, 直接发卡
            if (cashAmount.compareTo(java.math.BigDecimal.ZERO) <= 0) {
                bo.setPayStatus(PayConstants.PAID);
                // 先插入订单记录(标记已支付), 再发卡
                boolean inserted = orderCardService.insertByBo(bo);
                if (!inserted) {
                    return R.fail("下单失败，请重试");
                }
                boolean done = orderCardService.updateByBoAndCard(bo);
                if (!done) {
                    return R.fail("发卡失败，请联系客服");
                }
                Map<String, Object> payResult = new HashMap<>();
                payResult.put("orderId", orderNo);
                payResult.put("payType", "balance");
                payResult.put("payStatus", PayConstants.PAID);
                return R.ok(payResult);
            }

            // 部分抵扣/未抵扣: 生成微信支付订单(只付剩余金额)
            WxPayUnifiedOrderV3Request.Payer payer = new WxPayUnifiedOrderV3Request.Payer();
            WxPayUnifiedOrderV3Request.Amount amount = new WxPayUnifiedOrderV3Request.Amount();
            WxPayUnifiedOrderV3Request request = new WxPayUnifiedOrderV3Request();
            amount.setTotal(cashAmount.multiply(Constants.MULTIPLE).intValue());
            request.setPayer(payer.setOpenid(LoginHelper.getWxLoginUser().getOpenid()));
            request.setAmount(amount);
            request.setDescription(bo.getDescription());
            request.setOutTradeNo(bo.getOrderId());
            request.setAttach(Constants.CARD);
            boolean flag = orderCardService.insertByBo(bo);
            if (!flag){
                return R.fail();
            }
            Object orderV3 = this.payService.createOrderV3(TradeTypeEnum.JSAPI, request);
            Map<String, Object> payResult = new HashMap<>();
            payResult.put("orderId", orderNo);
            payResult.putAll(BeanUtil.beanToMap(orderV3));
            return R.ok(payResult);
        } catch (com.ruoyi.common.exception.ServiceException e) {
            return R.fail(e.getMessage());
        } catch (WxPayException e) {
            return R.fail(e.getReturnMsg());
        } catch (Exception e) {
            log.error("购买卡下单失败", e);
            return R.fail("下单失败，请稍后再试");
        }
    }


    @ApiOperation("充值余额统一下单，并组装所需支付参数")
    @PostMapping("/createOrder/recharge")
    public R<Object> payRecharge(@RequestBody OrderRechargeBo bo) {
        String orderNo = OrderNumUtils.getOrderNum();
        // 空值防护: 充值金额缺失时直接拦截, 避免 NPE
        if (bo.getAmountTotal() == null || bo.getAmountTotal().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            return R.fail("充值金额不合法");
        }
        WxPayUnifiedOrderV3Request.Payer payer = new WxPayUnifiedOrderV3Request.Payer();
        WxPayUnifiedOrderV3Request.Amount amount = new WxPayUnifiedOrderV3Request.Amount();
        WxPayUnifiedOrderV3Request request = new WxPayUnifiedOrderV3Request();
        bo.setUserId(LoginHelper.getUserId());
        bo.setOrderId(orderNo);
        bo.setPayStatus(PayConstants.UNPAID);
        // 到账额度=实付金额×服务端赠送档位, 禁止信任客户端 rechargeTotal(防止 1 分到账任意额度)
        bo.setRechargeTotal(computeRechargePoints(bo.getAmountTotal()));
        amount.setTotal(bo.getAmountTotal().multiply(Constants.MULTIPLE).intValue());
        request.setPayer(payer.setOpenid(LoginHelper.getWxLoginUser().getOpenid()));
        request.setAmount(amount);
        request.setDescription(bo.getDescription());
        request.setOutTradeNo(bo.getOrderId());
        request.setAttach(Constants.RECHARGE);
        try {
            boolean flag = rechargeService.insertByBo(bo);
            if (!flag){
                return R.fail();
            }
            Object orderV3 = this.payService.createOrderV3(TradeTypeEnum.JSAPI, request);
            Map<String, Object> payResult = new HashMap<>();
            payResult.put("orderId", orderNo);
            payResult.putAll(BeanUtil.beanToMap(orderV3));
            return R.ok(payResult);
        } catch (WxPayException e) {
            return R.fail("发生系统错误，请联系管理员");
        }
    }

    @ApiOperation(value = "支付回调通知处理")
    @PostMapping("/notify/order")
    public String parseOrderNotifyResult(@RequestBody String data) throws WxPayException {
        WxPayOrderNotifyV3Result v3Result = this.payService.parseOrderNotifyV3Result((String) data, null);
        String attach = v3Result.getResult().getAttach();
        String outTradeNo = v3Result.getResult().getOutTradeNo();
        log.info("支付回调通知: attach={}, outTradeNo={}", attach, outTradeNo);
        //优惠卡充值订单回调
        if (Constants.CARD.equals(attach)){
            OrderCardVo vo = orderCardService.queryByOrderId(outTradeNo);
            if (ObjectUtil.isNotNull(vo)){
                OrderCardBo bo = BeanUtil.toBean(vo, OrderCardBo.class);
                bo.setPayStatus(PayConstants.PAID);
                orderCardService.updateByBoAndCard(bo);
            }else {
                return WxPayNotifyResponse.fail("失败");
            }

        }
        //余额充值订单回调
        else if(Constants.RECHARGE.equals(attach)){
            OrderRechargeVo vo = rechargeService.queryByOrderId(outTradeNo);
            if (ObjectUtil.isNotNull(vo)){
                OrderRechargeBo bo = BeanUtil.toBean(vo, OrderRechargeBo.class);
                bo.setPayStatus(PayConstants.PAID);
                rechargeService.updateByBoAndBalance(bo);
            }else {
                return WxPayNotifyResponse.fail("失败");
            }
        }

        return WxPayNotifyResponse.success("成功");
    }

    @ApiOperation("主动查询订单支付结果(本地开发回调兜底)")
    @GetMapping("/query/{orderId}")
    public R<Object> queryOrder(@PathVariable("orderId") String orderId) {
        try {
            String attach = null;
            OrderRechargeVo rechargeVo = rechargeService.queryByOrderId(orderId);
            if (ObjectUtil.isNotNull(rechargeVo)) {
                if (!rechargeVo.getUserId().equals(LoginHelper.getUserId())) {
                    return R.fail("无权查询该订单");
                }
                if (PayConstants.PAID.equals(rechargeVo.getPayStatus())) {
                    return R.ok("已支付");
                }
                attach = Constants.RECHARGE;
            } else {
                OrderCardVo cardVo = orderCardService.queryByOrderId(orderId);
                if (ObjectUtil.isNotNull(cardVo)) {
                    // 防横向越权: 卡订单同样校验归属
                    if (cardVo.getUserId() == null || !cardVo.getUserId().equals(LoginHelper.getUserId())) {
                        return R.fail("无权查询该订单");
                    }
                    if (PayConstants.PAID.equals(cardVo.getPayStatus())) {
                        return R.ok("已支付");
                    }
                    attach = Constants.CARD;
                } else {
                    return R.fail("订单不存在");
                }
            }
            WxPayOrderQueryV3Request queryRequest = new WxPayOrderQueryV3Request();
            queryRequest.setMchid(payService.getConfig().getMchId());
            queryRequest.setOutTradeNo(orderId);
            WxPayOrderQueryV3Result queryResult = payService.queryOrderV3(queryRequest);
            if ("SUCCESS".equals(queryResult.getTradeState())) {
                if (Constants.RECHARGE.equals(attach)) {
                    OrderRechargeBo bo = BeanUtil.toBean(rechargeService.queryByOrderId(orderId), OrderRechargeBo.class);
                    bo.setPayStatus(PayConstants.PAID);
                    rechargeService.updateByBoAndBalance(bo);
                } else {
                    OrderCardBo bo = BeanUtil.toBean(orderCardService.queryByOrderId(orderId), OrderCardBo.class);
                    bo.setPayStatus(PayConstants.PAID);
                    orderCardService.updateByBoAndCard(bo);
                }
                return R.ok("已支付");
            }
            return R.ok("未支付");
        } catch (WxPayException e) {
            log.error("查询订单状态失败, orderId={}", orderId, e);
            return R.fail("查询失败,请稍后重试");
        }
    }


    @ApiOperation("预定座位")
    @PostMapping("/order/seat")
    public R<Void> orderSeat(@RequestBody OrderBo orderBo){
        if (orderBo.getSeatId() == null){
            return R.fail("请选择座位");
        }
        if (orderBo.getStartTime() == null || orderBo.getEndTime() == null){
            return R.fail("请选择正确的预约时间");
        }
        Date startTime = orderBo.getStartTime();
        DateTime now = DateUtil.date();
        //预定时间在当前时间之前，不符合逻辑直接失效
        if (startTime.before(now)){
            return R.fail("请选择正确的时间");
        }
        //插入订单表，支付状态为 未消费
        orderBo.setOrderId(OrderNumUtils.getOrderNum());
        orderBo.setUserId(LoginHelper.getUserId());
        orderBo.setPayStatus(PayConstants.NOT_CONSUMED);
        boolean saved = orderService.insertByBo(orderBo);
        if (saved && orderBo.getSeatId() != null) {
            // 自动打开座位设备
            try {
                miotService.turnOnSeatDevices(orderBo.getSeatId());
                log.info("座位设备已自动开启: seatId={}", orderBo.getSeatId());
            } catch (Exception e) {
                log.error("自动开启设备失败", e);
            }
        }
        return toAjax(saved);
    }



    @ApiOperation("消费座位")
    @PutMapping("/order/seat/consumed")
    public R<Object> orderSeatConsumed(@RequestBody OrderBo orderBo){

        //更新订单表，支付状态为 已消费
        orderBo.setPayStatus(PayConstants.CONSUMED);
        orderBo.setUserId(LoginHelper.getUserId());
        try {
            orderService.updateByBoAndRank(orderBo);
            // 消费完成后自动关闭座位设备
            if (orderBo.getSeatId() != null) {
                try {
                    miotService.turnOffSeatDevices(orderBo.getSeatId());
                    log.info("座位设备已自动关闭: seatId={}", orderBo.getSeatId());
                } catch (Exception e) {
                    log.error("自动关闭设备失败", e);
                }
            }
            return R.ok();
        } catch (Exception e) {
            log.error("座位消费完成处理失败", e);
            return R.fail(e.getMessage());
        }

    }

    /**
     * 充值到账"点"计算(1点=1元): 实付整数元 + 赠送档位, 与前端充值页档位一致
     */
    private int computeRechargePoints(java.math.BigDecimal amount) {
        if (amount == null || amount.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            return 0;
        }
        int amt = amount.setScale(0, java.math.RoundingMode.CEILING).intValue();
        int give;
        if (amt >= 2500) give = 1100;
        else if (amt >= 1250) give = 470;
        else if (amt >= 500) give = 140;
        else if (amt >= 200) give = 40;
        else if (amt >= 80) give = 8;
        else give = 0;
        return amt + give;
    }

}
