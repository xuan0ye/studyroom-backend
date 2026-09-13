package com.ruoyi.studyroom.wx.api;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ObjectUtil;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.constant.PayConstants;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.redis.RedisUtils;
import com.ruoyi.studyroom.domain.bo.OrderBo;
import com.ruoyi.studyroom.domain.bo.OrderRechargeBo;
import com.ruoyi.studyroom.domain.bo.RecordBo;
import com.ruoyi.studyroom.domain.bo.UserBo;
import com.ruoyi.studyroom.domain.vo.*;
import com.ruoyi.studyroom.service.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @ProjectName: study-room-vue
 * @Author: WenZhengcheng
 * @Date: 2022-5-9 下午 08:15
 * @Desc:
 */

@Api(value = "微信用户信息管理器", tags = "微信用户信息")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/wx/user")
public class WxUserContrller extends BaseController {

    private final IUserService userService;
    private final ICardUserService cardService;
    private final IBalanceService balanceService;
    private final IOrderService orderService;
    private final IOrderRechargeService rechargeService;
    private final IRecordService recordService;
    private final IDayCardConsumptionService dayCardConsumptionService;



    @ApiOperation("获取用户信息")
    @GetMapping
    public R<UserVo> user() {
        UserVo userVo = userService.queryById(LoginHelper.getLoginUser().getUserId());
        if (userVo != null) {
            userVo.setAvatarUrl(normalizeAvatar(userVo.getAvatarUrl()));
        }
        return R.ok(userVo);
    }

    /**
     * 头像值归一化: 数据库可能存裸 base64 或 data URI 截断残留(如 /9j/... 前导斜杠),
     * 前端 image 组件需要 data:image 前缀, 否则被当成本地路径加载报 "Failed to load local image resource"
     */
    private String normalizeAvatar(String avatar) {
        if (avatar == null || avatar.isEmpty()) {
            return "";
        }
        String s = avatar.trim();
        if (s.startsWith("data:image") || s.startsWith("http") || s.startsWith("wxfile")) {
            return s;
        }
        // 兼容 data URI 截断残留: 以 / 开头但去掉斜杠后仍是 base64(如 /9j/4AAQ...), 补前缀
        String t = s;
        if (t.startsWith("/")) {
            t = t.substring(1);
        }
        if (t.length() > 50 && t.matches("[A-Za-z0-9+/=]+")) {
            return "data:image/jpeg;base64," + t;
        }
        // 以 / 开头的本地资源路径原样返回
        if (s.startsWith("/")) {
            return s;
        }
        return s;
    }

    @ApiOperation("更新用户信息")
    @PutMapping
    public R<Void> user(@RequestBody UserBo bo) {
        // 强制使用当前登录用户ID, 防止前端篡改他人资料
        if (bo == null) {
            return R.fail("参数不能为空");
        }
        bo.setUserId(LoginHelper.getUserId());
        return toAjax(userService.updateByBo(bo));
    }

    @ApiOperation("上传用户头像(Base64)")
    @PostMapping("/avatar")
    public R<Map<String, String>> uploadAvatar(@RequestBody UserBo bo) {
        if (bo == null || StringUtils.isBlank(bo.getAvatarUrl())) {
            return R.fail("请上传头像图片");
        }
        String avatar = bo.getAvatarUrl();
        int idx = avatar.indexOf("base64,");
        if (idx > -1) {
            avatar = avatar.substring(idx + 7);
        }
        if (avatar.length() > 500000) {
            return R.fail("头像图片过大，请压缩后重试");
        }
        // base64 解码并保存为本地文件(小程序 image 加载 URL 比超长 base64 data URI 更可靠)
        String url = saveAvatarFile(avatar);
        if (url == null) {
            return R.fail("头像图片数据不合法");
        }
        UserBo update = new UserBo();
        update.setUserId(LoginHelper.getUserId());
        update.setAvatarUrl(url);
        boolean ok = userService.updateByBo(update);
        if (!ok) {
            return R.fail("头像保存失败");
        }
        Map<String, String> map = new HashMap<>(2);
        map.put("url", url);
        return R.ok(map);
    }

