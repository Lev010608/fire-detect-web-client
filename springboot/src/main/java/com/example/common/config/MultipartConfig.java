// springboot/src/main/java/com/example/common/config/MultipartConfig.java
package com.example.common.config;

import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

import javax.servlet.MultipartConfigElement;

/**
 * Multipart文件上传配置
 */
@Configuration
public class MultipartConfig {

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();

        // 设置文件大小限制
        factory.setMaxFileSize(DataSize.ofMegabytes(100)); // 100MB
        factory.setMaxRequestSize(DataSize.ofMegabytes(100)); // 100MB

        return factory.createMultipartConfig();
    }
}