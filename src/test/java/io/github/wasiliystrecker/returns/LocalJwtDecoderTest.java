package io.github.wasiliystrecker.returns;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtException;

final class LocalJwtDecoderTest {

  private static final String ISSUER = "https://auth.example.test";
  private static final String AUDIENCE = "returns-api";

  private static KeyPair signingKeys;

  @BeforeAll
  static void createSigningKeys() throws Exception {
    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    signingKeys = generator.generateKeyPair();
  }

  @Test
  void verifiesSignatureIssuerAudienceAndTimeBoundsLocally() {
    var decoder =
        ApiSecurityConfiguration.localJwtDecoder(
            (RSAPublicKey) signingKeys.getPublic(), ISSUER, List.of(AUDIENCE));

    var jwt =
        decoder.decode(
            token(
                signingKeys.getPrivate(),
                ISSUER,
                AUDIENCE,
                Instant.now().minusSeconds(5),
                Instant.now().plusSeconds(300)));

    assertThat(jwt.getSubject()).isEqualTo("decoder-test");
    assertThat(jwt.getAudience()).containsExactly(AUDIENCE);
  }

  @Test
  void rejectsAnUnexpectedIssuer() {
    var decoder =
        ApiSecurityConfiguration.localJwtDecoder(
            (RSAPublicKey) signingKeys.getPublic(), ISSUER, List.of(AUDIENCE));

    assertThatThrownBy(
            () ->
                decoder.decode(
                    token(
                        signingKeys.getPrivate(),
                        "https://other-issuer.example",
                        AUDIENCE,
                        Instant.now().minusSeconds(5),
                        Instant.now().plusSeconds(300))))
        .isInstanceOf(JwtException.class);
  }

  @Test
  void rejectsAnUnexpectedAudience() {
    var decoder =
        ApiSecurityConfiguration.localJwtDecoder(
            (RSAPublicKey) signingKeys.getPublic(), ISSUER, List.of(AUDIENCE));

    assertThatThrownBy(
            () ->
                decoder.decode(
                    token(
                        signingKeys.getPrivate(),
                        ISSUER,
                        "another-api",
                        Instant.now().minusSeconds(5),
                        Instant.now().plusSeconds(300))))
        .isInstanceOf(JwtException.class);
  }

  @Test
  void rejectsAnExpiredToken() {
    var decoder =
        ApiSecurityConfiguration.localJwtDecoder(
            (RSAPublicKey) signingKeys.getPublic(), ISSUER, List.of(AUDIENCE));

    assertThatThrownBy(
            () ->
                decoder.decode(
                    token(
                        signingKeys.getPrivate(),
                        ISSUER,
                        AUDIENCE,
                        Instant.now().minusSeconds(600),
                        Instant.now().minusSeconds(300))))
        .isInstanceOf(JwtException.class);
  }

  private static String token(
      PrivateKey privateKey, String issuer, String audience, Instant issuedAt, Instant expiresAt) {
    String header = "{\"alg\":\"RS256\",\"typ\":\"JWT\"}";
    String claims =
        """
        {"iss":"%s","sub":"decoder-test","aud":["%s"],"iat":%d,"exp":%d}
        """
            .formatted(issuer, audience, issuedAt.getEpochSecond(), expiresAt.getEpochSecond())
            .strip();
    String unsigned = encode(header) + "." + encode(claims);

    try {
      Signature signer = Signature.getInstance("SHA256withRSA");
      signer.initSign(privateKey);
      signer.update(unsigned.getBytes(StandardCharsets.US_ASCII));
      return unsigned + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(signer.sign());
    } catch (java.security.GeneralSecurityException exception) {
      throw new IllegalStateException("Unable to create test JWT", exception);
    }
  }

  private static String encode(String value) {
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(value.getBytes(StandardCharsets.UTF_8));
  }
}
