package com.thlee.stock.market.stockmarket.news.application;

import com.thlee.stock.market.stockmarket.news.application.dto.NewsBatchSaveResult;
import com.thlee.stock.market.stockmarket.news.application.dto.NewsSaveRequest;
import com.thlee.stock.market.stockmarket.news.application.dto.NewsResultDto;
import com.thlee.stock.market.stockmarket.news.domain.model.Keyword;
import com.thlee.stock.market.stockmarket.news.domain.model.NewsPurpose;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
        List<Keyword> activeKeywords = keywordService.getAllActiveKeywords();
        if (activeKeywords.isEmpty()) {
            return 0;
        }

        List<NewsResultDto> allSearchedNews = new ArrayList<>();
        for (Keyword keyword : activeKeywords) {
            List<NewsResultDto> searchResults = newsSearchService.search(keyword.getKeyword());
            allSearchedNews.addAll(searchResults);
        }

        if (allSearchedNews.isEmpty()) {
            return 0;
        }

        List<NewsResultDto> newNews = filterNewNews(allSearchedNews);
        if (newNews.isEmpty()) {
            return 0;
        }

        List<NewsSaveRequest> saveRequests = newNews.stream()
                .map(dto -> new NewsSaveRequest(
                        dto.getUrl(),
                        null,
                        dto.getTitle(),
                        dto.getContent(),
                        dto.getPublishedAt(),
                        null
                ))
                .collect(Collectors.toList());

        NewsBatchSaveResult result = newsSaveService.saveBatch(saveRequests, NewsPurpose.KEYWORD);
        return result.getSuccessCount();
    }

    private List<NewsResultDto> filterNewNews(List<NewsResultDto> searchedNews) {
        List<String> urls = searchedNews.stream()
                .map(NewsResultDto::getUrl)
                .collect(Collectors.toList());

        List<String> existingUrls = newsQueryService.findExistingUrls(urls);

        return searchedNews.stream()
                .filter(news -> !existingUrls.contains(news.getUrl()))
                .collect(Collectors.toList());
    }
}