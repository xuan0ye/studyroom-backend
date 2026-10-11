package com.ruoyi.studyroom.wx.api;

import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.studyroom.domain.Reserved;
import com.ruoyi.studyroom.domain.bo.*;
import com.ruoyi.studyroom.domain.vo.*;
import com.ruoyi.studyroom.service.*;
import com.ruoyi.system.domain.SysNotice;
import com.ruoyi.system.service.ISysNoticeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * @ProjectName: study-room-vue
 * @Author: WenZhengcheng
 * @Date: 2022-5-9 下午 08:24
 * @Desc:
 */

@Slf4j
@Api(value = "通用资源请求控制器",tags = "通用资源请求")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/wx/common")
public class CommonController {


    private final IRoomService roomService;
    private final ISwiperService swiperService;
    private final IPromoService promoService;
    private final ISeatRecordService seatRecordService;
    private final ISeatService seatService;
    private final ICardService cardService;
    private final ISeatDeviceService seatDeviceService;
    private final IUserService userService;
    private final IRoomConfigService roomConfigService;
    /** 后台「系统管理-通知公告」的数据源(小程序首页公告条用) */
    private final ISysNoticeService noticeService;
    @ApiOperation("门店信息")
    @GetMapping("/store")
    public R<TableDataInfo<RoomVo>> roomList(RoomBo bo, PageQuery pageQuery){

        return R.ok(roomService.queryPageList(bo,pageQuery));
    }


    @ApiOperation("轮播图列表")
    @GetMapping("/swiper")
    public R<List<SwiperVo>> swiper(){
        SwiperBo swiperBo = new SwiperBo();
        swiperBo.setStatus(1);
        return R.ok(swiperService.queryList(swiperBo));
    }


    @ApiOperation("首页热门活动列表(上架中, 按排序)")
    @GetMapping("/promo")
    public R<List<PromoVo>> promo(){
        PromoBo promoBo = new PromoBo();
        promoBo.setStatus(1);
        return R.ok(promoService.queryList(promoBo));
    }


