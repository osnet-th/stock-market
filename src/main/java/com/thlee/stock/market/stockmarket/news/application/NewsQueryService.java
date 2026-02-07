package com.thlee.stock.market.stockmarket.news.application;

import com.thlee.stock.market.stockmarket.news.application.dto.NewsDto;
import com.thlee.stock.market.stockmarket.news.domain.model.News;
import com.thlee.stock.market.stockmarket.news.domain.model.NewsPurpose;
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

    public List<NewsDto> getByPurpose(NewsPurpose purpose) {
        List<News> results = newsRepository.findByPurpose(purpose);
        return results.stream()
                .map(NewsDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 여러 URL 중 이미 존재하는 것 필터링
     * @param originalLinks 확인할 URL 목록
     * @return 이미 존재하는 URL 목록
     */
    public List<String> findExistingUrls(List<String> originalLinks) {
        return originalLinks.stream()
                .filter(url -> newsRepository.findByOriginalUrl(url).isPresent())
                .collect(Collectors.toList());
    }
}
