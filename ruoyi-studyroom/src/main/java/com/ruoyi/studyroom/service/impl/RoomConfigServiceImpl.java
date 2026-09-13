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
import com.ruoyi.studyroom.domain.bo.RoomConfigBo;
import com.ruoyi.studyroom.domain.vo.RoomConfigVo;
import com.ruoyi.studyroom.domain.RoomConfig;
import com.ruoyi.studyroom.mapper.RoomConfigMapper;
import com.ruoyi.studyroom.service.IRoomConfigService;

import java.util.List;
import java.util.Map;
import java.util.Collection;

/**
 * 自习室配置Service业务层处理
 *
 * @author P_Peaceful
 * @date 2022-05-10
 */
@RequiredArgsConstructor
@Service
public class RoomConfigServiceImpl implements IRoomConfigService {


    private final RoomConfigMapper baseMapper;
    /**
     * 查询自习室配置
     *
     * @param id 自习室配置主键
     * @return 自习室配置
     */
    @Override
    public RoomConfigVo queryById(Long id){
        return baseMapper.selectVoById(id);
    }

    /**
     * 查询自习室配置列表
     *
     * @param bo 自习室配置
     * @return 自习室配置
     */
    @Override
    public TableDataInfo<RoomConfigVo> queryPageList(RoomConfigBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<RoomConfig> lqw = buildQueryWrapper(bo);
        Page<RoomConfigVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询自习室配置列表
     *
     * @param bo 自习室配置
     * @return 自习室配置
     */
    @Override
    public List<RoomConfigVo> queryList(RoomConfigBo bo) {
        LambdaQueryWrapper<RoomConfig> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<RoomConfig> buildQueryWrapper(RoomConfigBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<RoomConfig> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getId() != null, RoomConfig::getId, bo.getId());
        lqw.eq(bo.getRoomId() != null, RoomConfig::getRoomId, bo.getRoomId());
        lqw.eq(StringUtils.isNotBlank(bo.getConfigKey()), RoomConfig::getConfigKey, bo.getConfigKey());
        lqw.like(StringUtils.isNotBlank(bo.getConfigValue()), RoomConfig::getConfigValue, bo.getConfigValue());
        return lqw;
    }

    /**
     * 新增自习室配置
     *
     * @param bo 自习室配置
     * @return 结果
     */
    @Override
    public Boolean insertByBo(RoomConfigBo bo) {
        RoomConfig add = BeanUtil.toBean(bo, RoomConfig.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改自习室配置
     *
     * @param bo 自习室配置
     * @return 结果
     */
    @Override
    public Boolean updateByBo(RoomConfigBo bo) {
        RoomConfig update = BeanUtil.toBean(bo, RoomConfig.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     *
     * @param entity 实体类数据
     */
    private void validEntityBeforeSave(RoomConfig entity){
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除自习室配置
     *
     * @param ids 需要删除的自习室配置主键
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
     * 根据自习室ID和配置键查询配置值
     *
     * @param roomId 自习室ID
     * @param configKey 配置键
     * @return 配置值
     */
    @Override
    public String getConfigValue(Long roomId, String configKey) {
        RoomConfigBo bo = new RoomConfigBo();
        bo.setRoomId(roomId);
        bo.setConfigKey(configKey);
        List<RoomConfigVo> list = queryList(bo);
        if (list != null && !list.isEmpty()) {
            return list.get(0).getConfigValue();
        }
        return null;
    }
}
