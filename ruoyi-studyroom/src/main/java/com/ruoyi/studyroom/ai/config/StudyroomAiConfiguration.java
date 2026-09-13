package com.ruoyi.studyroom.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 学伴配置绑定
 * 通过 @Bean + @ConfigurationProperties 显式创建并绑定 studyroom.ai 配置段,
 * 避免模块间 ConfigurationProperties 自动绑定不生效的问题
 */
@Configuration
public class StudyroomAiConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "studyroom.ai")
    public StudyroomAiProperties studyroomAiProperties() {
        return new StudyroomAiProperties();
    }
}
