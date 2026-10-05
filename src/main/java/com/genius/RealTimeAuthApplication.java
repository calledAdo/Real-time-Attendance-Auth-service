package com.genius;

import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.boot.SpringApplication;

@SpringBootApplication(scanBasePackages = "com.genius")
public class RealTimeAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(RealTimeAuthApplication.class, args);
    }

}