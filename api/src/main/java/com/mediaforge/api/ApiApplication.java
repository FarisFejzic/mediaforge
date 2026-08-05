package com.mediaforge.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan("com.mediaforge.common.domain")
@EnableJpaRepositories("com.mediaforge.common.repository")
@ConfigurationPropertiesScan({"com.mediaforge.api", "com.mediaforge.common"})
@ComponentScan({"com.mediaforge.api", "com.mediaforge.common"})
public class ApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiApplication.class, args);
    }
}