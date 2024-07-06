package net.william278.backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;

@Configuration
public class SecurityConfiguration {

    public static final String DISCORD_USER_AGENT = "William278 (https://github.com/WiIIiam278/william278-backend)";

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http.oauth2Login(a -> {
            // https://github.com/spring-projects/spring-security/issues/4958
            a.tokenEndpoint(e -> e.accessTokenResponseClient(this.customAccessTokenResponseClient()));
            a.userInfoEndpoint(e -> e.userService(this.customOAuth2UserService()));
        }).build();
    }

    private OAuth2UserService<OAuth2UserRequest, OAuth2User> customOAuth2UserService() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setErrorHandler(new OAuth2ErrorResponseErrorHandler());
        setRestTemplateUserAgent(restTemplate);
        final DefaultOAuth2UserService service = new DefaultOAuth2UserService();
        service.setRestOperations(restTemplate);
        return service;
    }

    private OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> customAccessTokenResponseClient() {
        RestTemplate restTemplate = new RestTemplate(Arrays.asList(
                new FormHttpMessageConverter(), new OAuth2AccessTokenResponseHttpMessageConverter()
        ));
        setRestTemplateUserAgent(restTemplate);
        restTemplate.setErrorHandler(new OAuth2ErrorResponseErrorHandler());
        final DefaultAuthorizationCodeTokenResponseClient service = new DefaultAuthorizationCodeTokenResponseClient();
        service.setRestOperations(restTemplate);
        return service;
    }

    private void setRestTemplateUserAgent(RestTemplate restTemplate) {
        restTemplate.getInterceptors().add((request, body, execution) -> {
            request.getHeaders().add("User-Agent", DISCORD_USER_AGENT);
            return execution.execute(request, body);
        });
    }

}
