package com.assignment.dataset.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc OpenAPI configuration.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Dynamic JSON Dataset Query API")
                        .version("1.0.0")
                        .description("REST API that stores arbitrary JSON records per dataset in a relational database, " +
                                "and lets clients query a dataset with dynamic group-by and sort-by.")
                        .contact(new Contact()
                                .name("Engineering Team")
                                .email("engineering@dataset-service.local")));
    }
}
