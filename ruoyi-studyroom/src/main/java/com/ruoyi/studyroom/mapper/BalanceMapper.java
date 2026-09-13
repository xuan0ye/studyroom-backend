package com.ruoyi.studyroom.mapper;

import com.ruoyi.studyroom.domain.Balance;
import com.ruoyi.studyroom.domain.vo.BalanceVo;
import com.ruoyi.common.core.mapper.BaseMapperPlus;
import org.apache.ibatis.annotations.Param;

/**
 * 余额
Mapper接口
 *
 * @author P_Peaceful
 * @date 2022-05-08
 */
public interface BalanceMapper extends BaseMapperPlus<BalanceMapper, Balance, BalanceVo> {

    /**
     * 原子扣减余额(防止并发超扣): 仅当余额充足时扣减, 返回影响行数(0=余额不足)
     *
     * @param userId 用户ID
     * @param points 扣减点数(正整数)
     * @return 影响行数
     */
    int deductBalance(@Param("userId") Long userId, @Param("points") long points);

    /**
     * 原子增加余额(充值入账): UPDATE 行内自增, 避免并发 lost-update
     *
     * @param userId 用户ID
     * @param points 增加点数(正整数)
     * @return 影响行数
     */
    int addBalance(@Param("userId") Long userId, @Param("points") long points);

}
