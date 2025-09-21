package com.luckxpress;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EntityScan(basePackages = {"com.luckxpress.data.entity"})
@EnableJpaRepositories(basePackages = {"com.luckxpress.data.repository"})
@EnableJpaAuditing
public class LuckXpressApplication {

    public static void main(String[] args) {
        SpringApplication.run(LuckXpressApplication.class, args);
    }
}
