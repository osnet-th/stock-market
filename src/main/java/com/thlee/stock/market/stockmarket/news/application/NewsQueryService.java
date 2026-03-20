package com.thlee.stock.market.stockmarket.news.application;

import com.thlee.stock.market.stockmarket.common.response.PageResult;
import com.thlee.stock.market.stockmarket.news.application.dto.NewsDto;
import com.thlee.stock.market.stockmarket.news.domain.model.News;
import com.thlee.stock.market.stockmarket.news.domain.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 저장된 뉴스 조회 유스케이스 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NewsQueryService {

    private final NewsRepository newsRepository;

    /**
     * 여러 URL 중 이미 존재하는 것 필터링
     */
    public List<String> findExistingUrls(List<String> originalLinks) {
        return originalLinks.stream()
                .filter(url -> newsRepository.findByOriginalUrl(url).isPresent())
                .collect(Collectors.toList());
    }

    /**
     * keywordId 기반 뉴스 조회
     */
    public PageResult<NewsDto> getNewsByKeywordId(Long keywordId, int page, int size) {
        PageResult<News> result = newsRepository.findByKeywordId(keywordId, page, size);

        List<NewsDto> dtoList = result.getContent().stream()
                .map(NewsDto::from)
                .collect(Collectors.toList());

        return new PageResult<>(dtoList, result.getPage(), result.getSize(), result.getTotalElements());
    }
}
