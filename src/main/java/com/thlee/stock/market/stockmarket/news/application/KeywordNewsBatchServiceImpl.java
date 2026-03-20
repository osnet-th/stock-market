package com.thlee.stock.market.stockmarket.news.application;

import com.thlee.stock.market.stockmarket.news.application.dto.NewsBatchSaveResult;
import com.thlee.stock.market.stockmarket.news.application.dto.NewsSaveRequest;
import com.thlee.stock.market.stockmarket.news.application.dto.NewsResultDto;
import com.thlee.stock.market.stockmarket.news.application.vo.KeywordSearchContext;
import com.thlee.stock.market.stockmarket.news.domain.model.Keyword;
import com.thlee.stock.market.stockmarket.news.domain.model.Region;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 키워드 뉴스 배치 조회 서비스 구현
 */
@Service
@RequiredArgsConstructor
public class KeywordNewsBatchServiceImpl implements KeywordNewsBatchService {

    private final KeywordService keywordService;
    private final NewsSearchService newsSearchService;
    private final NewsSaveService newsSaveService;
    private final NewsQueryService newsQueryService;

    @Override
    public int executeKeywordNewsBatch() {
        List<Keyword> allKeywords = keywordService.getAllKeywords();
        if (allKeywords.isEmpty()) {
            return 0;
        }

        List<KeywordSearchContext> searchedContexts = new ArrayList<>();
        for (Keyword keyword : allKeywords) {
            List<NewsResultDto> searchResults = newsSearchService.search(keyword.getKeyword(), keyword.getRegion());
            for (NewsResultDto dto : searchResults) {
                searchedContexts.add(new KeywordSearchContext(
                        keyword.getId(),
                        keyword.getRegion(),
                        dto
                ));
            }
        }

        if (searchedContexts.isEmpty()) {
            return 0;
        }

        List<KeywordSearchContext> newContexts = filterNewNews(searchedContexts);
        if (newContexts.isEmpty()) {
            return 0;
        }

        List<NewsSaveRequest> saveRequests = newContexts.stream()
                .map(context -> new NewsSaveRequest(
                        context.news().getUrl(),
                        context.news().getTitle(),
                        context.news().getContent(),
                        context.news().getPublishedAt(),
                        context.keywordId(),
                        context.region()
                ))
                .toList();

        NewsBatchSaveResult result = newsSaveService.saveBatch(saveRequests);
        return result.getSuccessCount();
    }

    @Override
    public NewsBatchSaveResult collectByKeyword(Long keywordId, String keyword, Region region) {
        List<NewsResultDto> searchResults = newsSearchService.search(keyword, region);
        if (searchResults.isEmpty()) {
            return new NewsBatchSaveResult(0, 0, 0);
        }

        List<KeywordSearchContext> contexts = searchResults.stream()
                .map(dto -> new KeywordSearchContext(keywordId, region, dto))
                .toList();

        List<KeywordSearchContext> newContexts = filterNewNews(contexts);
        if (newContexts.isEmpty()) {
            return new NewsBatchSaveResult(0, 0, 0);
        }

        List<NewsSaveRequest> saveRequests = newContexts.stream()
                .map(context -> new NewsSaveRequest(
                        context.news().getUrl(),
                        context.news().getTitle(),
                        context.news().getContent(),
                        context.news().getPublishedAt(),
                        context.keywordId(),
                        context.region()
                ))
                .toList();

        return newsSaveService.saveBatch(saveRequests);
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
