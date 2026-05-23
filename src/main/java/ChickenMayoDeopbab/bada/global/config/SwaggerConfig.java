package ChickenMayoDeopbab.bada.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

import java.util.List;

@Configuration
public class SwaggerConfig {
    @Value("${app.server-url}")
    private String serverUrl;

    @Bean
    public OpenAPI customOpenAPI() {
        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name(HttpHeaders.AUTHORIZATION);

        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList(HttpHeaders.AUTHORIZATION);

        return new OpenAPI()
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local"),
                        new Server().url(serverUrl).description("Production")
                ))
                .components(new Components().addSecuritySchemes(HttpHeaders.AUTHORIZATION, securityScheme))
                .addSecurityItem(securityRequirement)
                .info(new Info()
                        .title("BADA API")
                        .description("BADA 서비스의 API 문서")
                        .version("1.0.0"));
    }
}