    /**
     * 首页通知公告(后台「系统管理-通知公告」里维护的, 小程序首页公告条用)。
     *
     * <p><b>状态语义注意</b>: {@code sys_notice.status} 是 {@code 0=正常 / 1=关闭}(与直觉相反),
     * 这里只取 {@code "0"}; 否则 RuoYi 自带的演示公告会被推到顾客首页。
     * (部署时已把两条 2018 年的演示公告置为关闭。)</p>
     *
     * <p>返回里同时给富文本 {@code content} 和压成一行的纯文本 {@code contentText}:
     * 首页公告条只显示一行, 点开才用富文本渲染全文。</p>
     *
     * @param limit 最多返回几条, 默认 5
     */
    @ApiOperation("首页通知公告(已发布, 最新的在前)")
    @GetMapping("/notice")
    public R<List<Map<String, Object>>> notice(@RequestParam(required = false) Integer limit) {
        int n = (limit == null || limit <= 0 || limit > 20) ? 5 : limit;
        List<SysNotice> all = noticeService.selectNoticeList(new SysNotice());
        List<Map<String, Object>> out = new ArrayList<>();
        if (all != null) {
            all.stream()
                .filter(x -> x != null && "0".equals(String.valueOf(x.getStatus())))
                .sorted(Comparator.comparing(SysNotice::getCreateTime,
                    Comparator.nullsLast(Comparator.<java.util.Date>reverseOrder())))
                .limit(n)
                .forEach(x -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("noticeId", x.getNoticeId());
                    m.put("title", x.getNoticeTitle());
                    m.put("type", x.getNoticeType());
                    m.put("content", x.getNoticeContent());
                    m.put("contentText", htmlToPlainText(x.getNoticeContent()));
                    m.put("createTime", x.getCreateTime());
                    out.add(m);
                });
        }
        return R.ok(out);
    }

    /**
     * 把公告富文本压成一行纯文本(首页公告条只显示一行; 详情仍用富文本渲染)。
     * 只做最朴素的去标签与实体还原 —— 这里不引入 jsoup, 公告内容是门店后台自己写的。
     */
    private static String htmlToPlainText(String html) {
        if (StringUtils.isBlank(html)) {
            return "";
        }
        String t = html
            .replaceAll("(?is)<(script|style)[^>]*>.*?</\\1>", " ")
            .replaceAll("(?is)<br\\s*/?>", " ")
            .replaceAll("(?is)</(p|div|li|tr|h[1-6])>", " ")
            .replaceAll("(?is)<[^>]+>", "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replaceAll("\\s+", " ")
            .trim();
        return t.length() > 80 ? t.substring(0, 80) + "…" : t;
    }


    @ApiOperation("优惠卡列表")
    @GetMapping("/card")
    public R<List<CardVo>> cardList(){
        CardBo bo = new CardBo();
        bo.setStatus(1);
        return R.ok(cardService.queryList(bo));
    }



    @ApiOperation("座位详情")
    @PostMapping("/seat")
    public R<Reserved> seat(@RequestBody SeatRecordBo bo){
        List<SeatVo> seatVip = new ArrayList<>();
        List<SeatVo> seatCom = new ArrayList<>();
        //查询在开始结束时间段不可预定的座位列表
        List<SeatRecordVo> list = seatRecordService.queryListByTime(bo);
        SeatBo seatBo = new SeatBo();
        seatBo.setRoomId(bo.getRoomId());
        //根据roomId查询所有座位
        List<SeatVo> seatVos = seatService.queryList(seatBo);
        // 只保留已绑定米家开关设备的座位(可控制灯的真实座位), 过滤掉无设备的模板/重复座位
        java.util.Set<Long> boundSeatIds = new java.util.HashSet<>();
        try {
            List<com.ruoyi.studyroom.domain.vo.SeatDeviceVo> devices = seatDeviceService.queryByRoomId(bo.getRoomId());
            for (com.ruoyi.studyroom.domain.vo.SeatDeviceVo dev : devices) {
                if (dev.getSeatId() != null) {
                    boundSeatIds.add(dev.getSeatId());
                }
            }
        } catch (Exception e) {
            log.error("查询座位设备绑定失败, 不过滤座位", e);
        }
        if (!boundSeatIds.isEmpty()) {
            seatVos.removeIf(item -> !boundSeatIds.contains(item.getSeatId()));
        }
        //根据不可预定座位列表更改seatVos中status字段
        // status：true未预定，false 已预定
        // 同时为已占座位填充占用者头像(seat_record.user_id -> sr_user.avatar_url)
        // 隐私: 仅用户设置 showAvatar=1(在预约界面公开头像) 时才返回头像, 否则不返回(前端显示默认占位)
        Map<Long, String> seatAvatarMap = new HashMap<>();
        if (!list.isEmpty()){
            for (SeatRecordVo seatRecordVo : list) {
                if (seatRecordVo.getUserId() != null) {
                    try {
                        UserVo u = userService.queryById(seatRecordVo.getUserId());
                        boolean showAvatar = u != null && (u.getShowAvatar() == null || u.getShowAvatar() == 1);
                        if (u != null && showAvatar && StringUtils.isNotBlank(u.getAvatarUrl())) {
                            seatAvatarMap.put(seatRecordVo.getSeatId(), normalizeAvatar(u.getAvatarUrl()));
                        }
                    } catch (Exception e) {
                        log.warn("查询座位占用者头像失败 seatId={}", seatRecordVo.getSeatId(), e);
                    }
                }
            }
            seatVos.forEach(item -> {
                for (SeatRecordVo seatRecordVo : list) {
                    if (item.getSeatId().equals(seatRecordVo.getSeatId())){
                        item.setStatus(false);
                        item.setOccupantAvatar(seatAvatarMap.get(item.getSeatId()));
                        break;
                    }else{
                        item.setStatus(true);
                    }
                }
                boolean b = Constants.SEAT_COM.equals(item.getSeatArea()) ? seatCom.add(item) : seatVip.add(item);
            });
        }else{
            seatVos.forEach(item -> {
                item.setStatus(true);
                boolean b = Constants.SEAT_COM.equals(item.getSeatArea()) ? seatCom.add(item) : seatVip.add(item);
            });
        }
        return R.ok(Reserved.builder()
                .seatCom(seatCom)
                .seatVip(seatVip)
                .reservedNum(list.size())
                .noReservedNum(seatVos.size() - list.size())
                .build());
    }

    /**
     * 头像值归一化: 数据库可能存裸 base64 或 data URI 截断残留(如 /9j/... 前导斜杠),
     * 统一补 data:image 前缀, 避免前端 image 组件把 base64 当 URL 加载报错
     */
    private static String normalizeAvatar(String avatar) {
        if (avatar == null || avatar.isEmpty()) {
            return "";
        }
        String s = avatar.trim();
        if (s.startsWith("data:image") || s.startsWith("http") || s.startsWith("wxfile")) {
            return s;
        }
        String t = s;
        if (t.startsWith("/")) {
            t = t.substring(1);
        }
        if (t.length() > 50 && t.matches("[A-Za-z0-9+/=]+")) {
            return "data:image/jpeg;base64," + t;
        }
        if (s.startsWith("/")) {
            return s;
        }
        return s;
    }

    /**
     * 座位布局坐标KEY(存 sr_room_config, 所有设备共享, 解决真机/换机坐标丢失问题)
     */
    private static final String SEAT_POSITIONS_KEY = "seatPositions.v3";
    private static final String SEAT_POSITIONS_REMARK = "座位布局图坐标(JSON: 座位号->{x,y}百分比)";

    /**
     * 读取座位布局坐标(未保存时返回空)
     */
    @ApiOperation("读取座位布局坐标")
    @GetMapping("/seatPositions")
    public R<Object> seatPositions(RoomConfigBo bo) {
        Long roomId = bo.getRoomId();
        if (roomId == null) {
            // 默认取第一个自习室
            List<RoomVo> rooms = roomService.queryList(new RoomBo());
            if (rooms != null && !rooms.isEmpty()) {
                roomId = rooms.get(0).getRoomId();
            }
        }
        if (roomId == null) {
            return R.ok(java.util.Collections.emptyMap());
        }
        RoomConfigBo query = new RoomConfigBo();
        query.setRoomId(roomId);
        query.setConfigKey(SEAT_POSITIONS_KEY);
        List<RoomConfigVo> list = roomConfigService.queryList(query);
        if (list != null && !list.isEmpty() && StringUtils.isNotBlank(list.get(0).getConfigValue())) {
            // 返回结构化 data(座位号 -> {x,y}), 勿用 R.ok(String) 以免被当作消息放进 msg
            try {
                cn.hutool.json.JSONObject obj = cn.hutool.json.JSONUtil.parseObj(list.get(0).getConfigValue());
                return R.ok(obj);
            } catch (Exception ex) {
                log.warn("座位坐标JSON解析失败 roomId={}", roomId, ex);
                return R.ok(java.util.Collections.emptyMap());
            }
        }
        return R.ok(java.util.Collections.emptyMap());
    }

    /**
     * 保存座位布局坐标(管理员/编辑布局后同步到服务器, 所有设备一致)
     */
    @ApiOperation("保存座位布局坐标")
    @PostMapping("/seatPositions")
    public R<Void> saveSeatPositions(@RequestBody Map<String, Object> body) {
        if (body == null) {
            return R.fail("参数不能为空");
        }
        Long roomId = body.get("roomId") != null ? Long.valueOf(String.valueOf(body.get("roomId"))) : null;
        Object positions = body.get("positions");
        if (positions == null || StringUtils.isBlank(String.valueOf(positions))) {
            return R.fail("座位坐标不能为空");
        }
        if (roomId == null) {
            List<RoomVo> rooms = roomService.queryList(new RoomBo());
            if (rooms != null && !rooms.isEmpty()) {
                roomId = rooms.get(0).getRoomId();
            }
        }
        if (roomId == null) {
            return R.fail("未找到自习室");
        }
        String json = String.valueOf(positions);
        // JSON 合法性粗校验(必须是对象)
        if (!json.trim().startsWith("{") || !json.trim().endsWith("}")) {
            return R.fail("座位坐标格式不正确");
        }
        RoomConfigBo query = new RoomConfigBo();
        query.setRoomId(roomId);
        query.setConfigKey(SEAT_POSITIONS_KEY);
        List<RoomConfigVo> list = roomConfigService.queryList(query);
        if (list != null && !list.isEmpty()) {
            RoomConfigBo update = new RoomConfigBo();
            update.setId(list.get(0).getId());
            update.setConfigValue(json);
            roomConfigService.updateByBo(update);
        } else {
            RoomConfigBo add = new RoomConfigBo();
            add.setRoomId(roomId);
            add.setConfigKey(SEAT_POSITIONS_KEY);
            add.setConfigValue(json);
            add.setRemark(SEAT_POSITIONS_REMARK);
            roomConfigService.insertByBo(add);
        }
        return R.ok();
    }

}
