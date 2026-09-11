package com.freshflow.api.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3.0 configuration for FreshFlow MVP REST API.
 *
 * <p>Exposes API documentation at {@code /api-docs} and interactive Swagger UI at {@code
 * /swagger-ui/index.html}.
 */
@Configuration
public class OpenApiConfig {

  public static final String BEARER_AUTH = "bearerAuth";
  public static final String MERCHANT_USER_ID_HEADER = "merchantUserIdAuth";

  @Bean
  public OpenAPI freshflowOpenAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("FreshFlow MVP REST API")
                .description(
                    "REST API documentation for FreshFlow Quick-Commerce & F&B platform. "
                        + "Provides customer catalog browsing (search, filter, pagination, daily capacity) "
                        + "and merchant catalog administration.")
                .version("v1.0.0")
                .contact(
                    new Contact()
                        .name("FreshFlow Engineering Team")
                        .email("engineering@freshflow.com"))
                .license(
                    new License()
                        .name("Apache 2.0")
                        .url("https://www.apache.org/licenses/LICENSE-2.0")))
        .servers(
            List.of(
                new Server().url("http://localhost:8080").description("Local Development Server")))
        .components(
            new Components()
                .addSecuritySchemes(
                    BEARER_AUTH,
                    new SecurityScheme()
                        .name(BEARER_AUTH)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description(
                            "JWT Bearer token for authenticated customers, merchants, and drivers"))
                .addSecuritySchemes(
                    MERCHANT_USER_ID_HEADER,
                    new SecurityScheme()
                        .name("X-User-Id")
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .description(
                            "Merchant actor User ID header (ownership verification for MVP)")));
  }
}
