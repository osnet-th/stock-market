package com.thlee.stock.market.stockmarket.glossary.application.support;

/**
 * SQL LIKE 와일드카드 이스케이프 + 검색 패턴 빌드 유틸 (R15).
 *
 * <p>backslash escape 문자(`\`)를 사용한다. JPQL/SQL 측에서는 {@code LIKE :pattern ESCAPE '\\'} 형태로
 * 호출돼야 한다.
 */
public final class LikeEscaper {

    /** 검색 입력 길이 상한 (DoS 완화) */
    public static final int MAX_QUERY_LENGTH = 100;

    private LikeEscaper() {
    }

    /**
     * 사용자 입력 검색어를 LIKE 패턴으로 변환한다.
     *
     * <ul>
     *   <li>null / blank → null 반환 (필터 비활성)</li>
     *   <li>입력 길이 상한 초과 → 앞에서부터 {@link #MAX_QUERY_LENGTH} 자까지만 사용</li>
     *   <li>와일드카드(`\`, `%`, `_`) 이스케이프 후 양쪽에 `%` 둘러쌈 (contains 검색)</li>
     * </ul>
     *
     * @return contains 검색 패턴 또는 {@code null}
     */
    public static String toContainsPattern(String input) {
        if (input == null) {
            return null;
        }
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > MAX_QUERY_LENGTH) {
            trimmed = trimmed.substring(0, MAX_QUERY_LENGTH);
        }
        return "%" + escapeWildcards(trimmed) + "%";
    }

    private static String escapeWildcards(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' || c == '%' || c == '_') {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.toString();
    }
}