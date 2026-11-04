package com.dowglasmaia.financialtransaction.shared.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.SupplierJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Spec BE-04: the API is a stateless OAuth2 resource server. It validates the bearer JWT locally (signature through the
 * issuer's cached JWKS, issuer, audience, expiry, subject), maps the roles claim to {@code ROLE_*}, and answers 401/403
 * in the same {@code {code,message}} shape as every other error. No session, no login page, no client secret, no
 * provider SDK: only the issuer, the audience and the roles-claim name are provider specific (see .spec/auth-auth0.md).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Duration CLOCK_SKEW = Duration.ofSeconds(60);

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            JwtAuthenticationConverter jwtAuthenticationConverter,
                                            CorsConfigurationSource corsConfigurationSource,
                                            AuthenticationEntryPoint authenticationEntryPoint,
                                            AccessDeniedHandler accessDeniedHandler,
                                            @Qualifier("customerLinkedAuthorization") AuthorizationManager<RequestAuthorizationContext> customerLinkedAuthorization) throws Exception {
        return http
              .csrf(AbstractHttpConfigurer::disable) // bearer-only API: no cookies, nothing for CSRF to protect
              .cors(cors -> cors.configurationSource(corsConfigurationSource))
              .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
              .authorizeHttpRequests(authorize -> authorize
                    .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                    .requestMatchers("/actuator/prometheus").permitAll() // BE-08: scraped on the private management port (never the API port)
                    .requestMatchers("/actuator/**").hasRole("ADMIN") // no admin role is issued today: effectively closed
                    .requestMatchers("/api/**").access(customerLinkedAuthorization)
                    .anyRequest().authenticated())
              .exceptionHandling(handling -> handling
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler))
              .oauth2ResourceServer(resourceServer -> resourceServer
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler))
              .build();
    }

    /**
     * Built lazily from the issuer's discovery document so the application starts even when the identity provider is
     * briefly unreachable. The default decoder only checks issuer and expiry: the audience and subject are added here.
     */
    @Bean
    JwtDecoder jwtDecoder(@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer,
                          @Value("${app.security.audience}") String audience) {
        return new SupplierJwtDecoder(() -> {
            NimbusJwtDecoder decoder = JwtDecoders.fromIssuerLocation(issuer);
            decoder.setJwtValidator(tokenValidator(issuer, audience));
            return decoder;
        });
    }

    static OAuth2TokenValidator<Jwt> tokenValidator(String issuer, String audience) {
        return new DelegatingOAuth2TokenValidator<>(
              new JwtTimestampValidator(CLOCK_SKEW),
              new JwtIssuerValidator(issuer),
              jwt -> jwt.getAudience() != null && jwt.getAudience().contains(audience)
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "The token audience is not this API.", null)),
              jwt -> jwt.getSubject() != null && !jwt.getSubject().isBlank()
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "The token has no subject.", null)));
    }

    /** Only the roles claim becomes authorities ({@code customer} -> {@code ROLE_CUSTOMER}); scopes are not used (D-30). */
    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter(@Value("${app.security.roles-claim}") String rolesClaim) {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> rolesOf(jwt, rolesClaim));
        return converter;
    }

    static Collection<GrantedAuthority> rolesOf(Jwt jwt, String rolesClaim) {
        List<String> roles = jwt.getClaimAsStringList(rolesClaim);
        if (roles == null) return List.of();
        return roles.stream()
              .filter(role -> role != null && !role.isBlank())
              .<GrantedAuthority>map(role -> new SimpleGrantedAuthority("ROLE_" + role.trim().toUpperCase()))
              .toList();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${app.security.cors.allowed-origins}") List<String> allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        configuration.setAllowCredentials(false); // the token travels in a header, never in a cookie
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    AuthenticationEntryPoint authenticationEntryPoint(ObjectMapper mapper) {
        BearerTokenAuthenticationEntryPoint challenge = new BearerTokenAuthenticationEntryPoint();
        return (request, response, exception) -> {
            challenge.commence(request, response, exception); // sets 401 and WWW-Authenticate: Bearer ...
            writeError(mapper, response, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication is required with a valid access token.");
        };
    }

    @Bean
    AccessDeniedHandler accessDeniedHandler(ObjectMapper mapper) {
        return (request, response, exception) ->
              writeError(mapper, response, HttpStatus.FORBIDDEN, "FORBIDDEN", "Access denied.");
    }

    private static void writeError(ObjectMapper mapper, jakarta.servlet.http.HttpServletResponse response,
                                   HttpStatus status, String code, String message) throws java.io.IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(), Map.of("code", code, "message", message));
    }
}
