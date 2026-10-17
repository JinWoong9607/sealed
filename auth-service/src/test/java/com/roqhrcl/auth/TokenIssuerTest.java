package com.roqhrcl.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.roqhrcl.auth.token.TokenIssuer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class TokenIssuerTest {

    @Autowired
    TokenIssuer tokenIssuer;

    @Autowired
    MockMvc mockMvc;

    private RSAKey publishedKey;
    private JwtDecoder decoder;

    @BeforeEach
    void loadPublishedKey() throws Exception {
        String jwks = mockMvc.perform(get("/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        publishedKey = JWKSet.parse(jwks).getKeys().get(0).toRSAKey();
        decoder = NimbusJwtDecoder.withPublicKey(publishedKey.toRSAPublicKey()).build();
    }

    @Test
    void jwksDoesNotExposePrivateKey() {
        assertThat(publishedKey.isPrivate()).isFalse();
    }

    @Test
    void issuedTokenIsVerifiedWithPublishedKey() {
        String token = tokenIssuer.issueAccessToken(42L);

        Jwt jwt = decoder.decode(token);

        assertThat(jwt.getSubject()).isEqualTo("42");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo(TokenIssuer.ISSUER);
        assertThat(jwt.getId()).isNotBlank();
        assertThat(jwt.getExpiresAt()).isAfter(jwt.getIssuedAt());
        assertThat(jwt.getHeaders().get("kid")).isEqualTo(publishedKey.getKeyID());
    }

    @Test
    void tamperedTokenIsRejected() {
        String[] parts = tokenIssuer.issueAccessToken(42L).split("\\.");
        String forgedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"1\",\"iss\":\"sealed-auth\"}".getBytes(StandardCharsets.UTF_8));
        String forged = parts[0] + "." + forgedPayload + "." + parts[2];

        assertThatThrownBy(() -> decoder.decode(forged))
                .isInstanceOf(JwtException.class);
    }
}