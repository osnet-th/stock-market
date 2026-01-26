package com.thlee.stock.market.stockmarket.user.infrastructure.oauth.kakao;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class KakaoJwksProvider {
    private static final String KAKAO_JWKS_URI = "https://kauth.kakao.com/.well-known/jwks.json";
    private static final Duration JWKS_CACHE_TTL = Duration.ofHours(6);

    private final KakaoClient kakaoClient;

    private volatile JwksCache jwksCache = JwksCache.empty();

    public RSAPublicKey getPublicKey(String kid) {
        JwksCache cache = getJwksCache();
        RSAPublicKey key = cache.keys.get(kid);
        if (key != null) {
            return key;
        }
        cache = refreshKeys();
        key = cache.keys.get(kid);
        if (key == null) {
            throw new IllegalStateException("JWKS에 해당 kid가 존재하지 않습니다: " + kid);
        }
        return key;
    }

    private JwksCache getJwksCache() {
        JwksCache cache = jwksCache;
        if (!cache.isExpired()) {
            return cache;
        }
        return refreshKeys();
    }

    private JwksCache refreshKeys() {
        synchronized (this) {
            JwksCache cache = jwksCache;
            if (!cache.isExpired()) {
                return cache;
            }
            RestClient restClient = kakaoClient.restClient();
            JwksResponse response = restClient.get()
                .uri(KAKAO_JWKS_URI)
                .retrieve()
                .body(JwksResponse.class);
            if (response == null || response.keys == null || response.keys.length == 0) {
                throw new IllegalStateException("JWKS 응답이 비어 있습니다.");
            }
            Map<String, RSAPublicKey> keys = new ConcurrentHashMap<>();
            for (JwkKey jwk : response.keys) {
                if (!"RSA".equals(jwk.kty)) {
                    continue;
                }
                keys.put(jwk.kid, toPublicKey(jwk));
            }
            jwksCache = new JwksCache(keys, Instant.now());
            return jwksCache;
        }
    }

    private RSAPublicKey toPublicKey(JwkKey jwk) {
        try {
            byte[] nBytes = Base64.getUrlDecoder().decode(jwk.n);
            byte[] eBytes = Base64.getUrlDecoder().decode(jwk.e);
            BigInteger modulus = new BigInteger(1, nBytes);
            BigInteger exponent = new BigInteger(1, eBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) keyFactory.generatePublic(new RSAPublicKeySpec(modulus, exponent));
        } catch (Exception e) {
            throw new IllegalStateException("JWKS 공개키 생성 실패", e);
        }
    }

    private record JwksResponse(
        JwkKey[] keys
    ) {
    }

    private record JwkKey(
        String kid,
        String kty,
        String alg,
        String use,
        String n,
        String e
    ) {
    }

    private record JwksCache(
        Map<String, RSAPublicKey> keys,
        Instant fetchedAt
    ) {
        boolean isExpired() {
            return fetchedAt == null || fetchedAt.plus(JWKS_CACHE_TTL).isBefore(Instant.now());
        }

        static JwksCache empty() {
            return new JwksCache(new ConcurrentHashMap<>(), null);
        }
    }
}