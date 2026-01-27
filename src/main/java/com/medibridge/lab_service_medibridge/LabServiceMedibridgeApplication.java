package com.medibridge.lab_service_medibridge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class LabServiceMedibridgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(LabServiceMedibridgeApplication.class, args);
    }

}
