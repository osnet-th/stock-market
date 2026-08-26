package com.thlee.stock.market.stockmarket.glossary.application.dto;

import com.thlee.stock.market.stockmarket.glossary.domain.model.GlossaryTermContent;

import java.util.List;

/**
 * 용어 생성 명령.
 *
 * <p>카테고리 결정 우선순위:
 * <ol>
 *   <li>{@code categoryId} 가 non-null → ownership 검증 후 그 ID 사용</li>
 *   <li>{@code categoryId} null + {@code categoryName} non-blank → R13 인라인 find-or-create</li>
 *   <li>둘 다 null/blank → null (미분류)</li>
 * </ol>
 *
 * <p>{@code relatedTermIds} 는 service 에서 소유권/실존/자기 참조 정규화 후 도메인에 전달된다.
 */
public record CreateGlossaryTermCommand(
        String name,
        String abbreviation,
        String oneLine,
        String definition,
        String scaleNote,
        String example,
        String takeaway,
        Long categoryId,
        String categoryName,
        List<Long> relatedTermIds
) {
    /** 구조화 콘텐츠 묶음 — 도메인 VO 로 변환 (길이 검증은 VO 책임). */
    public GlossaryTermContent content() {
        return new GlossaryTermContent(abbreviation, oneLine, definition, scaleNote, example, takeaway);
    }
}
