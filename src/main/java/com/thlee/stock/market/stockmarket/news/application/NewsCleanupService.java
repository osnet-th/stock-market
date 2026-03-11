package com.thlee.stock.market.stockmarket.news.application;

import com.thlee.stock.market.stockmarket.news.domain.model.NewsPurpose;
import com.thlee.stock.market.stockmarket.news.domain.repository.NewsRepository;
import com.thlee.stock.market.stockmarket.news.domain.repository.NewsSourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 뉴스 출처 매핑 삭제 및 orphan 뉴스 정리 서비스
 */
@Service
@RequiredArgsConstructor
public class NewsCleanupService {

    private final NewsSourceRepository newsSourceRepository;
    private final NewsRepository newsRepository;

    /**
     * 뉴스 출처 매핑 삭제 + 아무도 참조하지 않는 orphan 뉴스 정리
     */
    public void deleteSourceAndCleanOrphans(NewsPurpose purpose, Long sourceId) {
        // 1. 삭제 대상 newsId 목록 조회
        List<Long> newsIds = newsSourceRepository.findNewsIdsByPurposeAndSourceId(purpose, sourceId);

        // 2. 뉴스 출처 매핑 삭제
        newsSourceRepository.deleteByPurposeAndSourceId(purpose, sourceId);

        // 3. 다른 사용자가 참조하지 않는 orphan 뉴스만 삭제
        if (!newsIds.isEmpty()) {
            List<Long> stillReferenced = newsSourceRepository.findNewsIdsWithSources(newsIds);
            List<Long> orphanIds = newsIds.stream()
                    .filter(id -> !stillReferenced.contains(id))
                    .toList();
            newsRepository.deleteByIds(orphanIds);
        }
    }
}