    /**
     * 头像 base64 保存为本地文件, 返回可访问 URL(/upload/avatar/xxx.jpg)
     */
    private String saveAvatarFile(String base64Data) {
        try {
            byte[] bytes = cn.hutool.core.codec.Base64.decode(base64Data);
            if (bytes.length < 100 || bytes.length > 1024 * 1024) {
                return null;
            }
            // 校验图片头(JPEG/PNG/GIF)
            boolean isJpeg = bytes.length > 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8;
            boolean isPng = bytes.length > 8 && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G';
            boolean isGif = bytes.length > 6 && bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F';
            if (!isJpeg && !isPng && !isGif) {
                return null;
            }
            String suffix = isPng ? ".png" : (isGif ? ".gif" : ".jpg");
            String dir = System.getProperty("user.dir") + File.separator + "upload" + File.separator + "avatar";
            File dirFile = new File(dir);
            if (!dirFile.exists() && !dirFile.mkdirs()) {
                return null;
            }
            String fileName = "avatar_" + System.currentTimeMillis() + "_" + cn.hutool.core.util.IdUtil.fastSimpleUUID() + suffix;
            File target = new File(dirFile, fileName);
            cn.hutool.core.io.FileUtil.writeBytes(bytes, target);
            return "/upload/avatar/" + fileName;
        } catch (Exception e) {
            return null;
        }
    }

    @ApiOperation("获取用户拥有的的优惠卡")
    @GetMapping("/card")
    public R<Object> card() {

        return R.ok(cardService.queryListByUserId(LoginHelper.getLoginUser().getUserId()));
    }


    @ApiOperation("获取用户余额信息")
    @GetMapping("/balance")
    public R<BalanceVo> balance() {
        return R.ok(balanceService.queryByUserId(LoginHelper.getUserId()));
    }

    @ApiOperation("获取用户订单")
    @GetMapping("/order")
    public R<TableDataInfo<OrderVo>> order(OrderBo bo, PageQuery pageQuery) {
        bo.setUserId(LoginHelper.getUserId());
        return R.ok(orderService.queryPageList(bo, pageQuery));
    }

    @ApiOperation("获取用户已消费订单")
    @GetMapping("/order/paid")
    public R<TableDataInfo<OrderVo>> consumeOrder(OrderBo bo, PageQuery pageQuery) {
        bo.setUserId(LoginHelper.getUserId());
        bo.setPayStatus(PayConstants.CONSUMED);
        return R.ok(orderService.queryPageList(bo, pageQuery));
    }


    @ApiOperation("获取用户充值订单记录")
    @PostMapping("/recharge")
    public R<TableDataInfo<OrderRechargeVo>> recharge(OrderRechargeBo bo, PageQuery pageQuery) {
        bo.setUserId(LoginHelper.getUserId());
        return R.ok(rechargeService.queryPageList(bo, pageQuery));
    }

    @ApiOperation("更新学习记录排行榜")
    @PostMapping("/record")
    @Deprecated
    public R<Void> record(Double hours) {
        RecordVo vo = recordService.queryByUserId(LoginHelper.getUserId());

        RecordBo bo = BeanUtil.toBean(vo, RecordBo.class);
        bo.setHours(bo.getHours() + hours);
        return toAjax(recordService.updateByBo(bo));

    }

    @ApiOperation("获取学习记录排行榜")
    @GetMapping("/record")
    public R<List<RecordVo>> record() {
        if (!RedisUtils.hasKey(Constants.RECORD_RANK)) {
            List<RecordVo> list = recordService.queryDescList();
            RedisUtils.setCacheList(Constants.RECORD_RANK, list);
            RedisUtils.expire(Constants.RECORD_RANK,Constants.TIMEOUT);
            return R.ok(list);
        }
       return R.ok(RedisUtils.getCacheList(Constants.RECORD_RANK));
    }

    @ApiOperation("连续签到排行榜(按连续签到天数)")
    @GetMapping("/record/streak")
    public R<List<java.util.Map<String, Object>>> streakRank() {
        return R.ok(dayCardConsumptionService.queryStreakRank());
    }

    @ApiOperation("今日早到排行榜(按当天首次开灯时间)")
    @GetMapping("/record/early")
    public R<List<java.util.Map<String, Object>>> earlyRank() {
        return R.ok(dayCardConsumptionService.queryEarlyRank());
    }

    @ApiOperation("获取用户自己的学习记录排行榜")
    @GetMapping("/record/mine")
    public R<RecordVo> recordByUserId() {

        return R.ok(recordService.queryByUserId(LoginHelper.getUserId()));
    }
}
