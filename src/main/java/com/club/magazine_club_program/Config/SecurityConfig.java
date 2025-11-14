package com.club.magazine_club_program.Config;

import com.club.magazine_club_program.Service.CustomOAuth2UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomOAuth2AuthorizedClientRepository customOAuth2AuthorizedClientRepository;

    public SecurityConfig(
            CustomOAuth2UserService customOAuth2UserService,
            CustomOAuth2AuthorizedClientRepository customOAuth2AuthorizedClientRepository) {
        this.customOAuth2UserService = customOAuth2UserService;
        this.customOAuth2AuthorizedClientRepository = customOAuth2AuthorizedClientRepository;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
        requestCache.setMatchingRequestParameterName(null);
        
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(new AntPathRequestMatcher("/admin/setup-authenticator")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/admin/verify-authenticator")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/admin/verify-for-chat")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/admin/logout")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/login/kakao/**")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/login/oauth2/**")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/oauth2/authorization/**")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/oauth2/token/test")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/oauth2/token/**")).authenticated()
                        .requestMatchers(new AntPathRequestMatcher("/oauth2/**")).permitAll()
                        .anyRequest().permitAll()
                )
                .requestCache(cache -> cache.requestCache(requestCache))
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                        .authorizedClientRepository(customOAuth2AuthorizedClientRepository)
                        .authorizationEndpoint(authorization -> authorization
                                .baseUri("/oauth2/authorization")
                                .authorizationRequestRepository(authorizationRequestRepository())
                        )
                        .redirectionEndpoint(redirection -> redirection
                                .baseUri("/login/oauth2/code/*")
                        )
                        .defaultSuccessUrl("/login/kakao/success", true)
                        .failureUrl("/login/kakao/failure")
                )
                .headers(headers -> headers.frameOptions().disable())
                .sessionManagement(session -> session
                        .sessionFixation().migrateSession()
                        .maximumSessions(1)
                        .maxSessionsPreventsLogin(false)
                        .sessionRegistry(sessionRegistry())
                )
                .build();
    }

    @Bean
    public AuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository() {
        HttpSessionOAuth2AuthorizationRequestRepository repository = 
                new HttpSessionOAuth2AuthorizationRequestRepository();
        return repository;
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }
}
