package com.se1908.group01.config;

import com.se1908.group01.security.JwtAuthenticationFilter;
import com.se1908.group01.security.OAuth2FrontendFailureHandler;
import com.se1908.group01.security.OAuth2FrontendSuccessHandler;
import com.se1908.group01.security.RestAccessDeniedHandler;
import com.se1908.group01.security.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;
    private final OAuth2FrontendSuccessHandler oAuth2FrontendSuccessHandler;
    private final OAuth2FrontendFailureHandler oAuth2FrontendFailureHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config)
            throws Exception {

        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http)
            throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .cors(cors ->
                        cors.configurationSource(
                                corsConfigurationSource()))

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS))

                .formLogin(form -> form.disable())

                .authorizeHttpRequests(auth -> auth

                        // Auth
                        .requestMatchers("/api/auth/**")
                        .permitAll()

                        // OAuth2
                        .requestMatchers(
                                "/oauth2/**",
                                "/login/oauth2/**")
                        .permitAll()

                        // Swagger
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**")
                        .permitAll()

                        // Public documents
                        .requestMatchers(
                                "/api/documents/public/**")
                        .permitAll()

                        // Test SubscriptionPlan
                        .requestMatchers(
                                "/api/subscription-plans/**")
                        .permitAll()

                        // TẠM THỜI MỞ PAYMENT ĐỂ DEBUG
                        .requestMatchers(
                                "/api/payments/**")
                        .permitAll()

                        .anyRequest()
                        .authenticated()
                )

                .exceptionHandling(exception ->
                        exception
                                .authenticationEntryPoint(
                                        restAuthenticationEntryPoint)
                                .accessDeniedHandler(
                                        restAccessDeniedHandler))

                .oauth2Login(oauth2 ->
                        oauth2
                                .successHandler(
                                        oAuth2FrontendSuccessHandler)
                                .failureHandler(
                                        oAuth2FrontendFailureHandler))

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config =
                new CorsConfiguration();

        config.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:5173"
        ));

        config.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "DELETE",
                "PATCH",
                "OPTIONS"
        ));

        config.setAllowedHeaders(List.of("*"));

        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                config);

        return source;
    }
}