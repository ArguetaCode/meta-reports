package com.sodi.metareports;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class MetaReportsApplication {
    public static void main(String[] args) {
        SpringApplication.run(MetaReportsApplication.class, args);
    }
}
