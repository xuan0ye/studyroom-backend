package com.ruoyi.studyroom.service;

import com.ruoyi.studyroom.domain.vo.PromoVo;
import com.ruoyi.studyroom.domain.bo.PromoBo;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.domain.PageQuery;

import java.util.Collection;
import java.util.List;

/**
 * 首页热门活动Service接口
 *
 * @author ruoyi
 */
public interface IPromoService {

    /**
     * 查询首页热门活动
     */
    PromoVo queryById(Long id);

    /**
     * 查询首页热门活动列表(按排序升序)
     */
    TableDataInfo<PromoVo> queryPageList(PromoBo bo, PageQuery pageQuery);

    /**
     * 查询首页热门活动列表(按排序升序)
     */
    List<PromoVo> queryList(PromoBo bo);

    /**
     * 新增首页热门活动
     */
    Boolean insertByBo(PromoBo bo);

    /**
     * 修改首页热门活动
     */
    Boolean updateByBo(PromoBo bo);

    /**
     * 校验并批量删除首页热门活动信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
