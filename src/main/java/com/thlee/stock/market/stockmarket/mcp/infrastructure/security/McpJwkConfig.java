package com.thlee.stock.market.stockmarket.mcp.infrastructure.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.thlee.stock.market.stockmarket.mcp.config.McpOAuthProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.util.StringUtils;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

/**
 * MCP 토큰 서명 키. 인가 서버가 이 키로 access token(JWT)을 서명하고, 리소스 서버가 같은 키로 검증한다.
 * 키는 환경 변수 PEM으로 받아 재배포 후에도 토큰이 유지된다. 비어 있으면(dev) 기동 시 새로 만든다.
 */
@Slf4j
@Configuration
public class McpJwkConfig {

    @Bean
    public JWKSource<SecurityContext> mcpJwkSource(McpOAuthProperties properties) {
        RSAKey rsaKey = rsaKey(properties);
        return new ImmutableJWKSet<>(new JWKSet(rsaKey));
    }

    @Bean
    public JwtDecoder mcpJwtDecoder(JWKSource<SecurityContext> mcpJwkSource, McpOAuthProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSource(mcpJwkSource).build();
        // 만료 시간에 더해 발급자(iss)가 이 인가 서버인지 본다. aud 검증은 리소스 서버 설정이 덧씌운다
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.getIssuer()));
        return decoder;
    }

    private RSAKey rsaKey(McpOAuthProperties properties) {
        try {
            KeyPair keyPair = StringUtils.hasText(properties.getJwkPrivateKey())
                    ? fromPem(properties.getJwkPrivateKey())
                    : generate();
            return new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                    .privateKey((RSAPrivateKey) keyPair.getPrivate())
                    .keyID(properties.getJwkKeyId())
                    .build();
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("MCP 토큰 서명 키를 읽을 수 없습니다 (mcp.oauth.jwk-private-key)", e);
        }
    }

    private KeyPair generate() throws GeneralSecurityException {
        log.warn("mcp.oauth.jwk-private-key가 없어 MCP 서명 키를 새로 만듭니다. 재기동하면 발급된 MCP 토큰이 무효가 됩니다.");
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private KeyPair fromPem(String pem) throws GeneralSecurityException {
        String base64 = pem.replaceAll("-----(BEGIN|END) PRIVATE KEY-----", "").replaceAll("\\s", "");
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        RSAPrivateCrtKey privateKey = (RSAPrivateCrtKey) keyFactory.generatePrivate(
                new PKCS8EncodedKeySpec(Base64.getDecoder().decode(base64)));
        RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(
                new RSAPublicKeySpec(privateKey.getModulus(), privateKey.getPublicExponent()));
        return new KeyPair(publicKey, privateKey);
    }
}
