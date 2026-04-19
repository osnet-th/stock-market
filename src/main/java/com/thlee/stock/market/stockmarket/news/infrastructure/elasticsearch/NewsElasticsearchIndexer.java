package com.thlee.stock.market.stockmarket.news.infrastructure.elasticsearch;

import com.thlee.stock.market.stockmarket.news.domain.model.News;
import com.thlee.stock.market.stockmarket.news.domain.service.NewsIndexPort;
import com.thlee.stock.market.stockmarket.news.infrastructure.elasticsearch.document.NewsDocument;
import com.thlee.stock.market.stockmarket.news.infrastructure.elasticsearch.mapper.NewsDocumentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * ES 인덱싱 어댑터 — NewsIndexPort 구현
 * ES 장애 시 예외를 전파하지 않고 로그만 남긴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NewsElasticsearchIndexer implements NewsIndexPort {

    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public int indexAll(List<News> newsList) {
        if (newsList == null || newsList.isEmpty()) {
            return 0;
        }

        try {
            List<NewsDocument> documents = newsList.stream()
                    .map(NewsDocumentMapper::toDocument)
                    .toList();

            elasticsearchOperations.save(documents);
            log.info("ES 뉴스 인덱싱 완료: {}건", documents.size());
            return documents.size();
        } catch (Exception e) {
            log.warn("ES 뉴스 인덱싱 실패 ({}건): {}", newsList.size(), e.getMessage());
            return 0;
        }
    }
}