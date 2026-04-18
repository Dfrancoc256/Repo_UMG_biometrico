package com.umg.biometrico.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        Path rutaUploads = Paths.get("uploads").toAbsolutePath().normalize();
        Path rutaFotosPersonas = Paths.get("fotos_personas").toAbsolutePath().normalize();

        String uploadsUri = rutaUploads.toUri().toString();
        if (!uploadsUri.endsWith("/")) uploadsUri += "/";

        String fotosUri = rutaFotosPersonas.toUri().toString();
        if (!fotosUri.endsWith("/")) fotosUri += "/";

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadsUri);

        registry.addResourceHandler("/fotos_personas/**")
                .addResourceLocations(fotosUri);
    }
}