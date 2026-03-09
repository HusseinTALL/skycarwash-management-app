package com.skycarwash;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SkyCarWashApplication {

    public static void main(String[] args) {
        SpringApplication.run(SkyCarWashApplication.class, args);
    }
}
