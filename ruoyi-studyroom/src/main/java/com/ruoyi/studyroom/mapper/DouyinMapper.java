package com.ruoyi.studyroom.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * 抖音团购核销 Mapper（配置/核销记录/商品映射）
 * 说明: 返回 Map 的查询统一用 camelCase 别名, 避免 Map 结果键名大小写不确定
 */
public interface DouyinMapper {

    /* ==================== 配置 ==================== */

    @Select("select config_key as configKey, config_value as configValue from sr_douyin_config")
    List<Map<String, Object>> selectConfigs();

    @Insert("insert into sr_douyin_config(config_key, config_value, update_time) values(#{key}, #{value}, now()) "
        + "on duplicate key update config_value = #{value}, update_time = now()")
    int upsertConfig(@Param("key") String key, @Param("value") String value);

    /* ==================== 核销记录 ==================== */

    @Insert("insert into sr_douyin_verify(user_id, poi_id, code, code_hash, encrypted_code, certificate_id, verify_id, douyin_order_id, "
        + "product_id, product_title, grant_type, balance_amount, balance_action, verify_time, status, card_user_id, card_id, seat_id, remark, raw_prepare, raw_verify, "
        + "create_time, update_time) values(#{p.userId}, #{p.poiId}, #{p.code}, #{p.codeHash}, #{p.encryptedCode}, #{p.certificateId}, "
        + "#{p.verifyId}, #{p.douyinOrderId}, #{p.productId}, #{p.productTitle}, #{p.grantType}, #{p.balanceAmount}, #{p.balanceAction}, "
        + "#{p.verifyTime}, #{p.status}, #{p.cardUserId}, #{p.cardId}, #{p.seatId}, #{p.remark}, #{p.rawPrepare}, #{p.rawVerify}, now(), now())")
    int insertVerify(@Param("p") Map<String, Object> p);

    /** 按券码哈希查已核销记录(本地去重, 避免重复核销) */
    @Select("select id, verify_time as verifyTime, status from sr_douyin_verify "
        + "where code_hash = #{codeHash} and status = 1 order by id desc limit 1")
    Map<String, Object> selectVerifiedByCodeHash(@Param("codeHash") String codeHash);

    @Select("<script>select id, user_id as userId, poi_id as poiId, code, certificate_id as certificateId, "
        + "verify_id as verifyId, douyin_order_id as douyinOrderId, product_id as productId, product_title as productTitle, "
        + "grant_type as grantType, balance_amount as balanceAmount, balance_action as balanceAction, verify_time as verifyTime, "
        + "status, cancel_time as cancelTime, card_user_id as cardUserId, card_id as cardId, seat_id as seatId, remark, "
        + "create_time as createTime from sr_douyin_verify where 1=1 "
        + "<if test='q.userId != null'> and user_id = #{q.userId}</if> "
        + "<if test='q.status != null'> and status = #{q.status}</if> "
        + "<if test='q.certificateId != null and q.certificateId != \"\"'> and certificate_id = #{q.certificateId}</if> "
        + "<if test='q.verifyId != null and q.verifyId != \"\"'> and verify_id = #{q.verifyId}</if> "
        + "<if test='q.code != null and q.code != \"\"'> and code = #{q.code}</if> "
        + "order by id desc limit #{q.offset}, #{q.limit}</script>")
    List<Map<String, Object>> selectVerifyList(@Param("q") Map<String, Object> q);

    @Select("<script>select count(*) from sr_douyin_verify where 1=1 "
        + "<if test='q.userId != null'> and user_id = #{q.userId}</if> "
        + "<if test='q.status != null'> and status = #{q.status}</if> "
        + "<if test='q.certificateId != null and q.certificateId != \"\"'> and certificate_id = #{q.certificateId}</if> "
        + "<if test='q.verifyId != null and q.verifyId != \"\"'> and verify_id = #{q.verifyId}</if> "
        + "<if test='q.code != null and q.code != \"\"'> and code = #{q.code}</if> "
        + "</script>")
    long countVerify(@Param("q") Map<String, Object> q);

