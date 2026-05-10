package com.thlee.stock.market.stockmarket.realestate.infrastructure.source.common;

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
     * 원본 throwable의 message만 마스킹한 sanitized 사본을 반환.
     * stacktrace는 보존하여 디버깅성을 유지하되, 평문 인증키는 제거한다.
     */
    public static Throwable sanitize(Throwable original) {
        if (original == null) {
            return null;
        }
        Throwable copy = new RuntimeException(mask(String.valueOf(original.getMessage())));
        copy.setStackTrace(original.getStackTrace());
        return copy;
    }
}
