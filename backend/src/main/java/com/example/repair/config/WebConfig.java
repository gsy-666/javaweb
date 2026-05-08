package com.example.repair.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
  @Value("${app.cors.allowed-origin-patterns:http://localhost:*}")
  private String allowedOriginPatterns;

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    String[] originPatterns = java.util.Arrays.stream(allowedOriginPatterns.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .toArray(String[]::new);

    registry
        .addMapping("/**")
        .allowedOriginPatterns(originPatterns.length == 0 ? new String[] {"http://localhost:*"} : originPatterns)
        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(true);
  }

  @Value("${app.upload.dir:./uploads}")
  private String uploadDir;

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    String dir = uploadDir;
    if (dir == null || dir.isBlank()) dir = "./uploads";
    if (!dir.endsWith("/")) dir = dir + "/";
    registry.addResourceHandler("/uploads/**").addResourceLocations("file:" + dir);
  }
}

