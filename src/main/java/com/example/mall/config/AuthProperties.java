package com.example.mall.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.Duration;

@ConstructorBinding
@ConfigurationProperties(prefix = "mall.auth")
@Validated
public class AuthProperties {
    @NotBlank
    private final String secret;

    @NotNull
    private final Duration ttl;

    @NotBlank
    private final String issuer;

    public AuthProperties(String secret, Duration ttl, String issuer) {
        this.secret = secret;
        this.ttl = ttl;
        this.issuer = issuer;
    }

    public String getSecret() {
        return secret;
    }

    public Duration getTtl() {
        return ttl;
    }

    public String getIssuer() {
        return issuer;
    }
}
