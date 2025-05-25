// springboot/src/main/java/com/example/common/config/WebConfig.java
package com.example.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Resource
    private JwtInterceptor jwtInterceptor;

    // 加自定义拦截器JwtInterceptor，设置拦截规则
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor).addPathPatterns("/**")
                .excludePathPatterns("/")
                .excludePathPatterns("/login")
                .excludePathPatterns("/register")
                .excludePathPatterns("/files/**")
                .excludePathPatterns("/visuals/health")
                .excludePathPatterns("/visuals/model/details")
                .excludePathPatterns("/visuals/result/**")        // 结果文件访问
                .excludePathPatterns("/visuals/batch/**")         // 批量结果文件访问
                .excludePathPatterns("/visuals/debug/**")
                .excludePathPatterns("/visuals/test/**")
                .excludePathPatterns("/visuals/download/**")
                .excludePathPatterns("/visuals/detect_frame_base64")

                .excludePathPatterns("/ws/**")           // WebSocket端点
                .excludePathPatterns("/realtime/**")
                .excludePathPatterns("/error");
    }
}