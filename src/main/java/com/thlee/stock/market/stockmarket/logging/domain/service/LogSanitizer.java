package com.thlee.stock.market.stockmarket.logging.domain.service;

import com.thlee.stock.market.stockmarket.logging.domain.model.ApplicationLog;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 로그 적재 직전에 payload 를 정제한다.
 *
 * 순서 (Data Integrity Policy):
 *  1. 민감 정보 마스킹 (Bearer, Authorization 헤더, API 키, 패스워드 JSON 필드, 주민번호 등)
 *  2. CRLF 치환 (log injection 방어)
 *  3. 스택트레이스 20줄 상한
 *  4. 문자열 필드 2KB 상한
 *  5. 문서 전체 합계 16KB 상한 (초과 시 {@code truncated=true}, {@code originalSize} 기록)
 *
 * 마스킹이 truncation 보다 먼저 — 부분 시크릿 노출 방지.
 * Regex 는 possessive / bounded quantifier 로 ReDoS 방어.
 */
public final class LogSanitizer {

    private static final int MAX_STRING_FIELD_BYTES = 2 * 1024;     // 2KB
    private static final int MAX_DOCUMENT_BYTES = 16 * 1024;        // 16KB
    private static final int MAX_STACK_TRACE_LINES = 20;
    private static final int MAX_MASK_INPUT = 8192;
    private static final String TRUNCATED_MARKER = "...[truncated]";

    private static final Pattern CRLF = Pattern.compile("[\\r\\n]");

    private record MaskRule(Pattern pattern, String replacement) {
    }

    private static final List<MaskRule> MASK_RULES = List.of(
            new MaskRule(Pattern.compile("(?i)Bearer\\s++[A-Za-z0-9\\-._~+/]{10,500}+=*+"), "Bearer ***"),
            new MaskRule(Pattern.compile("(?i)(authorization|cookie|set-cookie)\\s*+[:=]\\s*+\\S{1,500}+"), "$1: ***"),
            new MaskRule(Pattern.compile("\\b(sk|pk)_(live|test)_[A-Za-z0-9]{10,64}+"), "$1_$2_***"),
            new MaskRule(Pattern.compile("(?i)\"(password|passwd|token|api[_-]?key|secret|access[_-]?token|refresh[_-]?token|client[_-]?secret)\"\\s*+:\\s*+\"[^\"]{1,500}+\""), "\"$1\":\"***\""),
            new MaskRule(Pattern.compile("(?i)([?&](?:api[_-]?key|access[_-]?token|password|token)=)[^&\\s]{1,500}+"), "$1***"),
            new MaskRule(Pattern.compile("\\b\\d{6}+[-]\\d{7}+\\b"), "******-*******"),
            new MaskRule(Pattern.compile("\\b01[016789][-\\s]?+\\d{3,4}+[-\\s]?+\\d{4}+\\b"), "010-****-****")
    );

    public ApplicationLog sanitize(ApplicationLog log) {
        if (log == null) {
            return null;
        }
        Map<String, Object> sanitizedPayload = sanitizePayload(log.payload());
        int originalBytes = estimateBytes(log.payload());
        int sanitizedBytes = estimateBytes(sanitizedPayload);
        boolean truncated = log.truncated();
        Integer originalSize = log.originalSize();

        if (sanitizedBytes > MAX_DOCUMENT_BYTES) {
            sanitizedPayload = truncateDocument(sanitizedPayload);
            truncated = true;
            originalSize = originalBytes;
        }

        return new ApplicationLog(
                log.timestamp(),
                log.domain(),
                log.userId(),
                log.requestId(),
                sanitizedPayload,
                truncated,
                originalSize
        );
    }

    private Map<String, Object> sanitizePayload(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return payload;
        }
        Map<String, Object> out = new LinkedHashMap<>(payload.size());
        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            out.put(entry.getKey(), sanitizeValue(entry.getKey(), entry.getValue()));
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private Object sanitizeValue(String key, Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String s) {
            return sanitizeString(key, s);
        }
        if (value instanceof List<?> list) {
            List<Object> out = new ArrayList<>(list.size());
            for (Object item : list) {
                out.add(sanitizeValue(key, item));
            }
            return out;
        }
        if (value instanceof Map<?, ?> map) {
            return sanitizePayload((Map<String, Object>) map);
        }
        return value;
    }

    private String sanitizeString(String key, String raw) {
        if (raw.isEmpty()) {
            return raw;
        }
        String masked = mask(raw);
        String noCrlf = CRLF.matcher(masked).replaceAll("_");
        String stackTrimmed = "stackTrace".equalsIgnoreCase(key) ? trimStackTrace(noCrlf) : noCrlf;
        return truncateField(stackTrimmed);
    }

    private String mask(String raw) {
        String input = raw.length() > MAX_MASK_INPUT ? raw.substring(0, MAX_MASK_INPUT) : raw;
        String out = input;
        for (MaskRule rule : MASK_RULES) {
            out = rule.pattern().matcher(out).replaceAll(rule.replacement());
        }
        // 원본이 상한을 넘으면 잘린 부분은 복구 불가 — 안전하게 마킹
        return raw.length() > MAX_MASK_INPUT ? out + TRUNCATED_MARKER : out;
    }

    private String trimStackTrace(String stack) {
        String[] lines = stack.split("\\n", -1);
        if (lines.length <= MAX_STACK_TRACE_LINES) {
            return stack;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < MAX_STACK_TRACE_LINES; i++) {
            sb.append(lines[i]);
            if (i < MAX_STACK_TRACE_LINES - 1) {
                sb.append('\n');
            }
        }
        sb.append("\n... [").append(lines.length - MAX_STACK_TRACE_LINES).append(" more lines truncated]");
        return sb.toString();
    }

    private String truncateField(String value) {
        byte[] bytes = value.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (bytes.length <= MAX_STRING_FIELD_BYTES) {
            return value;
        }
        String truncated = new String(bytes, 0, MAX_STRING_FIELD_BYTES, java.nio.charset.StandardCharsets.UTF_8);
        return truncated + TRUNCATED_MARKER;
    }

    private Map<String, Object> truncateDocument(Map<String, Object> payload) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("_truncated", true);
        // key 목록만 보존, 값은 요약
        out.put("_keys", new ArrayList<>(payload.keySet()));
        return out;
    }

    private int estimateBytes(Map<String, Object> payload) {
        if (payload == null) {
            return 0;
        }
        int total = 0;
        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            total += entry.getKey().length();
            total += estimateValueBytes(entry.getValue());
        }
        return total;
    }

    @SuppressWarnings("unchecked")
    private int estimateValueBytes(Object value) {
        if (value == null) {
            return 4;
        }
        if (value instanceof String s) {
            return s.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
        }
        if (value instanceof Number || value instanceof Boolean) {
            return 16;
        }
        if (value instanceof Map<?, ?> map) {
            return estimateBytes((Map<String, Object>) map);
        }
        if (value instanceof List<?> list) {
            int total = 0;
            for (Object item : list) {
                total += estimateValueBytes(item);
            }
            return total;
        }
        return value.toString().length();
    }
}