package com.thlee.stock.market.stockmarket.news.application;

import com.thlee.stock.market.stockmarket.news.application.dto.NewsResultDto;
import com.thlee.stock.market.stockmarket.news.application.dto.NewsSearchOutcome;
import com.thlee.stock.market.stockmarket.news.domain.model.NewsSearchResult;
import com.thlee.stock.market.stockmarket.news.domain.model.Region;
import com.thlee.stock.market.stockmarket.news.domain.service.NewsSearchPort;
import com.thlee.stock.market.stockmarket.news.domain.service.NewsSearchPortFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 뉴스 조회 유스케이스 서비스 (외부 API 조회)
 *
 * <p>포트를 순서대로 시도해 <b>처음으로 결과가 있는 포트</b>의 응답을 쓴다(폴백).
 *
 * <p>#115 부터 반환형이 {@link NewsSearchOutcome} 이다. 이전에는 모든 포트가 실패해도
 * 빈 리스트를 돌려줘서 "수집 실패"와 "새 기사 없음"을 호출부가 구분할 수 없었고,
 * 그래서 수집 이력에 실패를 남길 수 없었다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NewsSearchService {

    private final NewsSearchPortFactory portFactory;

    public NewsSearchOutcome search(String keyword, Region region) {
        LocalDateTime fromDateTime = LocalDate.now().atStartOfDay();
        List<NewsSearchPort> ports = portFactory.getPorts(region);
        if (ports.isEmpty()) {
            return NewsSearchOutcome.failure("사용 가능한 뉴스 검색 포트가 없습니다: region=" + region);
        }

        boolean anyPortResponded = false;
        String lastError = null;

        for (NewsSearchPort port : ports) {
            try {
                List<NewsSearchResult> results = port.search(keyword, fromDateTime);
                anyPortResponded = true;
                if (results != null && !results.isEmpty()) {
                    return NewsSearchOutcome.success(results.stream()
                            .map(NewsResultDto::from)
                            .collect(Collectors.toList()));
                }
            } catch (Exception e) {
                // 다음 포트로 폴백하되, 전부 실패했을 때 이력에 남길 원인을 잡아 둔다
                lastError = port.getClass().getSimpleName() + ": " + e.getMessage();
                log.warn("뉴스 검색 포트 실패: keyword={}, port={}, error={}",
                        keyword, port.getClass().getSimpleName(), e.getMessage());
            }
        }

        // 포트가 응답은 했는데 전부 빈 결과 = 수집 성공, 새 기사 없음
        return anyPortResponded
                ? NewsSearchOutcome.success(List.of())
                : NewsSearchOutcome.failure(lastError);
    }
}
