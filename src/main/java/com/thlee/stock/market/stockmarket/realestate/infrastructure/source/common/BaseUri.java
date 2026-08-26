package com.thlee.stock.market.stockmarket.realestate.infrastructure.source.common;

import java.net.URI;

/**
 * 어댑터 baseUrl 파싱 결과 (scheme, host, port, contextPath).
 * <p>
 * 기존 어댑터들이 개별로 보유하던 {@code stripScheme/stripContext/extractPort} 헬퍼를
 * 통합한다. {@code RestClient.get().uri(builder -> ...)} 람다에서 한 번에 사용한다.
 *
 * <pre>
 * BaseUri base = BaseUri.parse(props.getBaseUrl());
 * restClient.get().uri(b -> b
 *     .scheme(base.scheme())
 *     .host(base.host())
 *     .port(base.port())
 *     .path(base.contextPath() + EXTRA_PATH)
 *     .build());
 * </pre>
 *
 * port 미지정 baseUrl은 {@code -1}을 반환 — Spring {@code UriBuilder.port(-1)}는
 * scheme 기본값을 사용한다.
 */
public record BaseUri(String scheme, String host, int port, String contextPath) {

    public static BaseUri parse(String baseUrl) {
        URI uri = URI.create(baseUrl);
        String path = uri.getPath();
        return new BaseUri(
                uri.getScheme(),
                uri.getHost(),
                uri.getPort(),
                path == null ? "" : path);
    }
}