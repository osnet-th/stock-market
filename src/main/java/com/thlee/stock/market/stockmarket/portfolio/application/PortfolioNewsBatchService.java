package com.thlee.stock.market.stockmarket.portfolio.application;

import com.thlee.stock.market.stockmarket.news.application.NewsQueryService;
import com.thlee.stock.market.stockmarket.news.application.NewsSaveService;
import com.thlee.stock.market.stockmarket.news.application.NewsSearchService;
import com.thlee.stock.market.stockmarket.news.application.dto.NewsBatchSaveResult;
import com.thlee.stock.market.stockmarket.news.application.dto.NewsResultDto;
import com.thlee.stock.market.stockmarket.news.application.dto.NewsSaveRequest;
import com.thlee.stock.market.stockmarket.news.domain.model.NewsPurpose;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.PortfolioItem;
import com.thlee.stock.market.stockmarket.portfolio.domain.repository.PortfolioItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 포트폴리오 뉴스 배치 수집 서비스
 */
@Service
@RequiredArgsConstructor
public class PortfolioNewsBatchService {

    private final PortfolioItemRepository portfolioItemRepository;
    private final NewsSearchService newsSearchService;
    private final NewsSaveService newsSaveService;
    private final NewsQueryService newsQueryService;

    /**
     * 뉴스 활성화된 포트폴리오 항목 전체 뉴스 수집
     */
    public int collectNews() {
        List<PortfolioItem> items = portfolioItemRepository.findByNewsEnabled(true);
        if (items.isEmpty()) {
            return 0;
        }

        List<PortfolioNewsContext> searchedContexts = new ArrayList<>();
        for (PortfolioItem item : items) {
            List<NewsResultDto> searchResults = newsSearchService.search(item.getItemName(), item.getRegion());
            for (NewsResultDto dto : searchResults) {
                searchedContexts.add(new PortfolioNewsContext(
                        item.getUserId(),
                        item.getId(),
                        item.getRegion(),
                        dto
                ));
            }
        }

        if (searchedContexts.isEmpty()) {
            return 0;
        }

        List<PortfolioNewsContext> newContexts = filterNewNews(searchedContexts);
        if (newContexts.isEmpty()) {
            return 0;
        }

        List<NewsSaveRequest> saveRequests = newContexts.stream()
                .map(context -> new NewsSaveRequest(
                        context.news().getUrl(),
                        context.userId(),
                        context.news().getTitle(),
                        context.news().getContent(),
                        context.news().getPublishedAt(),
                        context.itemId(),
                        context.region()
                ))
                .toList();

        NewsBatchSaveResult result = newsSaveService.saveBatch(saveRequests, NewsPurpose.PORTFOLIO);
        return result.getSuccessCount();
    }

    private List<PortfolioNewsContext> filterNewNews(List<PortfolioNewsContext> searchedContexts) {
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