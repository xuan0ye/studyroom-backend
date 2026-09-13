package com.ruoyi.studyroom.ai.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.studyroom.domain.bo.RoomBo;
import com.ruoyi.studyroom.domain.bo.SeatBo;
import com.ruoyi.studyroom.domain.bo.SeatRecordBo;
import com.ruoyi.studyroom.domain.vo.BalanceVo;
import com.ruoyi.studyroom.domain.vo.CardUserVo;
import com.ruoyi.studyroom.domain.vo.RecordVo;
import com.ruoyi.studyroom.domain.vo.RoomVo;
import com.ruoyi.studyroom.domain.vo.SeatRecordVo;
import com.ruoyi.studyroom.domain.vo.SeatVo;
import com.ruoyi.studyroom.service.IBalanceService;
import com.ruoyi.studyroom.service.ICardUserService;
import com.ruoyi.studyroom.service.IRecordService;
import com.ruoyi.studyroom.service.IRoomService;
import com.ruoyi.studyroom.service.ISeatDeviceService;
import com.ruoyi.studyroom.service.ISeatRecordService;
import com.ruoyi.studyroom.service.ISeatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * AI 工具执行器：把模型决策的 tool_call 落到真实业务服务上
 * 工具即接口：模型只做意图识别与参数抽取，数据永远以工具返回为准（可审计、可兜底）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiToolExecutor {

    private final IBalanceService balanceService;
    private final ICardUserService cardUserService;
    private final IRecordService recordService;
    private final IRoomService roomService;
    private final ISeatService seatService;
    private final ISeatRecordService seatRecordService;
    private final ISeatDeviceService seatDeviceService;

    /**
     * 工具定义（OpenAI function calling 格式）
     */
    public String toolDefinitionsJson() {
        return JSONUtil.toJsonStr(new Object[]{
            tool("query_balance", "查询当前登录用户的账户余额与持有的优惠卡情况（日卡/月卡/季卡/年卡）", "{}"),
            tool("query_available_seats",
                "查询自习室门店的座位实时可用情况，可传入 roomId（门店ID）、startTime/endTime（格式 HH:mm，不传则默认当前时间）",
                "{\"type\":\"object\",\"properties\":{\"roomId\":{\"type\":\"integer\",\"description\":\"门店ID，可选\"},\"startTime\":{\"type\":\"string\",\"description\":\"开始时间 HH:mm\"},\"endTime\":{\"type\":\"string\",\"description\":\"结束时间 HH:mm\"}},\"required\":[]}"),
            tool("query_learn_hours", "查询当前登录用户的累计学习时长与学习时长排名", "{}"),
            tool("query_store_info", "查询自习室门店信息：店名、地址、联系电话", "{}")
        });
    }

    private JSONObject tool(String name, String description, String params) {
        return JSONUtil.createObj()
            .set("type", "function")
            .set("function", JSONUtil.createObj()
                .set("name", name)
                .set("description", description)
                .set("parameters", JSONUtil.parseObj(params)));
    }

    /**
     * 是否内置工具
     */
    public boolean isBuiltin(String name) {
        return "query_balance".equals(name) || "query_available_seats".equals(name)
            || "query_learn_hours".equals(name) || "query_store_info".equals(name);
    }

    /**
     * 执行工具，返回 JSON 字符串结果
     */
    public String execute(String name, String argumentsJson) {
        JSONObject args = StrUtil.isBlank(argumentsJson) ? new JSONObject() : JSONUtil.parseObj(argumentsJson);
        Long userId = LoginHelper.getUserId();
        switch (name) {
            case "query_balance":
                return queryBalance(userId);
            case "query_available_seats":
                return querySeats(args);
            case "query_learn_hours":
                return queryLearnHours(userId);
            case "query_store_info":
                return queryStore();
            default:
                return JSONUtil.createObj().set("error", "未知工具: " + name).toString();
        }
    }

    private String queryBalance(Long userId) {
        BalanceVo balance = balanceService.queryByUserId(userId);
        List<CardUserVo> cards = cardUserService.queryListByUserId(userId);
        JSONObject cardsJson = new JSONObject();
        if (cards != null) {
            for (CardUserVo c : cards) {
                String cardName = c.getCard() == null ? "未知卡" : c.getCard().getCardTitle();
                cardsJson.set(cardName, c.getExpiryTime());
            }
        }
        return JSONUtil.createObj()
            .set("balance", balance == null ? 0 : balance.getBalance())
            .set("cards", cardsJson)
            .toString();
    }

    private String querySeats(JSONObject args) {
        // 强制使用答岸自习室门店ID(模型无法得知真实ID, 忽略其传入的roomId)
        Long roomId = 1523324098582265857L;
        String startTime = args.getStr("startTime");
        String endTime = args.getStr("endTime");
        SeatBo seatBo = new SeatBo();
        seatBo.setRoomId(roomId);
        List<SeatVo> all = seatService.queryList(seatBo);
        // 只保留绑定设备的座位(真实可预约座位, 答岸自习室共22个: 轻音1-12 + 静音13-22)
        if (all != null && !all.isEmpty()) {
            try {
                List<com.ruoyi.studyroom.domain.vo.SeatDeviceVo> devices = seatDeviceService.queryByRoomId(roomId);
                java.util.Set<Long> boundIds = new java.util.HashSet<>();
                if (devices != null) {
                    for (com.ruoyi.studyroom.domain.vo.SeatDeviceVo d : devices) {
                        if (d.getSeatId() != null) boundIds.add(d.getSeatId());
                    }
                }
                if (!boundIds.isEmpty()) {
                    all.removeIf(s -> !boundIds.contains(s.getSeatId()));
                }
            } catch (Exception e) {
                log.warn("querySeats 过滤绑定座位失败: {}", e.getMessage());
            }
        }
        // 时间段内已被占用的座位
        SeatRecordBo recordBo = new SeatRecordBo();
        recordBo.setRoomId(roomId);
        Date now = new Date();
        if (StrUtil.isNotBlank(startTime)) {
            recordBo.setStartTime(parseTime(startTime));
        } else {
            recordBo.setStartTime(now);
        }
        if (StrUtil.isNotBlank(endTime)) {
            recordBo.setEndTime(parseTime(endTime));
        } else {
            // 未指定结束时间时默认未来 2 小时
            recordBo.setEndTime(new Date(now.getTime() + 2L * 60 * 60 * 1000));
        }
        List<SeatRecordVo> occupied = seatRecordService.queryListByTime(recordBo);
        // 占用数只统计绑定座位范围内的
        int occupiedCount = 0;
        if (occupied != null && all != null) {
            java.util.Set<Long> seatIds = new java.util.HashSet<>();
            for (SeatVo s : all) seatIds.add(s.getSeatId());
            for (SeatRecordVo r : occupied) {
                if (seatIds.contains(r.getSeatId())) occupiedCount++;
            }
        }
        int vip = 0;
        int common = 0;
        int total = all == null ? 0 : all.size();
        for (SeatVo s : all == null ? java.util.Collections.<SeatVo>emptyList() : all) {
            if (Constants.SEAT_COM.equals(s.getSeatArea())) {
                common++;
            } else {
                vip++;
            }
        }
        JSONObject result = JSONUtil.createObj()
            .set("roomId", roomId)
            .set("totalSeats", total)
            .set("vipSeats", vip)
            .set("commonSeats", common)
            .set("occupiedCount", occupiedCount)
            .set("availableCount", total - occupiedCount);
        if (StrUtil.isNotBlank(startTime)) {
            result.set("startTime", startTime);
        }
        if (StrUtil.isNotBlank(endTime)) {
            result.set("endTime", endTime);
        }
        return result.toString();
    }

    private Date parseTime(String hhmm) {
        try {
            String[] parts = hhmm.trim().split(":");
            int h = Integer.parseInt(parts[0]);
            int m = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            Date date = new Date();
            date.setHours(h);
            date.setMinutes(m);
            date.setSeconds(0);
            return date;
        } catch (Exception e) {
            return new Date();
        }
    }

    private String queryLearnHours(Long userId) {
        RecordVo record = recordService.queryByUserId(userId);
        List<RecordVo> ranks = recordService.queryDescList();
        int rank = 1;
        if (ranks != null) {
            for (RecordVo r : ranks) {
                if (r.getUserId() != null && r.getUserId().equals(userId)) {
                    break;
                }
                rank++;
            }
        }
        return JSONUtil.createObj()
            .set("hours", record == null || record.getHours() == null ? 0 : record.getHours())
            .set("rank", ranks == null || ranks.isEmpty() ? 0 : rank)
            .set("totalUsers", ranks == null ? 0 : ranks.size())
            .toString();
    }

    private String queryStore() {
        List<RoomVo> rooms = roomService.queryList(new RoomBo());
        JSONObject result = new JSONObject();
        int idx = 1;
        if (rooms != null) {
            for (RoomVo r : rooms) {
                result.set("store" + idx, JSONUtil.createObj()
                    .set("name", r.getRoomName())
                    .set("address", r.getRoomAddr())
                    .set("phone", r.getPhone()));
                idx++;
            }
        }
        return result.toString();
    }

}