    @Select("select id, user_id as userId, poi_id as poiId, code, certificate_id as certificateId, "
        + "verify_id as verifyId, douyin_order_id as douyinOrderId, product_id as productId, product_title as productTitle, "
        + "grant_type as grantType, balance_amount as balanceAmount, balance_action as balanceAction, verify_time as verifyTime, "
        + "status, cancel_time as cancelTime, card_user_id as cardUserId, card_id as cardId, seat_id as seatId, remark, "
        + "create_time as createTime from sr_douyin_verify where id = #{id}")
    Map<String, Object> selectVerifyById(@Param("id") Long id);

    /** 已核销(未撤销)的券: 用于幂等判断与退款审核 */
    @Select("select id, user_id as userId, code, certificate_id as certificateId, verify_id as verifyId, "
        + "product_title as productTitle, grant_type as grantType, balance_amount as balanceAmount, "
        + "balance_action as balanceAction, verify_time as verifyTime, status, card_user_id as cardUserId, card_id as cardId, "
        + "seat_id as seatId from sr_douyin_verify where certificate_id = #{certificateId} and status = 1 order by id desc limit 1")
    Map<String, Object> selectVerifiedByCertificateId(@Param("certificateId") String certificateId);

    @Update("update sr_douyin_verify set balance_action = #{action}, update_time = now() where id = #{id}")
    int updateBalanceAction(@Param("id") Long id, @Param("action") Integer action);

    @Update("update sr_douyin_verify set status = #{status}, cancel_time = #{cancelTime}, remark = #{remark}, "
        + "update_time = now() where id = #{id}")
    int updateVerifyStatus(@Param("id") Long id, @Param("status") Integer status,
                           @Param("cancelTime") java.util.Date cancelTime, @Param("remark") String remark);

    /* ==================== 商品映射 ==================== */

    @Select("select id, product_id as productId, product_title as productTitle, card_id as cardId, "
        + "grant_type as grantType, balance_amount as balanceAmount, once_per_user as oncePerUser, "
        + "lock_mode as lockMode, remark, status from sr_douyin_product_map where status = 1 order by id desc")
    List<Map<String, Object>> selectProductMaps();

    @Select("<script>select id, product_id as productId, product_title as productTitle, card_id as cardId, "
        + "grant_type as grantType, balance_amount as balanceAmount, once_per_user as oncePerUser, "
        + "lock_mode as lockMode, remark, status from sr_douyin_product_map where status = 1 "
        + "<if test='productId != null and productId != \"\"'> and product_id = #{productId}</if> "
        + "order by id desc limit 1</script>")
    Map<String, Object> selectProductMapByProductId(@Param("productId") String productId);

    /** 该用户是否已核销过该商品(用于"每用户限一次"防重复薅) */
    @Select("select count(*) from sr_douyin_verify where user_id = #{userId} and product_id = #{productId} and status = 1")
    long countVerifiedByUserAndProduct(@Param("userId") Long userId, @Param("productId") String productId);

    @Insert("insert into sr_douyin_product_map(product_id, product_title, card_id, grant_type, balance_amount, once_per_user, lock_mode, remark, status, update_time) "
        + "values(#{p.productId}, #{p.productTitle}, #{p.cardId}, #{p.grantType}, #{p.balanceAmount}, #{p.oncePerUser}, #{p.lockMode}, #{p.remark}, 1, now())")
    int insertProductMap(@Param("p") Map<String, Object> p);

    @Update("update sr_douyin_product_map set product_id = #{p.productId}, product_title = #{p.productTitle}, "
        + "card_id = #{p.cardId}, grant_type = #{p.grantType}, balance_amount = #{p.balanceAmount}, "
        + "once_per_user = #{p.oncePerUser}, lock_mode = #{p.lockMode}, remark = #{p.remark}, update_time = now() where id = #{p.id}")
    int updateProductMap(@Param("p") Map<String, Object> p);

    @Delete("delete from sr_douyin_product_map where id = #{id}")
    int deleteProductMap(@Param("id") Long id);
}
