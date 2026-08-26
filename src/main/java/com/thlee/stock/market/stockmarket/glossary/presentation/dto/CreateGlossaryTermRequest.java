package com.thlee.stock.market.stockmarket.glossary.presentation.dto;

import com.thlee.stock.market.stockmarket.glossary.application.dto.CreateGlossaryTermCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 용어 생성 Request.
 *
 * <p>{@code categoryId} / {@code categoryName} 동시 입력 시 categoryId 우선 (service 정규화).
 * 구조화 콘텐츠(약어/한 줄 정의/풀이/기준/예시/투자 관점)와 함께 볼 용어는 전부 선택.
 * {@code definition} 은 화면에서 '풀이' 로 노출된다 (필드명은 기존 API 하위호환 유지).
 */
public record CreateGlossaryTermRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 200) String abbreviation,
        @Size(max = 300) String oneLine,
        @Size(max = 4000) String definition,
        @Size(max = 4000) String scaleNote,
        @Size(max = 4000) String example,
        @Size(max = 4000) String takeaway,
        Long categoryId,
        @Size(max = 50) String categoryName,
        @Size(max = 20) List<Long> relatedTermIds
) {
    public CreateGlossaryTermCommand toCommand() {
        return new CreateGlossaryTermCommand(
                name, abbreviation, oneLine, definition, scaleNote, example, takeaway,
                categoryId, categoryName, relatedTermIds
        );
    }
}
