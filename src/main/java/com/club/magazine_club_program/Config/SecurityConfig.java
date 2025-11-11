package com.club.magazine_club_program.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(new AntPathRequestMatcher("/admin/setup-authenticator")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/admin/verify-authenticator")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/admin/verify-for-chat")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/admin/logout")).permitAll()
                        .anyRequest().permitAll()
                )
                .headers(headers -> headers.frameOptions().disable())
                .sessionManagement(session -> session
                        .maximumSessions(1)
                        .maxSessionsPreventsLogin(false)
                )
                .build();
    }
}
