package com.finbrain.config;

import io.minio.MinioClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "minio")
public class MinioConfig {

    private String endpoint = "localhost";
    private Integer port = 9000;
    private String accessKey = "minioadmin";
    private String secretKey = "minioadmin";
    private String bucketName = "finbrain-docs";

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint, port, false)
                .credentials(accessKey, secretKey)
                .build();
    }
}
