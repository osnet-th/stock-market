package com.thlee.stock.market.stockmarket.newsjournal.presentation.dto;

import com.thlee.stock.market.stockmarket.newsjournal.application.dto.NewsEventLinkCommand;
import com.thlee.stock.market.stockmarket.newsjournal.application.dto.UpdateNewsEventCommand;
import com.thlee.stock.market.stockmarket.newsjournal.domain.model.EventCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * 사건 수정 Request. id 는 path 에서, userId 는 SecurityContext 에서 주입한다.
 */
public record UpdateNewsEventRequest(
        @NotBlank @Size(max = 200) String title,
        @NotNull LocalDate occurredDate,
        @NotNull EventCategory category,
        @Size(max = 4000) String what,
        @Size(max = 4000) String why,
        @Size(max = 4000) String how,
        @Valid @Size(max = 20, message = "links 는 최대 20개까지 허용됩니다.") List<NewsEventLinkDto> links
) {

    public UpdateNewsEventCommand toCommand(Long id, Long userId) {
        List<NewsEventLinkCommand> linkCommands = links == null ? List.of() : links.stream()
                .map(l -> new NewsEventLinkCommand(l.title(), l.url()))
                .toList();
        return new UpdateNewsEventCommand(
                id, userId, title, occurredDate, category, what, why, how, linkCommands
        );
    }
}