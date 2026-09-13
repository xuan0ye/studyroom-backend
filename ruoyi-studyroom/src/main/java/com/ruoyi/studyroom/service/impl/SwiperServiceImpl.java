package com.ruoyi.studyroom.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.domain.PageQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.ruoyi.studyroom.domain.bo.SwiperBo;
import com.ruoyi.studyroom.domain.vo.SwiperVo;
import com.ruoyi.studyroom.domain.Swiper;
import com.ruoyi.studyroom.mapper.SwiperMapper;
import com.ruoyi.studyroom.service.ISwiperService;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Collection;

/**
 * 轮播图Service业务层处理
 *
 * @author P_Peaceful
 * @date 2022-05-08
 */
@RequiredArgsConstructor
@Service
public class SwiperServiceImpl implements ISwiperService {

    private final SwiperMapper baseMapper;

    /**
     * 查询轮播图
     *
     * @param ossId 轮播图主键
     * @return 轮播图
     */
    @Override
    public SwiperVo queryById(Long ossId){
        return baseMapper.selectVoById(ossId);
    }

    /**
     * 查询轮播图列表
     *
     * @param bo 轮播图
     * @return 轮播图
     */
    @Override
    public TableDataInfo<SwiperVo> queryPageList(SwiperBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<Swiper> lqw = buildQueryWrapper(bo);
        Page<SwiperVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询轮播图列表
     *
     * @param bo 轮播图
     * @return 轮播图
     */
    @Override
    public List<SwiperVo> queryList(SwiperBo bo) {
        LambdaQueryWrapper<Swiper> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<Swiper> buildQueryWrapper(SwiperBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<Swiper> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(bo.getFileName()), Swiper::getFileName, bo.getFileName());
        lqw.like(StringUtils.isNotBlank(bo.getOriginalName()), Swiper::getOriginalName, bo.getOriginalName());
        lqw.eq(StringUtils.isNotBlank(bo.getFileSuffix()), Swiper::getFileSuffix, bo.getFileSuffix());
        lqw.eq(StringUtils.isNotBlank(bo.getUrl()), Swiper::getUrl, bo.getUrl());
        lqw.eq(bo.getStatus() != null, Swiper::getStatus, bo.getStatus());
        lqw.eq(StringUtils.isNotBlank(bo.getService()), Swiper::getService, bo.getService());
        return lqw;
    }

    /**
     * 新增轮播图
     *
     * @param bo 轮播图
     * @return 结果
     */
    @Override
    public Boolean insertByBo(SwiperBo bo) {
        Swiper add = BeanUtil.toBean(bo, Swiper.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setOssId(add.getOssId());
        }
        return flag;
    }

    @Override
    public Swiper upload(MultipartFile file) {
        String originalfileName = file.getOriginalFilename();
        if (originalfileName == null) {
            throw new ServiceException("文件名不能为空");
        }
        int dotIdx = originalfileName.lastIndexOf(".");
        String suffix = dotIdx >= 0 ? originalfileName.substring(dotIdx) : "";
        // 本地存储: 保存到 ruoyi-admin 运行目录/upload/swiper/, 通过 /upload/** 静态映射访问
        String dir = System.getProperty("user.dir") + File.separator + "upload" + File.separator + "swiper";
        File dirFile = new File(dir);
        if (!dirFile.exists() && !dirFile.mkdirs()) {
            throw new ServiceException("创建上传目录失败");
        }
        String fileName = System.currentTimeMillis() + "_" + IdUtil.fastSimpleUUID() + suffix;
        File target = new File(dirFile, fileName);
        try {
            file.transferTo(target);
        } catch (IOException e) {
            throw new ServiceException("文件保存失败: " + e.getMessage());
        }
        String url = "/upload/swiper/" + fileName;
        // 保存文件信息
        Swiper oss = new Swiper();
        oss.setUrl(url);
        oss.setFileSuffix(suffix);
        oss.setFileName(fileName);
        oss.setOriginalName(originalfileName);
        oss.setService("local");
        oss.setStatus(1);
        baseMapper.insert(oss);
        return oss;
    }

    /**
     * 修改轮播图
     *
     * @param bo 轮播图
     * @return 结果
     */
    @Override
    public Boolean updateByBo(SwiperBo bo) {
        Swiper update = BeanUtil.toBean(bo, Swiper.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     *
     * @param entity 实体类数据
     */
    private void validEntityBeforeSave(Swiper entity){
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除轮播图
     *
     * @param ids 需要删除的轮播图主键
     * @return 结果
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if(isValid){
            //TODO 做一些业务上的校验,判断是否需要校验
        }
        List<Swiper> list = baseMapper.selectBatchIds(ids);
        for (Swiper swiper : list) {
            // 本地存储文件删除(仅删除本地 upload 目录下的文件)
            if (swiper.getUrl() != null && swiper.getUrl().startsWith("/upload/")) {
                String fileName = swiper.getUrl().substring("/upload/".length());
                File f = new File(System.getProperty("user.dir") + File.separator + "upload" + File.separator + fileName);
                if (f.exists()) {
                    f.delete();
                }
            }
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }
}
