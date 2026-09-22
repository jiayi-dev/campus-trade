package com.jiayi.campustrade.config;

import com.jiayi.campustrade.auth.AuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;

    // 上传根目录（与 UploadController 保持同一配置项）
    @Value("${app.upload.dir:D:/campus-trade-uploads}")
    private String uploadDir;

    // 配置拦截器
    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        // 仅放行无需登录态的接口
                        // 注意：不再使用 /auth/** 通配，否则 /auth/me 也会被放行，
                        // 导致 AuthContext 永远不被写入，下游 AuthService.currentUser() 拿到 userId=null
                        "/auth/student/login",
                        "/auth/seller/login",
                        "/auth/admin/login",
                        "/user/register",
                        // 首页平台概览：公开接口（在售数/已完成交易数/卖家数/收藏数/动态校区），
                        // 无需登录即可展示首页平台数据
                        "/home/overview",
                        // 上传后的图片走静态资源映射对外访问：
                        // <img src> 请求不会携带 Authorization 请求头，
                        // 不放行会导致所有图片 401；文件名为 UUID 随机串，可防枚举
                        "/files/**",
                        "/error"
                );
    }

    // 静态资源映射：/files/** → 本地上传目录
    // context-path 为 /api，外部实际访问路径是 /api/files/goods/xxx.jpg
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:" + uploadDir + "/");

    }

    // 配置跨域 CORS
    @Override
    public void addCorsMappings(CorsRegistry registry) {

        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}