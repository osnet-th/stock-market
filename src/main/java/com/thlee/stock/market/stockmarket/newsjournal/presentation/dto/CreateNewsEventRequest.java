package com.thlee.stock.market.stockmarket.newsjournal.presentation.dto;

import com.thlee.stock.market.stockmarket.newsjournal.application.dto.CreateNewsEventCommand;
import com.thlee.stock.market.stockmarket.newsjournal.application.dto.NewsEventLinkCommand;
import com.thlee.stock.market.stockmarket.newsjournal.domain.model.EventImpact;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * 사건 생성 Request.
 *
 * <p>{@code category} 필드는 *주제 분류명* 으로 사용된다 (시장영향은 {@code impact} 로 분리).
 * {@code keywords} 는 해시태그형 키워드 목록 (선택, 각 50자 이하, 최대 20개).
 */
public record CreateNewsEventRequest(
        @NotBlank @Size(max = 200) String title,
        @NotNull LocalDate occurredDate,
        @NotNull EventImpact impact,
        @NotBlank @Size(max = 50) String category,
        @Size(max = 4000) String what,
        @Size(max = 4000) String why,
        @Size(max = 4000) String how,
        @Valid @Size(max = 20, message = "links 는 최대 20개까지 허용됩니다.") List<NewsEventLinkDto> links,
        @Size(max = 20, message = "keywords 는 최대 20개까지 허용됩니다.")
        List<@NotBlank @Size(max = 50) String> keywords
) {

    public CreateNewsEventCommand toCommand(Long userId) {
        List<NewsEventLinkCommand> linkCommands = links == null ? List.of() : links.stream()
                .map(l -> new NewsEventLinkCommand(l.title(), l.url()))
                .toList();
        List<String> keywordValues = keywords == null ? List.of() : keywords;
        return new CreateNewsEventCommand(
                userId, title, occurredDate, impact, category, what, why, how, linkCommands, keywordValues
        );
    }
}