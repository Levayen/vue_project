package com.example.app.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3 文档配置（M5 收尾）
 * - 文档标题/版本/描述
 * - Bearer JWT 认证方案：在 Swagger UI 右上角 "Authorize" 填入 token 即可调用受保护接口
 * - 访问地址：/swagger-ui/index.html
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearer-jwt";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("学生考试系统 API")
                        .version("1.0.0")
                        .description("三角色（管理员/教师/学生）在线考试系统：题库、组卷、考试、自动阅卷、成绩统计。"
                                + " 登录获取 token 后，点击页面右上角 Authorize，输入 Bearer 前缀的 token 即可调用受保护接口。")
                        .contact(new Contact().name("学生考试系统").email("dev@example.com"))
                        .license(new License().name("MIT")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("登录接口返回的 token，直接填入（无需手动加 Bearer 前缀）")));
    }
}
