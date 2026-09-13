package com.ruoyi.studyroom.controller;

import java.util.List;
import java.util.Arrays;

import lombok.RequiredArgsConstructor;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.*;
import cn.dev33.satoken.annotation.SaCheckPermission;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import com.ruoyi.common.core.validate.QueryGroup;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.studyroom.domain.vo.PromoVo;
import com.ruoyi.studyroom.domain.bo.PromoBo;
import com.ruoyi.studyroom.service.IPromoService;
import com.ruoyi.common.core.page.TableDataInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiOperation;

/**
 * 首页热门活动Controller
 *
 * @author ruoyi
 */
@Validated
@Api(value = "首页热门活动控制器", tags = {"首页热门活动管理"})
@RequiredArgsConstructor
@RestController
@RequestMapping("/store/promo")
public class PromoController extends BaseController {

    private final IPromoService iPromoService;

    /**
     * 查询首页热门活动列表
     */
    @ApiOperation("查询首页热门活动列表")
    @SaCheckPermission("store:promo:list")
    @GetMapping("/list")
    public TableDataInfo<PromoVo> list(@Validated(QueryGroup.class) PromoBo bo, PageQuery pageQuery) {
        return iPromoService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出首页热门活动列表
     */
    @ApiOperation("导出首页热门活动列表")
    @SaCheckPermission("store:promo:export")
    @Log(title = "首页热门活动", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(@Validated PromoBo bo, HttpServletResponse response) {
        List<PromoVo> list = iPromoService.queryList(bo);
        ExcelUtil.exportExcel(list, "首页热门活动", PromoVo.class, response);
    }

    /**
     * 获取首页热门活动详细信息
     */
    @ApiOperation("获取首页热门活动详细信息")
    @SaCheckPermission("store:promo:query")
    @GetMapping("/{id}")
    public R<PromoVo> getInfo(@ApiParam("主键")
                              @NotNull(message = "主键不能为空")
                              @PathVariable("id") Long id) {
        return R.ok(iPromoService.queryById(id));
    }

    /**
     * 新增首页热门活动
     */
    @ApiOperation("新增首页热门活动")
    @SaCheckPermission("store:promo:add")
    @Log(title = "首页热门活动", businessType = BusinessType.INSERT)
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody PromoBo bo) {
        return toAjax(iPromoService.insertByBo(bo));
    }

    /**
     * 修改首页热门活动
     */
    @ApiOperation("修改首页热门活动")
    @SaCheckPermission("store:promo:edit")
    @Log(title = "首页热门活动", businessType = BusinessType.UPDATE)
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody PromoBo bo) {
        return toAjax(iPromoService.updateByBo(bo));
    }

    /**
     * 删除首页热门活动
     */
    @ApiOperation("删除首页热门活动")
    @SaCheckPermission("store:promo:remove")
    @Log(title = "首页热门活动", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@ApiParam("主键串")
                          @NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(iPromoService.deleteWithValidByIds(Arrays.asList(ids), true));
    }
}
