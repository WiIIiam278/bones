/*
 * MIT License
 *
 * Copyright (c) 2024 William278
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.william278.backend.controller;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import net.william278.backend.configuration.AppConfiguration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.net.URI;

@Controller
@RequiredArgsConstructor
public class RootController {

    private final AppConfiguration config;
    private final Environment environment;

    // Redirect to the API documentation on staging, or the frontend if in production
    @SneakyThrows
    @GetMapping({"/", "/docs"})
    public ResponseEntity<?> baseEndpointRedirect() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(isStaging() ? URI.create("docs/") : config.getFrontendBaseUrl().toURI())
                .build();
    }

    @GetMapping({"/login"})
    public ResponseEntity<?> redirectToLogin() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create("oauth2/authorization/discord"))
                .build();
    }

    private boolean isStaging() {
        return environment.getActiveProfiles().length > 0 && environment.getActiveProfiles()[0].equals("staging");
    }

}