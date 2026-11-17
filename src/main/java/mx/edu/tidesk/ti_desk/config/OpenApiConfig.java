package mx.edu.tidesk.ti_desk.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Documentación OpenAPI de la API. Declara la autenticación por token JWT
 * para que Swagger UI muestre el botón "Authorize" y envíe el encabezado
 * Authorization: Bearer en las rutas protegidas.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "TI-Desk API",
                version = "v1",
                description = "Mesa de ayuda del área de TI de la universidad"),
        security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT")
public class OpenApiConfig {
}