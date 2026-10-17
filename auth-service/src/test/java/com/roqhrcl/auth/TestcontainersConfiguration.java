package com.roqhrcl.auth;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgreSQLContainer() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"));
    }

    @Bean
    DynamicPropertyRegistrar testJwtKeys() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();

        Path dir = Files.createTempDirectory("jwt-keys");
        Path privateKey = Files.writeString(dir.resolve("private.pem"),
                pem("PRIVATE KEY", keyPair.getPrivate().getEncoded()));
        Path publicKey = Files.writeString(dir.resolve("public.pem"),
                pem("PUBLIC KEY", keyPair.getPublic().getEncoded()));

        return registry -> {
            registry.add("jwt.private-key", () -> privateKey.toUri().toString());
            registry.add("jwt.public-key", () -> publicKey.toUri().toString());
        };
    }

    private static String pem(String type, byte[] der) {
        String body = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(der);
        return "-----BEGIN " + type + "-----\n" + body + "\n-----END " + type + "-----\n";
    }
}
