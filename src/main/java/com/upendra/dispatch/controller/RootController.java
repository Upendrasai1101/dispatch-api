package com.upendra.dispatch.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/** Opening the base URL in a browser takes you straight to the API docs. */
@Hidden
@RestController
public class RootController {

    @GetMapping("/")
    public ResponseEntity<Void> redirectToSwagger() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create("/swagger-ui.html"))
                .build();
    }
}
