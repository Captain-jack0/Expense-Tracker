package com.expensetracker.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Central OpenAPI / Swagger UI configuration.
 *
 * <p>springdoc-openapi turns this bean plus the controller/DTO annotations
 * into a live OpenAPI 3 document served at:
 * <ul>
 *   <li><b>/v3/api-docs</b> — the raw JSON spec (import this into Postman)</li>
 *   <li><b>/swagger-ui.html</b> — interactive Swagger UI</li>
 * </ul>
 *
 * <p>A single {@code bearer-jwt} security scheme is declared and applied
 * globally so the "Authorize" button in Swagger UI attaches the
 * {@code Authorization: Bearer <token>} header to protected calls. The
 * public auth endpoints (register/login/refresh) still work without it.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearer-jwt";

    @Bean
    public OpenAPI expenseTrackerOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local development")))
                // Declare the scheme once under components...
                .components(new Components().addSecuritySchemes(BEARER_SCHEME, bearerScheme()))
                // ...and apply it to every operation by default.
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }

    private Info apiInfo() {
        return new Info()
                .title("Expense Tracker API")
                .version("v1")
                .description("""
                        REST API for the Expense Tracker application.

                        **Authentication:** obtain an access token from `POST /api/auth/login`
                        (or `/register`) and send it as `Authorization: Bearer <accessToken>`.
                        Use the **Authorize** button above to set it for all requests here.

                        **Response envelope:** auth endpoints return
                        `{ success, data, error }`. See `docs/api/README.md` in the repo
                        for the full conventions (error format, versioning, examples).""")
                .contact(new Contact().name("Expense Tracker").url("https://github.com/Captain-jack0/Expense-Tracker"))
                .license(new License().name("Educational use"));
    }

    private SecurityScheme bearerScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Paste the accessToken returned by /api/auth/login (no 'Bearer ' prefix).");
    }
}
