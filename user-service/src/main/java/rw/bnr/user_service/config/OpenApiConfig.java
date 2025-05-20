package rw.bnr.user_service.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;

@OpenAPIDefinition(
        info = @Info(
                contact = @Contact(
                        name = "Kabera Clapton",
                        email = "ckabera6@gmail.com"
                ),
                description = "API spec for BNR internship project",
                title = "BNR Internship - Kabera Clapton",
                version = "1.0"
        ),
        servers = {
                @Server(
                        description = "Local dev env",
                        url = "http://localhost:8001"
                )
        },
        security = {
                @SecurityRequirement(
                        name = "JWT Bearer Auth"
                )
        }
)
@SecurityScheme(
        name = "JWT Bearer Auth",
        description = "JWT auth description",
        scheme = "bearer",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER
)
public class OpenApiConfig {
}
