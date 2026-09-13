package com.ruoyi.studyroom.wx.douyin;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.studyroom.mapper.DouyinMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 抖音团购配置服务(存 sr_douyin_config, 管理后台可改)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DouyinConfigService {

    private final DouyinMapper douyinMapper;

    public Map<String, String> loadAll() {
        Map<String, String> map = new HashMap<>();
        List<Map<String, Object>> list = douyinMapper.selectConfigs();
        if (list != null) {
            for (Map<String, Object> row : list) {
                Object k = row.get("configKey");
                Object v = row.get("configValue");
                if (k != null) {
                    map.put(String.valueOf(k), v == null ? "" : String.valueOf(v));
                }
            }
        }
        return map;
    }

    public String get(String key) {
        String v = loadAll().get(key);
        return v == null ? "" : v;
    }

    public void save(String key, String value) {
        douyinMapper.upsertConfig(key, value == null ? "" : value.trim());
    }

    /** 取核销所需配置, 缺失时抛出可读异常 */
    public Map<String, String> requireConfig() {
        Map<String, String> c = loadAll();
        if (!"1".equals(c.get("enabled"))) {
            throw new ServiceException("抖音团购核销未启用，请联系门店工作人员");
        }
        if (isBlank(c.get("client_key")) || isBlank(c.get("client_secret"))) {
            throw new ServiceException("抖音对接参数未配置，请联系门店管理员");
        }
        if (isBlank(c.get("poi_id"))) {
            throw new ServiceException("抖音门店POI未配置，请联系门店管理员");
        }
        return c;
    }

    public boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
