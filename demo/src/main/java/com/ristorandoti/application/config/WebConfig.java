package com.ristorandoti.application.config;

import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.ristorandoti.application.service.FileStorageService;

import lombok.RequiredArgsConstructor;

/**
 * Espone le immagini caricate dagli utenti come file statici sotto {@code /uploads/**}.
 * I nomi sono UUID mai riutilizzati, quindi il browser può tenerle in cache a lungo.
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final FileStorageService fileStorageService;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(fileStorageService.getRootDir().toUri().toString())
                .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic());
    }
}
