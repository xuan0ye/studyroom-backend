package com.ruoyi.studyroom.controller;

import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.studyroom.domain.Feedback;
import com.ruoyi.studyroom.mapper.FeedbackMapper;
import com.ruoyi.studyroom.service.IPointsService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 后台管理: 积分发放 + 意见反馈处理
 */
@Api(value = "积分与反馈管理", tags = "积分与反馈管理")
@RestController
@RequiredArgsConstructor
@RequestMapping("/studyroom/points")
public class PointsAdminController {

    private final IPointsService pointsService;
    private final FeedbackMapper feedbackMapper;
    private final com.ruoyi.studyroom.mapper.RecoverRequestMapper recoverRequestMapper;

    @ApiOperation("客服审核通过后发放积分")
    @SaCheckPermission("studyroom:points:grant")
    @PostMapping("/grant")
    public R<Void> grant(@RequestBody Map<String, Object> body) {
        Long userId = body.get("userId") == null ? null : Long.valueOf(String.valueOf(body.get("userId")));
        Integer points = body.get("points") == null ? null : Integer.valueOf(String.valueOf(body.get("points")));
        String remark = body.get("remark") == null ? null : String.valueOf(body.get("remark"));
        if (userId == null || points == null) {
            return R.fail("参数错误");
        }
        pointsService.grantByAdmin(userId, points, remark, LoginHelper.getUsername());
        return R.ok();
    }

    @ApiOperation("意见反馈列表")
    @SaCheckPermission("studyroom:points:grant")
    @GetMapping("/feedback/list")
    public R<List<Feedback>> feedbackList(@RequestParam(required = false) Integer status) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Feedback> lqw =
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        lqw.eq(status != null, Feedback::getStatus, status)
            .orderByDesc(Feedback::getCreateTime);
        return R.ok(feedbackMapper.selectList(lqw));
    }

    @ApiOperation("处理意见反馈(回复/标记已处理)")
    @SaCheckPermission("studyroom:points:grant")
    @PostMapping("/feedback/handle")
    public R<Void> handleFeedback(@RequestBody Map<String, Object> body) {
        Long id = body.get("id") == null ? null : Long.valueOf(String.valueOf(body.get("id")));
        String reply = body.get("reply") == null ? null : String.valueOf(body.get("reply"));
        if (id == null) {
            return R.fail("参数错误");
        }
        Feedback fb = feedbackMapper.selectById(id);
        if (fb == null) {
            return R.fail("反馈不存在");
        }
        fb.setReply(reply);
        fb.setStatus(1);
        fb.setHandleTime(new Date());
        feedbackMapper.updateById(fb);
        return R.ok();
    }

    @ApiOperation("补签申请列表")
    @SaCheckPermission("studyroom:points:grant")
    @GetMapping("/recover/list")
    public R<List<com.ruoyi.studyroom.domain.RecoverRequest>> recoverList(@RequestParam(required = false) Integer status) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.ruoyi.studyroom.domain.RecoverRequest> lqw =
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        lqw.eq(status != null, com.ruoyi.studyroom.domain.RecoverRequest::getStatus, status)
            .orderByDesc(com.ruoyi.studyroom.domain.RecoverRequest::getCreateTime);
        return R.ok(recoverRequestMapper.selectList(lqw));
    }

    @ApiOperation("审核补签申请(通过后补发积分并恢复连续)")
    @SaCheckPermission("studyroom:points:grant")
    @PostMapping("/recover/handle")
    public R<Void> handleRecover(@RequestBody Map<String, Object> body) {
        Long id = body.get("id") == null ? null : Long.valueOf(String.valueOf(body.get("id")));
        Boolean pass = body.get("pass") == null ? null : Boolean.valueOf(String.valueOf(body.get("pass")));
        String remark = body.get("remark") == null ? null : String.valueOf(body.get("remark"));
        if (id == null || pass == null) {
            return R.fail("参数错误");
        }
        pointsService.handleRecover(id, pass, remark, LoginHelper.getUsername());
        return R.ok();
    }
}
