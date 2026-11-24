package com.upendra.dispatch.config;

import io.swagger.v3.oas.models.OpenAPI;
import com.upendra.dispatch.controller.DispatchController;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI dispatchOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Dispatch API")
                .version("1.0.0")
                        .description("Core dispatch endpoints: submit vehicles and orders, "
                                + "get the dispatch plan, plus the summary"))
                .addTagsItem(new Tag().name(DispatchController.TAG_EXTRAS)
                        .description("Helpers to list, fetch and delete stored orders and vehicles"));
    }
}
