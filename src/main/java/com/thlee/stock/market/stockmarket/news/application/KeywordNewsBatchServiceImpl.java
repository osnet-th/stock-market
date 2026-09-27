package com.thlee.stock.market.stockmarket.news.application;

import com.thlee.stock.market.stockmarket.news.application.dto.NewsBatchSaveResult;
import com.thlee.stock.market.stockmarket.news.application.dto.NewsSaveRequest;
import com.thlee.stock.market.stockmarket.news.application.dto.NewsResultDto;
import com.thlee.stock.market.stockmarket.news.application.dto.NewsSearchOutcome;
import com.thlee.stock.market.stockmarket.news.application.vo.KeywordSearchContext;
import com.thlee.stock.market.stockmarket.news.domain.model.Keyword;
import com.thlee.stock.market.stockmarket.news.domain.model.KeywordCollectionHistory;
import com.thlee.stock.market.stockmarket.news.domain.model.Region;
import com.thlee.stock.market.stockmarket.news.domain.repository.KeywordCollectionHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 키워드 뉴스 배치 조회 서비스 구현
 *
 * <p>#115 부터 키워드마다 <b>수집 시도 이력</b>을 남긴다. 이력이 연속 실패 판정과
 * "마지막 성공" 표시의 근거다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KeywordNewsBatchServiceImpl implements KeywordNewsBatchService {

    private final KeywordService keywordService;
    private final NewsSearchService newsSearchService;
    private final NewsSaveService newsSaveService;
    private final NewsQueryService newsQueryService;
    private final KeywordCollectionHistoryRepository collectionHistoryRepository;

    @Override
    public int executeKeywordNewsBatch() {
        List<Keyword> allKeywords = keywordService.getAllKeywords();
        if (allKeywords.isEmpty()) {
            return 0;
        }

        List<KeywordSearchContext> searchedContexts = new ArrayList<>();
        // 조회에 성공한 키워드만 모은다 — 실패한 키워드는 여기서 이력을 남기고 건너뛴다
        List<Long> succeededKeywordIds = new ArrayList<>();

        for (Keyword keyword : allKeywords) {
            NewsSearchOutcome outcome = newsSearchService.search(keyword.getKeyword(), keyword.getRegion());
            if (!outcome.succeeded()) {
                recordHistory(KeywordCollectionHistory.failure(keyword.getId(), outcome.errorMessage()));
                continue;
            }

            succeededKeywordIds.add(keyword.getId());
            for (NewsResultDto dto : outcome.results()) {
                searchedContexts.add(new KeywordSearchContext(
                        keyword.getId(),
                        keyword.getRegion(),
                        dto
                ));
            }
        }

        List<KeywordSearchContext> newContexts = searchedContexts.isEmpty()
                ? List.of()
                : filterNewNews(searchedContexts);

        int saved = 0;
        if (!newContexts.isEmpty()) {
            NewsBatchSaveResult result = newsSaveService.saveBatch(toSaveRequests(newContexts));
            saved = result.getSuccessCount();
        }

        recordBatchSuccessHistories(succeededKeywordIds, searchedContexts, newContexts);
        return saved;
    }

    @Override
    public NewsBatchSaveResult collectByKeyword(Long keywordId, String keyword, Region region) {
        NewsSearchOutcome outcome = newsSearchService.search(keyword, region);
        if (!outcome.succeeded()) {
            recordHistory(KeywordCollectionHistory.failure(keywordId, outcome.errorMessage()));
            return new NewsBatchSaveResult(0, 0, 0);
        }

        List<NewsResultDto> searchResults = outcome.results();
        if (searchResults.isEmpty()) {
            // 수집 자체는 성공했고 새 기사만 없었다 — 이력은 성공으로 남긴다
            recordHistory(KeywordCollectionHistory.success(keywordId, 0, 0));
            return new NewsBatchSaveResult(0, 0, 0);
        }

        List<KeywordSearchContext> contexts = searchResults.stream()
                .map(dto -> new KeywordSearchContext(keywordId, region, dto))
                .toList();

        List<KeywordSearchContext> newContexts = filterNewNews(contexts);
        if (newContexts.isEmpty()) {
            recordHistory(KeywordCollectionHistory.success(keywordId, 0, searchResults.size()));
            return new NewsBatchSaveResult(0, 0, 0);
        }

        NewsBatchSaveResult result = newsSaveService.saveBatch(toSaveRequests(newContexts));
        recordHistory(KeywordCollectionHistory.success(
                keywordId, result.getSuccessCount(), result.getIgnoredCount()));
        return result;
    }

    /**
     * 배치는 전 키워드를 한 번에 저장하므로 저장 결과를 키워드별로 쪼갤 수 없다.
     * 대신 키워드별로 <b>새로 저장 요청한 건수</b>와 <b>이미 있어 건너뛴 건수</b>를 남긴다 —
     * 이력의 핵심인 성공/실패 판정과 시각에는 영향이 없다.
     */
    private void recordBatchSuccessHistories(List<Long> succeededKeywordIds,
                                             List<KeywordSearchContext> searchedContexts,
                                             List<KeywordSearchContext> newContexts) {
        for (Long keywordId : succeededKeywordIds) {
            long fetched = searchedContexts.stream()
                    .filter(c -> keywordId.equals(c.keywordId()))
                    .count();
            long fresh = newContexts.stream()
                    .filter(c -> keywordId.equals(c.keywordId()))
                    .count();
            recordHistory(KeywordCollectionHistory.success(
                    keywordId, (int) fresh, (int) (fetched - fresh)));
        }
    }

    /**
     * 이력 저장 실패가 수집 자체를 깨뜨리면 안 된다 — 기록은 부가 기능이다.
     */
    private void recordHistory(KeywordCollectionHistory history) {
        try {
            collectionHistoryRepository.save(history);
        } catch (Exception e) {
            log.warn("수집 이력 저장 실패: keywordId={}, error={}", history.getKeywordId(), e.getMessage());
        }
    }

    private List<NewsSaveRequest> toSaveRequests(List<KeywordSearchContext> contexts) {
        return contexts.stream()
                .map(context -> new NewsSaveRequest(
                        context.news().getUrl(),
                        context.news().getTitle(),
                        context.news().getContent(),
                        context.news().getPublishedAt(),
                        context.keywordId(),
                        context.region(),
                        context.news().getSource()
                ))
                .toList();
    }

    private List<KeywordSearchContext> filterNewNews(List<KeywordSearchContext> searchedContexts) {
        List<String> urls = searchedContexts.stream()
                .map(context -> context.news().getUrl())
                .distinct()
                .toList();

        List<String> existingUrls = newsQueryService.findExistingUrls(urls);
        Set<String> existingUrlSet = new HashSet<>(existingUrls);
        Set<String> selectedUrls = new HashSet<>();

        return searchedContexts.stream()
                .filter(context -> !existingUrlSet.contains(context.news().getUrl()))
                .filter(context -> selectedUrls.add(context.news().getUrl()))
                .toList();
    }
}
