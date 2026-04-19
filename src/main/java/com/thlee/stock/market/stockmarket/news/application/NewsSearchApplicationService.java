package com.thlee.stock.market.stockmarket.news.application;

import com.thlee.stock.market.stockmarket.common.response.PageResult;
import com.thlee.stock.market.stockmarket.news.application.dto.NewsDto;
import com.thlee.stock.market.stockmarket.news.domain.model.News;
import com.thlee.stock.market.stockmarket.news.domain.model.Region;
import com.thlee.stock.market.stockmarket.news.domain.service.NewsFullTextSearchPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 뉴스 전문 검색 유스케이스 서비스
 */
@Service
@RequiredArgsConstructor
public class NewsSearchApplicationService {

    private final NewsFullTextSearchPort newsFullTextSearchPort;

    public PageResult<NewsDto> search(String query, LocalDate startDate, LocalDate endDate,
                                      Region region, int page, int size) {
        PageResult<News> result = newsFullTextSearchPort.search(query, startDate, endDate, region, page, size);

        List<NewsDto> dtoList = result.getContent().stream()
                .map(NewsDto::from)
                .toList();

        return new PageResult<>(dtoList, result.getPage(), result.getSize(), result.getTotalElements());
    }
}