package com.thlee.stock.market.stockmarket.realestate.infrastructure.source.common;

import org.springframework.web.client.RestClientResponseException;

import java.util.regex.Pattern;

/**
 * 외부 API 인증키 마스킹 헬퍼.
 * <p>
 * URL/메시지에 평문으로 노출되는 query string의 인증키 값을 {@code ***}로 치환한다.
 * 로그·예외 메시지 단계에서 호출해 민감정보 누출을 방지한다.
 */
public final class SecretMasker {

    private static final Pattern SECRET_PARAM = Pattern.compile(
            "(?i)(serviceKey|apiKey|authkey|crtfc[-_]?key|api[-_]?key|access[-_]?token|key)=[^&\\s\"']+");

    /** 응답 body snippet 최대 길이 (진단성 vs 로그 노이즈 균형). */
    private static final int BODY_SNIPPET_MAX = 300;

    private SecretMasker() {
    }

    /** 민감 query param 값을 마스킹한다. null/empty 입력은 그대로 반환. */
    public static String mask(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return SECRET_PARAM.matcher(input).replaceAll("$1=***");
    }

    /**
     * 원본 throwable의 message를 마스킹한 sanitized 사본을 반환.
     * stacktrace는 보존하여 디버깅성을 유지하되, 평문 인증키는 제거한다.
     * <p>
     * {@link RestClientResponseException}인 경우 응답 status·body snippet({@value #BODY_SNIPPET_MAX}자)을
     * message에 함께 포함 — 4xx/5xx 진단 시 어떤 응답이 왔는지 stacktrace에서 즉시 확인.
     */
    public static Throwable sanitize(Throwable original) {
        if (original == null) {
            return null;
        }
        String message = String.valueOf(original.getMessage());
        if (original instanceof RestClientResponseException rcre) {
            message = enrichWithResponseBody(message, rcre);
        }
        Throwable copy = new RuntimeException(mask(message));
        copy.setStackTrace(original.getStackTrace());
        return copy;
    }

    private static String enrichWithResponseBody(String message, RestClientResponseException rcre) {
        String body = String.valueOf(rcre.getResponseBodyAsString());
        // 공백 압축 — HTML 에러 페이지 등 멀티라인 응답을 한 줄로 압축
        String compact = body.replaceAll("\\s+", " ").trim();
        String snippet = compact.length() > BODY_SNIPPET_MAX
                ? compact.substring(0, BODY_SNIPPET_MAX) + "...(truncated)"
                : compact;
        return String.format("HTTP %d | %s | body: %s",
                rcre.getStatusCode().value(), message, snippet);
    }
}
