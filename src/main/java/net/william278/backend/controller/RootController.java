package net.william278.backend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.net.URI;

// Redirect to the API documentation
@Controller
public class RootController {

    @Value("${app.frontend-base-url}")
    public static final String CORS_FRONTEND_ORIGIN = "http://localhost:3000";

    @GetMapping({"/", "/docs"})
    public ResponseEntity<?> redirectToDocs() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create("docs/"))
                .build();
    }

    @GetMapping({"/login"})
    public ResponseEntity<?> redirectToLogin() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create("oauth2/authorization/discord"))
                .build();
    }

}