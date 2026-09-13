package com.ruoyi.studyroom.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.ruoyi.common.constant.PayConstants;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.domain.PageQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.studyroom.domain.bo.BalanceBo;
import com.ruoyi.studyroom.domain.vo.BalanceVo;
import com.ruoyi.studyroom.service.IBalanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.ruoyi.studyroom.domain.bo.OrderRechargeBo;
import com.ruoyi.studyroom.domain.vo.OrderRechargeVo;
import com.ruoyi.studyroom.domain.OrderRecharge;
import com.ruoyi.studyroom.mapper.OrderRechargeMapper;
import com.ruoyi.studyroom.service.IOrderRechargeService;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Collection;

/**
 * 充值订单Service业务层处理
 *
 * @author P_Peaceful
 * @date 2022-05-08
 */
@RequiredArgsConstructor
@Service
public class OrderRechargeServiceImpl implements IOrderRechargeService {

    private final OrderRechargeMapper baseMapper;
    private final IBalanceService balanceService;
    private final com.ruoyi.studyroom.mapper.UserMapper userMapper;

    /**
     * 补全关联展示字段(用户昵称/手机号)
     */
    private void fillDisplayFields(OrderRechargeVo vo) {
        if (vo == null || vo.getUserId() == null) {
            return;
        }
        com.ruoyi.studyroom.domain.User user = userMapper.selectById(vo.getUserId());
        if (user != null) {
            vo.setUserNickname(user.getNickname());
            vo.setUserPhone(user.getPhone());
        }
    }

    /**
     * 查询充值订单
     *
     * @param id 充值订单主键
     * @return 充值订单
     */
    @Override
    public OrderRechargeVo queryById(Long id){
        return baseMapper.selectVoById(id);
    }

    /**
     * 查询充值订单列表
     *
     * @param bo 充值订单
     * @return 充值订单
     */
    @Override
    public TableDataInfo<OrderRechargeVo> queryPageList(OrderRechargeBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<OrderRecharge> lqw = buildQueryWrapper(bo);
        Page<OrderRechargeVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        if (result.getRecords() != null) {
            result.getRecords().forEach(this::fillDisplayFields);
        }
        return TableDataInfo.build(result);
    }

    /**
     * 查询充值订单列表
     *
     * @param bo 充值订单
     * @return 充值订单
     */
    @Override
    public List<OrderRechargeVo> queryList(OrderRechargeBo bo) {
        LambdaQueryWrapper<OrderRecharge> lqw = buildQueryWrapper(bo);
        List<OrderRechargeVo> list = baseMapper.selectVoList(lqw);
        if (list != null) {
            list.forEach(this::fillDisplayFields);
        }
        return list;
    }

    private LambdaQueryWrapper<OrderRecharge> buildQueryWrapper(OrderRechargeBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<OrderRecharge> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getOrderId() != null, OrderRecharge::getOrderId, bo.getOrderId());
        lqw.eq(bo.getUserId() != null, OrderRecharge::getUserId, bo.getUserId());
        lqw.eq(StringUtils.isNotBlank(bo.getDescription()), OrderRecharge::getDescription, bo.getDescription());
        lqw.eq(bo.getAmountTotal() != null, OrderRecharge::getAmountTotal, bo.getAmountTotal());
        lqw.eq(bo.getRechargeTotal() != null, OrderRecharge::getRechargeTotal, bo.getRechargeTotal());
        lqw.eq(bo.getPayStatus() != null, OrderRecharge::getPayStatus, bo.getPayStatus());
        lqw.orderByDesc(OrderRecharge::getCreateTime);
        lqw.orderByAsc(OrderRecharge::getPayStatus);
        return lqw;
    }

    /**
     * 新增充值订单
     *
     * @param bo 充值订单
     * @return 结果
     */
    @Override
    public Boolean insertByBo(OrderRechargeBo bo) {
        OrderRecharge add = BeanUtil.toBean(bo, OrderRecharge.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改充值订单
     *
     * @param bo 充值订单
     * @return 结果
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean updateByBo(OrderRechargeBo bo) {
        if (PayConstants.CONSUMED.equals(bo.getPayStatus())){
            BalanceVo balanceVo = balanceService.queryByUserId(bo.getUserId());
            BalanceBo balanceBo = BeanUtil.toBean(balanceVo, BalanceBo.class);
            balanceBo.setBalance(Math.toIntExact(balanceVo.getBalance() + bo.getRechargeTotal()));
            balanceService.updateByBo(balanceBo);
        }
        OrderRecharge update = BeanUtil.toBean(bo, OrderRecharge.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 修改充值订单，同步修改余额表
     *
     * @param bo
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean updateByBoAndBalance(OrderRechargeBo bo) {
        if (bo.getOrderId() == null || bo.getUserId() == null) {
            return false;
        }
        // 幂等闸门: 仅当订单从未支付(UNPAID=0)时置为已支付并入账;
        // 重复回调/轮询/并发已处理过则直接返回成功, 杜绝重复入账
        int rows = baseMapper.update(null, Wrappers.<OrderRecharge>lambdaUpdate()
            .eq(OrderRecharge::getOrderId, bo.getOrderId())
            .eq(OrderRecharge::getPayStatus, PayConstants.UNPAID)
            .set(OrderRecharge::getPayStatus, PayConstants.PAID));
        if (rows <= 0) {
            OrderRechargeVo exist = queryByOrderId(bo.getOrderId());
            return exist != null && PayConstants.PAID.equals(exist.getPayStatus());
        }
        // 首次入账: 原子增加余额(到账点数以创建订单时服务端计算的 rechargeTotal 为准)
        int points = bo.getRechargeTotal() == null ? 0 : bo.getRechargeTotal();
        if (points > 0) {
            balanceService.add(bo.getUserId(), points);
        }
        return true;
    }

    /**
     * 保存前的数据校验
     *
     * @param entity 实体类数据
     */
    private void validEntityBeforeSave(OrderRecharge entity){
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除充值订单
     *
     * @param ids 需要删除的充值订单主键
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
     * 根据订单ID查询充值订单
     *
     * @param outTradeNo
     * @return
     */
    @Override
    public OrderRechargeVo queryByOrderId(String outTradeNo) {
        LambdaQueryWrapper<OrderRecharge> lqw = Wrappers.lambdaQuery();
        lqw.eq(OrderRecharge::getOrderId, outTradeNo);
        return baseMapper.selectVoOne(lqw);
    }
}
