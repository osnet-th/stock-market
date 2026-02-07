package com.thlee.stock.market.stockmarket.news.application;

import com.thlee.stock.market.stockmarket.news.application.dto.NewsResultDto;
import com.thlee.stock.market.stockmarket.news.domain.model.NewsSearchResult;
import com.thlee.stock.market.stockmarket.news.domain.service.NewsSearchPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 뉴스 조회 유스케이스 서비스 (외부 API 조회)
 */
@Service
@RequiredArgsConstructor
public class NewsSearchService {

    private final List<NewsSearchPort> searchPorts;

    public List<NewsResultDto> search(String keyword) {
        LocalDateTime fromDateTime = LocalDate.now().atStartOfDay();

        for (NewsSearchPort port : searchPorts) {
            try {
                List<NewsSearchResult> results = port.search(keyword, fromDateTime);
                if (results != null && !results.isEmpty()) {
                    return results.stream()
                            .map(NewsResultDto::from)
                            .collect(Collectors.toList());
                }
            } catch (Exception ignored) {
                // 다음 포트로 폴백
            }
        }

        return Collections.emptyList();
    }
}
