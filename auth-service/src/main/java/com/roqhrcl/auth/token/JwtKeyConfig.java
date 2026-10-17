package com.roqhrcl.auth.token;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@Configuration
public class JwtKeyConfig {

    @Bean
    RSAKey jwtSigningKey(@Value("${jwt.private-key}")
                         Resource privateKey, @Value("${jwt.public-key}")
    Resource publicKey) throws Exception {
        RSAPrivateKey priv = RsaKeyConverters.pkcs8().convert(privateKey.getInputStream());

        RSAPublicKey pub = RsaKeyConverters.x509().convert(publicKey.getInputStream());

        return new RSAKey.Builder(pub)
                .privateKey(priv)
                .keyIDFromThumbprint()
                .build();
    }

    @Bean
    JwtEncoder jwtEncoder(RSAKey jwtSigningKey) {
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwtSigningKey)));
    }
}
