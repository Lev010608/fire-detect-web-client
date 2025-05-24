// springboot/src/main/java/com/example/common/config/CorsConfig.java
package com.example.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * 跨域配置
 */
@Configuration
public class CorsConfig {
    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        // 普通接口的CORS配置
        CorsConfiguration generalConfig = new CorsConfiguration();
        generalConfig.addAllowedOriginPattern("*");
        generalConfig.addAllowedHeader("*");
        generalConfig.addAllowedMethod("*");
        generalConfig.setAllowCredentials(true);
        generalConfig.addExposedHeader("Content-Type");
        generalConfig.addExposedHeader("Content-Length");
        generalConfig.addExposedHeader("Content-Disposition");

        // 视频/文件资源的CORS配置（不设置credentials，使用*通配符）
        CorsConfiguration mediaConfig = new CorsConfiguration();
        mediaConfig.addAllowedOrigin("*");  // 使用通配符
        mediaConfig.addAllowedHeader("*");
        mediaConfig.addAllowedMethod("*");
        mediaConfig.setAllowCredentials(false);  // 关键：设为false以使用通配符
        mediaConfig.addExposedHeader("Content-Type");
        mediaConfig.addExposedHeader("Content-Length");
        mediaConfig.addExposedHeader("Content-Range");
        mediaConfig.addExposedHeader("Accept-Ranges");
        mediaConfig.addExposedHeader("Cache-Control");

        // 🔥 新增：WebSocket特殊配置
        CorsConfiguration webSocketConfig = new CorsConfiguration();
        webSocketConfig.addAllowedOriginPattern("*");
        webSocketConfig.setAllowCredentials(true);
        webSocketConfig.addAllowedMethod("*");
        webSocketConfig.addAllowedHeader("*");
        webSocketConfig.addExposedHeader("*");

        // 对不同路径应用不同配置
        source.registerCorsConfiguration("/visuals/result/**", mediaConfig);  // 媒体文件
        source.registerCorsConfiguration("/visuals/batch/**", mediaConfig);   // 批量结果文件
        source.registerCorsConfiguration("/files/**", mediaConfig);           // 普通文件

        // 🔥 新增：实时检测相关端点的CORS配置
        source.registerCorsConfiguration("/realtime/**", generalConfig);
        source.registerCorsConfiguration("/ws/**", webSocketConfig);

        source.registerCorsConfiguration("/**", generalConfig);               // 其他接口（放在最后）

        return new CorsFilter(source);
    }
}