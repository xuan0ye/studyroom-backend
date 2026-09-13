package com.ruoyi.studyroom.wx.api;

import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.studyroom.domain.Feedback;
import com.ruoyi.studyroom.domain.vo.PointsLogVo;
import com.ruoyi.studyroom.domain.vo.PointsVo;
import com.ruoyi.studyroom.mapper.FeedbackMapper;
import com.ruoyi.studyroom.service.IPointsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 微信端: 积分 + 意见反馈
 */
@Slf4j
@Api(value = "积分与反馈", tags = "积分与反馈")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/wx/points")
public class WxPointsFeedbackController {

    private final IPointsService pointsService;
    private final FeedbackMapper feedbackMapper;
    private final com.ruoyi.studyroom.mapper.RecoverRequestMapper recoverRequestMapper;

    @ApiOperation("查询我的积分")
    @GetMapping("/mine")
    public R<Map<String, Object>> mine() {
        Long userId = LoginHelper.getUserId();
        PointsVo vo = pointsService.getByUserId(userId);
        List<PointsLogVo> logs = pointsService.listLogs(userId, 20);
        Map<String, Object> map = new HashMap<>();
        map.put("points", vo.getPoints());
        map.put("totalEarned", vo.getTotalEarned());
        map.put("totalSpent", vo.getTotalSpent());
        map.put("logs", logs);
        return R.ok(map);
    }

    @ApiOperation("积分商城兑换日卡(3000积分=1张)")
    @PostMapping("/exchange/daycard")
    public R<Map<String, Object>> exchangeDayCard() {
        Long newCardId = pointsService.exchangeDayCard(LoginHelper.getUserId());
        Map<String, Object> map = new HashMap<>();
        map.put("cardUserId", newCardId);
        map.put("cost", 3000);
        return R.ok(map);
    }

    @ApiOperation("提交补签申请(断签好评截图)")
    @PostMapping("/recover/submit")
    public R<Long> submitRecover(@RequestBody Map<String, String> body) {
        Long id = pointsService.submitRecover(LoginHelper.getUserId(),
            body.get("missedDate"), body.get("reason"), body.get("contact"));
        return R.ok(id);
    }

    @ApiOperation("我的补签申请列表")
    @GetMapping("/recover/list")
    public R<List<com.ruoyi.studyroom.domain.RecoverRequest>> myRecover() {
        return R.ok(recoverRequestMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.ruoyi.studyroom.domain.RecoverRequest>()
                .eq(com.ruoyi.studyroom.domain.RecoverRequest::getUserId, LoginHelper.getUserId())
                .orderByDesc(com.ruoyi.studyroom.domain.RecoverRequest::getCreateTime)
                .last("limit 20")));
    }

    @ApiOperation("提交意见反馈")
    @PostMapping("/feedback")
    public R<Boolean> feedback(@RequestBody Map<String, String> body) {
        String content = body.get("content");
        String contact = body.get("contact");
        if (content == null || content.trim().isEmpty()) {
            return R.fail("反馈内容不能为空");
        }
        Feedback fb = new Feedback();
        fb.setUserId(LoginHelper.getUserId());
        fb.setUserName(LoginHelper.getUsername());
        fb.setContent(content.trim());
        fb.setContact(contact);
        fb.setStatus(0);
        fb.setCreateTime(new Date());
        return R.ok(feedbackMapper.insert(fb) > 0);
    }

    @ApiOperation("我的反馈列表")
    @GetMapping("/feedback/list")
    public R<List<Feedback>> myFeedback() {
        return R.ok(feedbackMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Feedback>()
                .eq(Feedback::getUserId, LoginHelper.getUserId())
                .orderByDesc(Feedback::getCreateTime)
                .last("limit 20")));
    }
}
