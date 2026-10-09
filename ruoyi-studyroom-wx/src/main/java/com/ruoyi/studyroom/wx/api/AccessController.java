package com.ruoyi.studyroom.wx.api;

import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.studyroom.domain.bo.AccessFaceUploadBo;
import com.ruoyi.studyroom.domain.vo.AccessProfileVo;
import com.ruoyi.studyroom.service.IAccessControlService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Api(value = "门禁控制", tags = "门禁控制")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/wx/access")
public class AccessController {

    private final IAccessControlService accessControlService;

    @ApiOperation("上传门禁人脸照片并同步到门禁设备")
    @PostMapping("/profile/upload")
    public R<AccessProfileVo> uploadProfile(@RequestBody AccessFaceUploadBo bo) {
        Long userId = LoginHelper.getUserId();
        String username = LoginHelper.getUsername();
        return R.ok(accessControlService.uploadFaceAndSync(userId, username, bo));
    }

    @ApiOperation("查询当前用户门禁档案")
    @GetMapping("/profile/current")
    public R<AccessProfileVo> currentProfile(@RequestParam(required = false) Long roomId) {
        return R.ok(accessControlService.queryCurrentProfile(LoginHelper.getUserId(), roomId));
    }

    @ApiOperation("远程开门(仅ACTIVE预约用户)")
    @PostMapping("/door/open")
    public R<Boolean> openDoor(@RequestParam(required = false) Long roomId) {
        return R.ok(accessControlService.remoteOpenDoor(LoginHelper.getUserId(), roomId));
    }

    @ApiOperation("一键开门是否可用(前端据此决定是否显示按钮)")
    @GetMapping("/door/available")
    public R<Boolean> doorAvailable() {
        return R.ok(accessControlService.doorOpenAvailable());
    }
}
