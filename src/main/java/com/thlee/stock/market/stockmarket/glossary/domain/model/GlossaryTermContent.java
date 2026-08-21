package com.thlee.stock.market.stockmarket.glossary.domain.model;

/**
 * 용어 본문 콘텐츠 VO — 구조화 필드 묶음 (마스터-디테일 리디자인).
 *
 * <p>모든 필드는 선택(null 허용)이며 길이 상한만 검증한다.
 * <ul>
 *   <li>{@code abbreviation} — 약어 · 영문 (예: "CPI · Consumer Price Index")</li>
 *   <li>{@code oneLine} — 한 줄 정의. 목록/함께 볼 용어 카드에는 이 문장만 노출된다</li>
 *   <li>{@code definition} — 풀이 본문. 기존 '설명' 컬럼을 rename 없이 재해석
 *       (ddl-auto: update 환경에서 rename 은 데이터 고아화)</li>
 *   <li>{@code scaleNote} — 기준 · 읽는 법</li>
 *   <li>{@code example} — 예시</li>
 *   <li>{@code takeaway} — 투자 관점</li>
 * </ul>
 */
public record GlossaryTermContent(
        String abbreviation,
        String oneLine,
        String definition,
        String scaleNote,
        String example,
        String takeaway
) {

    /** 약어 · 영문 길이 상한 */
    public static final int ABBREVIATION_MAX_LENGTH = 200;

    /** 한 줄 정의 길이 상한 (한 문장 유도) */
    public static final int ONE_LINE_MAX_LENGTH = 300;

    /** 장문 섹션(풀이/기준/예시/투자 관점) 공통 길이 상한 */
    public static final int LONG_TEXT_MAX_LENGTH = 4000;

    public GlossaryTermContent {
        requireWithin(abbreviation, ABBREVIATION_MAX_LENGTH, "abbreviation");
        requireWithin(oneLine, ONE_LINE_MAX_LENGTH, "oneLine");
        requireWithin(definition, LONG_TEXT_MAX_LENGTH, "definition");
        requireWithin(scaleNote, LONG_TEXT_MAX_LENGTH, "scaleNote");
        requireWithin(example, LONG_TEXT_MAX_LENGTH, "example");
        requireWithin(takeaway, LONG_TEXT_MAX_LENGTH, "takeaway");
    }

    /** 전 필드 미작성 콘텐츠 */
    public static GlossaryTermContent empty() {
        return new GlossaryTermContent(null, null, null, null, null, null);
    }

    private static void requireWithin(String value, int maxLength, String fieldName) {
        if (value != null && value.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " 길이는 " + maxLength + "자 이하여야 합니다.");
        }
    }
}
