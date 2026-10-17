package com.roqhrcl.auth.token;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class JwksController {

    private final RSAKey jwtSigningKey;

    JwksController(RSAKey jwtSigningKey) {
        this.jwtSigningKey = jwtSigningKey;
    }

    @GetMapping("/.well-known/jwks.json")
    Map<String, Object> jwks() {
        return new JWKSet(jwtSigningKey.toPublicJWK()).toJSONObject();
    }
}
