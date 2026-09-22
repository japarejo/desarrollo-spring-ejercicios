package com.atech.curso.m6;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.integration.annotation.IntegrationComponentScan;

@SpringBootApplication
@IntegrationComponentScan
public class M6Application {

    public static void main(String[] args) {
        SpringApplication.run(M6Application.class, args);
    }
}
