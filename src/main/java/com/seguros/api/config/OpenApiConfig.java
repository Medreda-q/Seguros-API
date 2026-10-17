package com.seguros.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration de la documentation OpenAPI 3.0 / Swagger UI.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI segurosOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("SegurosAPI — API REST de Gestión de Seguros")
                .description("API REST segura para la contratación de pólizas (Auto, Hogar, Vida), "
                    + "gestión de clientes asegurados y tramitación de expedientes de siniestros en España.")
                .version("1.0.0")
                .contact(new Contact()
                    .name("Equipo de Desarrollo SegurosAPI")
                    .email("soporte@seguros-api.es"))
                .license(new License()
                    .name("Proprietary")
                    .url("https://seguros-api.es/license")));
    }
}
