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
                .title("Dispatch Load Balancer API")
                .version("1.0.0")
                .description("Assigns delivery orders to vehicles: HIGH priority first, "
                        + "vehicle capacity never exceeded, nearest vehicle chosen using the "
                        + "Haversine distance. Quick start: POST /vehicles, POST /orders, GET /plan."))
                .addTagsItem(new Tag().name(DispatchController.TAG_CORE)
                        .description("The endpoints required by the assignment: submit vehicles and orders, "
                                + "get the dispatch plan, plus the bonus summary"))
                .addTagsItem(new Tag().name(DispatchController.TAG_EXTRAS)
                        .description("Helpers to list, fetch and delete stored orders and vehicles"));
    }
}
