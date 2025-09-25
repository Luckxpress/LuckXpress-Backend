package com.luckxpress.core.auth;

import com.luckxpress.common.security.UserPrincipal;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;
    private UserPrincipal principal;

    @BeforeEach
    void setup() throws Exception {
        // Generate in-memory RSA keypair for signing/verification
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) kp.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) kp.getPrivate();

        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
        JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(new JWKSet(rsaKey));

        JwtEncoder encoder = new NimbusJwtEncoder(jwkSource);
        JwtDecoder decoder = NimbusJwtDecoder.withPublicKey(publicKey).build();

        tokenProvider = new JwtTokenProvider(encoder, decoder);
        ReflectionTestUtils.setField(tokenProvider, "issuer", "test-issuer");
        ReflectionTestUtils.setField(tokenProvider, "accessTokenValidityInSeconds", 3600L); // 1h
        ReflectionTestUtils.setField(tokenProvider, "refreshTokenValidityInSeconds", 7200L); // 2h

        principal = UserPrincipal.builder()
                .userId("user-1")
                .username("john")
                .email("john@example.com")
                .stateCode("CA")
                .roles(List.of("USER", "ADMIN"))
                .kycVerified(true)
                .enabled(true)
                .accountLocked(false)
                .accountExpired(false)
                .credentialsExpired(false)
                .build();
    }

    @Test
    @DisplayName("Access token generation and parsing")
    void generateAndParseAccessToken() {
        String access = tokenProvider.generateAccessToken(principal);
        assertThat(access).isNotBlank();

        assertThat(tokenProvider.validateToken(access)).isTrue();
        assertThat(tokenProvider.isRefreshToken(access)).isFalse();

        UserPrincipal parsed = tokenProvider.getUserPrincipalFromToken(access);
        assertThat(parsed).isNotNull();
        assertThat(parsed.getUserId()).isEqualTo("user-1");
        assertThat(parsed.getUsername()).isEqualTo("john");
        assertThat(parsed.getEmail()).isEqualTo("john@example.com");
        assertThat(parsed.getStateCode()).isEqualTo("CA");
        assertThat(parsed.getAuthorities()).extracting("authority").contains("ROLE_USER", "ROLE_ADMIN");
        assertThat(parsed.isKycVerified()).isTrue();

        Instant exp = tokenProvider.getExpirationDateFromToken(access);
        assertThat(exp).isAfter(Instant.now());
        assertThat(tokenProvider.isTokenExpired(access)).isFalse();
    }

    @Test
    @DisplayName("Refresh token identification")
    void refreshTokenIdentification() {
        String refresh = tokenProvider.generateRefreshToken(principal);
        assertThat(refresh).isNotBlank();
        assertThat(tokenProvider.validateToken(refresh)).isTrue();
        assertThat(tokenProvider.isRefreshToken(refresh)).isTrue();
    }

    @Test
    @DisplayName("Expired token is detected and invalid")
    void expiredTokenDetected() {
        // Create an already-expired token by setting negative validity
        ReflectionTestUtils.setField(tokenProvider, "accessTokenValidityInSeconds", -5L);
        String expired = tokenProvider.generateAccessToken(principal);

        // Decoder should reject it as invalid; our validateToken should return false
        assertThat(tokenProvider.validateToken(expired)).isFalse();
        assertThat(tokenProvider.isTokenExpired(expired)).isTrue();
    }
}
