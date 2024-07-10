package net.william278.backend.configuration;

import lombok.RequiredArgsConstructor;
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
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

import static net.william278.backend.util.OAuthUtils.withUserAgent;

@Configuration
@RequiredArgsConstructor
public class DiscordOAuthConfiguration {

    private final DiscordOAuthUserService discordOAuthUserService;
    private final AppConfiguration configuration;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .oauth2Login(a -> {
                    a.loginPage("/oauth2/authorization/discord");
                    a.tokenEndpoint(e -> e.accessTokenResponseClient(this.accessTokenResponseClient()));
                    a.userInfoEndpoint(e -> e.userService(discordOAuthUserService));
                    a.defaultSuccessUrl("%s/account".formatted(configuration.getFrontendBaseUrl().toString()), true);
                })
                .logout(l -> {
                    l.logoutRequestMatcher(new AntPathRequestMatcher("/logout"));
                    l.deleteCookies("JSESSIONID");
                    l.logoutSuccessUrl(configuration.getFrontendBaseUrl().toString());
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
        serializer.setDomainNamePattern("^.+?\\.(\\w+\\.[a-z]+)$");
        return serializer;
    }

}
