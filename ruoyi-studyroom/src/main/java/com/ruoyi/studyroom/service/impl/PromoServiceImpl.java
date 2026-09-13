package com.ruoyi.studyroom.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.domain.PageQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.ruoyi.studyroom.domain.bo.PromoBo;
import com.ruoyi.studyroom.domain.vo.PromoVo;
import com.ruoyi.studyroom.domain.Promo;
import com.ruoyi.studyroom.mapper.PromoMapper;
import com.ruoyi.studyroom.service.IPromoService;

import java.util.List;
import java.util.Map;
import java.util.Collection;

/**
 * 首页热门活动Service业务层处理
 *
 * @author ruoyi
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class PromoServiceImpl implements IPromoService {

    private final PromoMapper baseMapper;

    @Override
    public PromoVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    public TableDataInfo<PromoVo> queryPageList(PromoBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<Promo> lqw = buildQueryWrapper(bo);
        Page<PromoVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    @Override
    public List<PromoVo> queryList(PromoBo bo) {
        LambdaQueryWrapper<Promo> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<Promo> buildQueryWrapper(PromoBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<Promo> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getId() != null, Promo::getId, bo.getId());
        lqw.eq(bo.getStatus() != null, Promo::getStatus, bo.getStatus());
        lqw.like(StringUtils.isNotBlank(bo.getTitle()), Promo::getTitle, bo.getTitle());
        // 首页展示顺序: 排序号小的在前, 其次是创建时间
        lqw.orderByAsc(Promo::getSort).orderByDesc(Promo::getId);
        return lqw;
    }

    @Override
    public Boolean insertByBo(PromoBo bo) {
        // 主键由 MyBatis-Plus 的 ASSIGN_ID 策略自动生成雪花ID(与其他业务表一致)
        Promo add = BeanUtil.toBean(bo, Promo.class);
        add.setId(null);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    @Override
    public Boolean updateByBo(PromoBo bo) {
        Promo update = BeanUtil.toBean(bo, Promo.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(Promo entity) {
        if (StringUtils.isBlank(entity.getTitle())) {
            throw new ServiceException("活动标题不能为空");
        }
        if (entity.getSort() == null) {
            entity.setSort(0);
        }
        if (entity.getStatus() == null) {
            entity.setStatus(1);
        }
    }

    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            // 首页活动为纯展示数据, 无需额外业务校验
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }
}
