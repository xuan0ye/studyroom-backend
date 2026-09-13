package com.ruoyi.studyroom.wx.douyin;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.helper.LoginHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 小程序 - 抖音团购自助核销
 *   POST /api/wx/douyin/prepare  验券(输码或短链)
 *   POST /api/wx/douyin/verify   核销(顾客确认)
 *   GET  /api/wx/douyin/records  我的核销记录
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/wx/douyin")
public class WxDouyinController extends BaseController {

    private final DouyinVerifyService douyinVerifyService;

    @PostMapping("/prepare")
    public R<Map<String, Object>> prepare(@RequestBody Map<String, Object> body) {
        Long userId = LoginHelper.getUserId();
        String code = str(body.get("code"));
        String shortLink = str(body.get("shortLink"));
        return R.ok(douyinVerifyService.prepare(userId, code, shortLink));
    }

    @PostMapping("/verify")
    public R<Map<String, Object>> verify(@RequestBody Map<String, Object> body) {
        Long userId = LoginHelper.getUserId();
        String sessionId = str(body.get("sessionId"));
        Long seatId = body.get("seatId") == null ? null : Long.valueOf(String.valueOf(body.get("seatId")));
        return R.ok(douyinVerifyService.verify(userId, sessionId, seatId));
    }

    @GetMapping("/records")
    public R<Map<String, Object>> records(@RequestParam(defaultValue = "1") Integer pageNum,
                                          @RequestParam(defaultValue = "20") Integer pageSize) {
        return R.ok(douyinVerifyService.listRecords(LoginHelper.getUserId(), null, null,
            pageNum == null ? 1 : pageNum, pageSize == null ? 20 : pageSize));
    }

    private String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}
