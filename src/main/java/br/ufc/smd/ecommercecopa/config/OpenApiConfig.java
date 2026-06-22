package br.ufc.smd.ecommercecopa.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String SESSION_AUTH = "sessionAuth";

    @Bean
    public OpenAPI ecommerceCopaOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Ecommerce Copa API")
                        .description("API backend do Ecommerce Copa com autenticação por sessão, catálogo público e CRUD administrativo.")
                        .version("1.0.0"))
                .components(new Components()
                        .addSecuritySchemes(SESSION_AUTH, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name("JSESSIONID")
                                .description("Cookie de sessão retornado após login.")));
    }
}
