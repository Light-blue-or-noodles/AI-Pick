package com.aipick.config;

import org.springframework.boot.web.servlet.filter.OrderedCharacterEncodingFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;

/**
 * Web MVC 配置
 *
 * @author AI-Pick
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;

    @Value("${upload.dir:uploads}")
    private String uploadDir;

    @Value("${upload.avatar-subdir:avatars}")
    private String avatarSubdir;

    public WebMvcConfig(JwtInterceptor jwtInterceptor) {
        this.jwtInterceptor = jwtInterceptor;
    }

    /**
     * 静态资源：上传的头像等通过 /api/static/** 访问，映射到项目目录 uploads/
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 默认封面图：从 classpath 提供，供数据库中 cover_image 引用
        registry.addResourceHandler("/static/covers/**")
                .addResourceLocations("classpath:/static/covers/");
        String location = "file:" + uploadDir + "/";
        registry.addResourceHandler("/static/**")
                .addResourceLocations(location);
    }

    /**
     * 配置字符编码过滤器，确保支持中文参数
     */
    @Bean
    public OrderedCharacterEncodingFilter characterEncodingFilter() {
        OrderedCharacterEncodingFilter filter = new OrderedCharacterEncodingFilter();
        filter.setOrder(OrderedCharacterEncodingFilter.HIGHEST_PRECEDENCE);
        filter.setEncoding(StandardCharsets.UTF_8.name());
        return filter;
    }

    /**
     * 配置跨域
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 生产环境应使用具体域名，可通过环境变量配置
        String allowedOrigins = System.getenv().getOrDefault("CORS_ALLOWED_ORIGINS", "*");
        registry.addMapping("/**")
                .allowedOriginPatterns(allowedOrigins.split(","))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * 配置拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/user/login",
                        "/api/user/register",
                        "/api/user/wechat-login",
                        "/api/static/**",
                        "/api/partner/list",
                        "/api/partner/*",
                        "/api/partner/filter",
                        "/api/activity/list",
                        "/api/activity/*",
                        "/api/activity/calendar",
                        "/api/ai/*"
                );
    }
}