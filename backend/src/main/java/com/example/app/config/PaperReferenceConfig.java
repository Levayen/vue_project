package com.example.app.config;

import com.example.app.service.PaperReferencePort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 试卷引用端口配置（SPEC-exam-paper）
 * 默认桩实现：exam-session 模块（M3）提供真实 PaperReferencePort 前，
 * 认为试卷未被考试引用；真实 Bean 存在时本桩自动退让。
 */
@Configuration
public class PaperReferenceConfig {

    @Bean
    @ConditionalOnMissingBean(PaperReferencePort.class)
    public PaperReferencePort noopPaperReferencePort() {
        return paperId -> false;
    }
}
