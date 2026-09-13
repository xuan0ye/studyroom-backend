package com.ruoyi.studyroom.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.studyroom.domain.AccessProfile;
import com.ruoyi.studyroom.mapper.AccessProfileMapper;
import com.ruoyi.studyroom.mapper.SeatMapper;
import com.ruoyi.studyroom.mapper.UserMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 门禁用户档案管理
 *
 * <p>后台查看门禁档案: 门禁名(userName), 关联用户, 座位, 设备, 有效性, 同步/撤销状态</p>
 *
 * @author studyroom
 */
@Validated
@Api(value = "门禁用户档案控制器", tags = {"门禁用户档案管理"})
@RequiredArgsConstructor
@RestController
@RequestMapping("/studyroom/accessProfile")
public class AccessProfileController extends BaseController {

    private final AccessProfileMapper accessProfileMapper;
    private final UserMapper userMapper;
    private final SeatMapper seatMapper;

    /**
     * 查询门禁档案列表
     */
    @ApiOperation("查询门禁档案列表")
    @SaCheckPermission("studyroom:accessProfile:list")
    @GetMapping("/list")
    public TableDataInfo<AccessProfile> list(com.ruoyi.studyroom.domain.bo.AccessProfileBo bo, PageQuery pageQuery) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AccessProfile> lqw =
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        lqw.eq(bo.getUserId() != null, AccessProfile::getUserId, bo.getUserId())
            .eq(bo.getRoomId() != null, AccessProfile::getRoomId, bo.getRoomId())
            .eq(bo.getSeatId() != null, AccessProfile::getSeatId, bo.getSeatId())
            .eq(bo.getSyncStatus() != null, AccessProfile::getSyncStatus, bo.getSyncStatus())
            .eq(bo.getRevokeStatus() != null, AccessProfile::getRevokeStatus, bo.getRevokeStatus())
            .like(bo.getUserName() != null && !bo.getUserName().isEmpty(), AccessProfile::getUserName, bo.getUserName())
            .orderByDesc(AccessProfile::getUpdateTime);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<AccessProfile> result =
            accessProfileMapper.selectPage(pageQuery.build(), lqw);
        // 补全关联展示字段: 用户昵称/手机号, 座位号, 区域名
        List<AccessProfile> records = result.getRecords();
        if (records != null && !records.isEmpty()) {
            Map<Long, com.ruoyi.studyroom.domain.User> userMap = records.stream()
                .filter(r -> r.getUserId() != null)
                .map(r -> userMapper.selectById(r.getUserId()))
                .filter(u -> u != null)
                .collect(Collectors.toMap(com.ruoyi.studyroom.domain.User::getUserId, Function.identity(), (a, b) -> a));
            Map<Long, com.ruoyi.studyroom.domain.Seat> seatMap = records.stream()
                .filter(r -> r.getSeatId() != null)
                .map(r -> seatMapper.selectById(r.getSeatId()))
                .filter(s -> s != null)
                .collect(Collectors.toMap(com.ruoyi.studyroom.domain.Seat::getSeatId, Function.identity(), (a, b) -> a));
            records.forEach(r -> {
                com.ruoyi.studyroom.domain.User u = r.getUserId() == null ? null : userMap.get(r.getUserId());
                if (u != null) {
                    r.setNickname(u.getNickname());
                    r.setUserPhone(u.getPhone());
                }
                com.ruoyi.studyroom.domain.Seat s = r.getSeatId() == null ? null : seatMap.get(r.getSeatId());
                if (s != null) {
                    r.setSeatNum(s.getSeatNum());
                    r.setAreaName(s.getAreaName());
                }
            });
        }
        return TableDataInfo.build(result);
    }

    /**
     * 查询门禁档案总数(概览用)
     */
    @ApiOperation("查询门禁档案列表(不分页)")
    @SaCheckPermission("studyroom:accessProfile:list")
    @GetMapping("/all")
    public R<List<AccessProfile>> all() {
        return R.ok(accessProfileMapper.selectList(null));
    }
}
