package com.securefilesharing.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.securefilesharing.security.JwtService;
import com.securefilesharing.service.CustomUserDetailsService;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(
            JwtService jwtService,
            CustomUserDetailsService userDetailsService) {

        return new JwtAuthenticationFilter(
                jwtService,
                userDetailsService);
    }

    @Bean
    public SecurityFilterChain securityfilerChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter)
            throws Exception {

        http
            .csrf(csrf -> csrf.disable())

            // Enable CORS.
            // Actual CORS configuration is inside CorsConfig.java
            .cors(cors -> {})

            .sessionManagement(session ->
                session.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS)
            )

            .authorizeHttpRequests(auth -> auth

                // Allow CORS preflight requests
                .requestMatchers(
                    org.springframework.http.HttpMethod.OPTIONS,
                    "/**"
                ).permitAll()

                .requestMatchers(
                    "/api/auth/register",
                    "/api/auth/register/resend-otp",
                    "/api/auth/register/verify-otp",
                    "/api/auth/login",
                    "/api/auth/forgot-password",
                    "/api/auth/forgot-password/resend-otp",
                    "/api/auth/forgot-password/verify-otp",
                    "/api/auth/forgot-password/reset",
                    "/api/files/download/**",
                    "/api/files/quick-share"
                ).permitAll()

                .anyRequest().authenticated()
            )

            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }
}