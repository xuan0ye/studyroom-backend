package com.ruoyi.studyroom.service;

import com.ruoyi.studyroom.domain.RoomConfig;
import com.ruoyi.studyroom.domain.vo.RoomConfigVo;
import com.ruoyi.studyroom.domain.bo.RoomConfigBo;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.domain.PageQuery;

import java.util.Collection;
import java.util.List;

/**
 * 自习室配置Service接口
 *
 * @author P_Peaceful
 * @date 2022-05-10
 */
public interface IRoomConfigService {

    /**
     * 查询自习室配置
     *
     * @param id 自习室配置主键
     * @return 自习室配置
     */
    RoomConfigVo queryById(Long id);

    /**
     * 查询自习室配置列表
     *
     * @param bo 自习室配置
     * @return 自习室配置集合
     */
    TableDataInfo<RoomConfigVo> queryPageList(RoomConfigBo bo, PageQuery pageQuery);

    /**
     * 查询自习室配置列表
     *
     * @param bo 自习室配置
     * @return 自习室配置集合
     */
    List<RoomConfigVo> queryList(RoomConfigBo bo);

    /**
     * 新增自习室配置
     *
     * @param bo 自习室配置
     * @return 结果
     */
    Boolean insertByBo(RoomConfigBo bo);

    /**
     * 修改自习室配置
     *
     * @param bo 自习室配置
     * @return 结果
     */
    Boolean updateByBo(RoomConfigBo bo);

    /**
     * 校验并批量删除自习室配置信息
     *
     * @param ids 需要删除的自习室配置主键集合
     * @param isValid 是否校验,true-删除前校验,false-不校验
     * @return 结果
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 根据自习室ID和配置键查询配置值
     *
     * @param roomId 自习室ID
     * @param configKey 配置键
     * @return 配置值
     */
    String getConfigValue(Long roomId, String configKey);
}
