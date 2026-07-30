package io.github.wasiliystrecker.returns;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.interfaces.RSAPublicKey;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import tools.jackson.databind.ObjectMapper;

@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
class ApiSecurityConfiguration {

  @Bean
  @ConditionalOnMissingBean(JwtDecoder.class)
  JwtDecoder jwtDecoder(OAuth2ResourceServerProperties resourceServerProperties)
      throws IOException {
    OAuth2ResourceServerProperties.Jwt properties = resourceServerProperties.getJwt();
    if (properties.getPublicKeyLocation() == null) {
      throw new IllegalStateException("JWT public-key-location must be configured");
    }

    RSAPublicKey publicKey;
    try (var input = properties.getPublicKeyLocation().getInputStream()) {
      publicKey =
          Objects.requireNonNull(
              RsaKeyConverters.x509().convert(input), "JWT public key could not be decoded");
    }

    return localJwtDecoder(publicKey, properties.getIssuerUri(), properties.getAudiences());
  }

  static NimbusJwtDecoder localJwtDecoder(
      RSAPublicKey publicKey, String issuer, List<String> expectedAudiences) {
    Objects.requireNonNull(publicKey, "publicKey");
    if (issuer == null || issuer.isBlank()) {
      throw new IllegalArgumentException("issuer must not be blank");
    }
    if (expectedAudiences == null
        || expectedAudiences.isEmpty()
        || expectedAudiences.stream()
            .anyMatch(audience -> audience == null || audience.isBlank())) {
      throw new IllegalArgumentException("expectedAudiences must contain a non-blank audience");
    }

    List<String> audiences = List.copyOf(expectedAudiences);
    var decoder = NimbusJwtDecoder.withPublicKey(publicKey).build();
    var audienceValidator =
        new JwtClaimValidator<List<String>>(
            JwtClaimNames.AUD,
            tokenAudiences ->
                tokenAudiences != null && !Collections.disjoint(tokenAudiences, audiences));
    List<OAuth2TokenValidator<Jwt>> validators =
        List.of(new JwtIssuerValidator(issuer), audienceValidator);
    decoder.setJwtValidator(JwtValidators.createDefaultWithValidators(validators));
    return decoder;
  }

  @Bean
  SecurityFilterChain apiSecurity(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
    AuthenticationEntryPoint authenticationEntryPoint = authenticationEntryPoint(objectMapper);
    AccessDeniedHandler accessDeniedHandler = accessDeniedHandler(objectMapper);

    http.sessionManagement(
            sessions -> sessions.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            authorize ->
                authorize
                    .requestMatchers("/livez", "/readyz", "/.well-known/oauth-protected-resource")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/actuator/health/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/v1/returns")
                    .hasAuthority("SCOPE_returns:write")
                    .requestMatchers(HttpMethod.POST, "/api/v1/returns/{returnId}/inspection")
                    .hasAuthority("SCOPE_returns:inspect")
                    .requestMatchers(HttpMethod.PUT, "/api/v1/returns/{returnId}/refund")
                    .hasAuthority("SCOPE_refunds:settle")
                    .requestMatchers(HttpMethod.GET, "/api/v1/returns/{returnId}")
                    .hasAuthority("SCOPE_returns:read")
                    .requestMatchers(HttpMethod.POST, "/actuator/eventpublications")
                    .hasAuthority("SCOPE_operations:manage")
                    .requestMatchers(HttpMethod.GET, "/actuator/**")
                    .hasAuthority("SCOPE_operations:read")
                    .anyRequest()
                    .denyAll())
        .exceptionHandling(
            exceptions ->
                exceptions
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler))
        .oauth2ResourceServer(
            oauth2 ->
                oauth2
                    .jwt(Customizer.withDefaults())
                    .protectedResourceMetadata(
                        metadata ->
                            metadata.protectedResourceMetadataCustomizer(
                                resource ->
                                    resource
                                        .resourceName("Returns workflow API")
                                        .scopes(
                                            scopes ->
                                                scopes.addAll(
                                                    List.of(
                                                        "returns:read",
                                                        "returns:write",
                                                        "returns:inspect",
                                                        "refunds:settle",
                                                        "operations:read",
                                                        "operations:manage")))))
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler));

    return http.build();
  }

  private static AuthenticationEntryPoint authenticationEntryPoint(ObjectMapper objectMapper) {
    var bearerEntryPoint = new BearerTokenAuthenticationEntryPoint();
    bearerEntryPoint.setRealmName("returns-api");

    return (request, response, exception) -> {
      bearerEntryPoint.commence(request, response, exception);
      writeProblem(
          objectMapper,
          request,
          response,
          "authentication-required",
          "Authentication required",
          "A valid bearer token is required.");
    };
  }

  private static AccessDeniedHandler accessDeniedHandler(ObjectMapper objectMapper) {
    var bearerDeniedHandler = new BearerTokenAccessDeniedHandler();
    bearerDeniedHandler.setRealmName("returns-api");

    return (request, response, exception) -> {
      bearerDeniedHandler.handle(request, response, exception);
      writeProblem(
          objectMapper,
          request,
          response,
          "insufficient-scope",
          "Insufficient scope",
          "The bearer token does not grant the required scope.");
    };
  }

  private static void writeProblem(
      ObjectMapper objectMapper,
      HttpServletRequest request,
      HttpServletResponse response,
      String code,
      String title,
      String detail)
      throws IOException {
    ProblemDetail problem = ProblemDetail.forStatus(response.getStatus());
    problem.setDetail(detail);
    problem.setType(URI.create("https://wasiliy-strecker.github.io/problems/" + code));
    problem.setTitle(title);
    problem.setInstance(URI.create(request.getRequestURI()));
    problem.setProperty("code", code.toUpperCase().replace('-', '_'));
    problem.setProperty("requestId", request.getAttribute(RequestCorrelationFilter.ATTRIBUTE_NAME));

    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response.setHeader("Cache-Control", "no-store");
    objectMapper.writeValue(response.getOutputStream(), problem);
  }
}
