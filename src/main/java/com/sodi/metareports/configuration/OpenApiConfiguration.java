package com.sodi.metareports.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {
    @Bean
    OpenAPI internalApi() {
        return new OpenAPI().info(new Info()
                .title("SODI Meta Reports API")
                .version("0.1.0")
                .description("Internal API. Real Meta integration is not enabled in this phase."));
    }
}
