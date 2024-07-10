package net.william278.backend.controller;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

// Redirect to the API documentation
@Controller
public class RootController {

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