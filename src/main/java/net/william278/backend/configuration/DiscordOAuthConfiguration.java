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

package net.william278.backend.configuration;

import lombok.RequiredArgsConstructor;
import net.william278.backend.database.model.Channel;
import net.william278.backend.database.model.Project;
import net.william278.backend.security.DiscordOAuthUserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.RequestEntity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequestEntityConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static net.william278.backend.util.OAuthUtils.withUserAgent;

@Configuration
@RequiredArgsConstructor
public class DiscordOAuthConfiguration {

    private final DiscordOAuthUserService discordOAuthUserService;
    private final AppConfiguration config;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(c -> {
                    final CsrfTokenRequestAttributeHandler handler = new CsrfTokenRequestAttributeHandler();
                    handler.setCsrfRequestAttributeName(null); // https://stackoverflow.com/a/75047103
                    c.csrfTokenRequestHandler(handler);

                    final CookieCsrfTokenRepository repo = new CookieCsrfTokenRepository();
                    repo.setCookieCustomizer(cookie -> cookie
                            .domain(config.getCookieDomain())
                            .maxAge(config.getCookieMaxAgeDays() * 24 * 60 * 60)
                            .build());
                    c.csrfTokenRepository(repo);

                    // Ignore webhook endpoint
                    c.ignoringRequestMatchers(new AntPathRequestMatcher(
                            "/v1/projects/{projectName:%s}/docs".formatted(Project.PATTERN),
                            "POST", false
                    ));
                    // Ignore transaction webhook endpoints
                    c.ignoringRequestMatchers(new AntPathRequestMatcher(
                            "/v1/transactions/paypal/{secret}",
                            "POST", false
                    ));
                    c.ignoringRequestMatchers(new AntPathRequestMatcher(
                            "/v1/transactions/stripe/{secret}",
                            "POST", false
                    ));
                    // Ignore API version publishing endpoint
                    c.ignoringRequestMatchers(new AntPathRequestMatcher(
                            "/v1/projects/{projectSlug:%s}/channels/{channelName:%s}/versions/api"
                                    .formatted(Project.PATTERN, Channel.PATTERN),
                            "POST", false
                    ));
                    // Ignore API user endpoints
                    c.ignoringRequestMatchers(new AntPathRequestMatcher(
                            "/v1/users/{userId}/email/api",
                            "POST", false
                    ));
                })
                .cors(c -> {
                    final CorsConfiguration cors = new CorsConfiguration();
                    cors.setAllowCredentials(true);
                    cors.setAllowedOrigins(List.of(config.getFrontendBaseUrl().toString()));
                    cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
                    cors.setAllowedHeaders(List.of("Cookie", "Content-Type", "X-XSRF-TOKEN"));

                    final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                    source.registerCorsConfiguration("/**", cors);
                    c.configurationSource(source);
                })
                .oauth2Login(a -> {
                    a.loginPage("/oauth2/authorization/discord");
                    a.tokenEndpoint(e -> e.accessTokenResponseClient(this.accessTokenResponseClient()));
                    a.userInfoEndpoint(e -> e.userService(discordOAuthUserService));
                    a.defaultSuccessUrl("%s/account".formatted(config.getFrontendBaseUrl().toString()), true);
                })
                .logout(l -> {
                    l.logoutRequestMatcher(new AntPathRequestMatcher("/logout"));
                    l.deleteCookies("JSESSIONID", "XSRF-TOKEN");
                    l.logoutSuccessUrl(config.getFrontendBaseUrl().toString());
                })
                .build();
    }

    @Bean
    public OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient() {
        DefaultAuthorizationCodeTokenResponseClient client = new DefaultAuthorizationCodeTokenResponseClient();

        client.setRequestEntityConverter(new OAuth2AuthorizationCodeGrantRequestEntityConverter() {
            @Override
            public RequestEntity<?> convert(OAuth2AuthorizationCodeGrantRequest oauth2Request) {
                return withUserAgent(super.convert(oauth2Request));
            }
        });

        return client;
    }

    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("JSESSIONID");
        serializer.setCookiePath("/");
        serializer.setDomainName(config.getCookieDomain());
        serializer.setCookieMaxAge(((int) config.getCookieMaxAgeDays()) * 24 * 60 * 60);
        return serializer;
    }

}
