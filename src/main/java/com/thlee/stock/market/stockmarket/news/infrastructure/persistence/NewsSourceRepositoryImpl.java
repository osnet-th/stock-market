package com.thlee.stock.market.stockmarket.news.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.common.response.PageResult;
import com.thlee.stock.market.stockmarket.news.domain.model.NewsSource;
import com.thlee.stock.market.stockmarket.news.domain.model.NewsPurpose;
import com.thlee.stock.market.stockmarket.news.domain.repository.NewsSourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class NewsSourceRepositoryImpl implements NewsSourceRepository {

    private final NewsSourceJpaRepository newsSourceJpaRepository;

    @Override
    public boolean insertIgnoreDuplicate(NewsSource newsSource) {
        int inserted = newsSourceJpaRepository.insertIgnoreDuplicate(
                newsSource.getNewsId(),
                newsSource.getUserId(),
                newsSource.getPurpose().name(),
                newsSource.getSourceId(),
                newsSource.getCreatedAt()
        );
        return inserted > 0;
    }

    @Override
    public List<String> findExistingUrlsByUser(List<String> urls, Long userId) {
        return newsSourceJpaRepository.findExistingUrlsByUser(urls, userId);
    }

    @Override
    public PageResult<Long> findNewsIdsByPurposeAndSourceId(NewsPurpose purpose, Long sourceId, int page, int size) {
        Page<NewsSourceEntity> entityPage = newsSourceJpaRepository
                .findByPurposeAndSourceIdOrderByCreatedAtDesc(purpose, sourceId, PageRequest.of(page, size));

        List<Long> newsIds = entityPage.getContent().stream()
                .map(NewsSourceEntity::getNewsId)
                .toList();

        return new PageResult<>(newsIds, page, size, entityPage.getTotalElements());
    }

    @Override
    public List<Long> findNewsIdsByPurposeAndSourceId(NewsPurpose purpose, Long sourceId) {
        return newsSourceJpaRepository.findNewsIdsByPurposeAndSourceId(purpose, sourceId);
    }

    @Override
    public List<Long> findNewsIdsWithSources(List<Long> newsIds) {
        if (newsIds.isEmpty()) {
            return List.of();
        }
        return newsSourceJpaRepository.findNewsIdsWithSources(newsIds);
    }

    @Override
    public void deleteByPurposeAndSourceId(NewsPurpose purpose, Long sourceId) {
        newsSourceJpaRepository.deleteByPurposeAndSourceId(purpose, sourceId);
    }
}