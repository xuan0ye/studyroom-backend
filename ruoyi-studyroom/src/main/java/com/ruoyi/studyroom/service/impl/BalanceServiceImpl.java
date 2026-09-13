package com.ruoyi.studyroom.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.domain.PageQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.ruoyi.studyroom.domain.bo.BalanceBo;
import com.ruoyi.studyroom.domain.vo.BalanceVo;
import com.ruoyi.studyroom.domain.Balance;
import com.ruoyi.studyroom.mapper.BalanceMapper;
import com.ruoyi.studyroom.service.IBalanceService;

import java.util.List;
import java.util.Map;
import java.util.Collection;

/**
 * 余额
Service业务层处理
 *
 * @author P_Peaceful
 * @date 2022-05-08
 */
@RequiredArgsConstructor
@Service
public class BalanceServiceImpl implements IBalanceService {

    private final BalanceMapper baseMapper;

    /**
     * 查询余额

     *
     * @param id 余额
主键
     * @return 余额

     */
    @Override
    public BalanceVo queryById(Long id){
        return baseMapper.selectVoById(id);
    }

    /**
     * 查询余额
列表
     *
     * @param bo 余额

     * @return 余额

     */
    @Override
    public TableDataInfo<BalanceVo> queryPageList(BalanceBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<Balance> lqw = buildQueryWrapper(bo);
        Page<BalanceVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询余额
列表
     *
     * @param bo 余额

     * @return 余额

     */
    @Override
    public List<BalanceVo> queryList(BalanceBo bo) {
        LambdaQueryWrapper<Balance> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<Balance> buildQueryWrapper(BalanceBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<Balance> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getUserId() != null, Balance::getUserId, bo.getUserId());
        lqw.eq(bo.getBalance() != null, Balance::getBalance, bo.getBalance());
        return lqw;
    }

    /**
     * 新增余额

     *
     * @param bo 余额

     * @return 结果
     */
    @Override
    public Boolean insertByBo(BalanceBo bo) {
        Balance add = BeanUtil.toBean(bo, Balance.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改余额

     *
     * @param bo 余额

     * @return 结果
     */
    @Override
    public Boolean updateByBo(BalanceBo bo) {
        Balance update = BeanUtil.toBean(bo, Balance.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     *
     * @param entity 实体类数据
     */
    private void validEntityBeforeSave(Balance entity){
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除余额

     *
     * @param ids 需要删除的余额
主键
     * @return 结果
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if(isValid){
            //TODO 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }

    /**
     * 根据用户ID获取余额信息
     *
     * @param userId
     * @return
     */
    @Override
    public BalanceVo queryByUserId(Long userId) {
        LambdaQueryWrapper<Balance> lqw = Wrappers.lambdaQuery();
        lqw.eq(Balance::getUserId, userId);
        return baseMapper.selectVoOne(lqw);
    }

    @Override
    public void deduct(java.math.BigDecimal amount, Long userId) {
        if (amount == null || amount.compareTo(java.math.BigDecimal.ZERO) <= 0 || userId == null) {
            return;
        }
        // 余额单位是整数"点"(1点=1元), 抵扣金额按元转整数点(向上取整, 至少1点)
        long need = amount.setScale(0, java.math.RoundingMode.CEILING).longValue();
        if (need <= 0) {
            return;
        }
        // 原子扣减: UPDATE sr_balance SET balance=balance-? WHERE user_id=? AND balance>=?
        // 影响行数为0 = 余额不足, 不产生任何修改(防并发超扣)
        int rows = baseMapper.deductBalance(userId, need);
        if (rows <= 0) {
            throw new com.ruoyi.common.exception.ServiceException("余额不足");
        }
    }

    @Override
    public void add(Long userId, int points) {
        if (userId == null || points <= 0) {
            return;
        }
        int rows = baseMapper.addBalance(userId, points);
        if (rows <= 0) {
            // 余额账户不存在(首次充值): 创建后再原子加一次, 防并发重复创建
            synchronized (userId.toString().intern()) {
                BalanceVo vo = queryByUserId(userId);
                if (vo == null) {
                    BalanceBo nb = new BalanceBo();
                    nb.setUserId(userId);
                    nb.setBalance(points);
                    nb.setCreateTime(new java.util.Date());
                    insertByBo(nb);
                    return;
                }
                baseMapper.addBalance(userId, points);
            }
        }
    }
}
