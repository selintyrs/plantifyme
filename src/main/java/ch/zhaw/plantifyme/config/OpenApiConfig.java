package ch.zhaw.plantifyme.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI plantifyOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Plantify API")
                        .description("REST API der Webapplikation Plantify, entwickelt im Rahmen der " +
                                "Bachelor-Arbeit \"Plantify — Pflanzliche Alternativen für Schweizer " +
                                "Supermärkte\" an der ZHAW im Frühlingssemester 2026.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Selin Soy"))
                )
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Auth0 JWT Token. Format im Authorization-Header: 'Bearer <token>'")
                        )
                );
    }
}
