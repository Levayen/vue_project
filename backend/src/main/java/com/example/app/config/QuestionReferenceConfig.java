package com.example.app.config;

import com.example.app.service.QuestionReferencePort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 题目引用端口配置（SPEC-question-bank）
 * 默认桩实现：exam-paper 模块（M2）提供真实 QuestionReferencePort 前，
 * 认为题目未被引用；真实 Bean 存在时本桩自动退让。
 */
@Configuration
public class QuestionReferenceConfig {

    @Bean
    @ConditionalOnMissingBean(QuestionReferencePort.class)
    public QuestionReferencePort noopQuestionReferencePort() {
        return questionId -> false;
    }
}
