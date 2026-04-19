package com.thlee.stock.market.stockmarket.news.application;

import com.thlee.stock.market.stockmarket.news.application.dto.NewsBatchSaveResult;
import com.thlee.stock.market.stockmarket.news.application.dto.NewsDto;
import com.thlee.stock.market.stockmarket.news.application.dto.NewsSaveRequest;
import com.thlee.stock.market.stockmarket.news.domain.model.News;
import com.thlee.stock.market.stockmarket.news.domain.repository.NewsRepository;
import com.thlee.stock.market.stockmarket.news.domain.service.NewsIndexPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * 뉴스 저장 유스케이스 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NewsSaveService {

    private final NewsRepository newsRepository;
    private final PlatformTransactionManager transactionManager;
    private final NewsIndexPort newsIndexPort;

    @Value("${spring.jpa.properties.hibernate.jdbc.batch_size:1000}")
    private int batchSize;

    @Transactional
    public NewsDto save(NewsSaveRequest request) {
        News news = News.create(
                request.getOriginalUrl(),
                request.getTitle(),
                request.getContent(),
                request.getPublishedAt(),
                request.getKeywordId(),
                request.getRegion()
        );

        News saved = newsRepository.save(news);
        return NewsDto.from(saved);
    }

    public NewsBatchSaveResult saveBatch(List<NewsSaveRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return new NewsBatchSaveResult(0, 0, 0);
        }

        int success = 0;
        int ignored = 0;
        int failed = 0;

        List<List<NewsSaveRequest>> chunks = splitByBatchSize(requests, batchSize);
        for (List<NewsSaveRequest> chunk : chunks) {
            TransactionTemplate template = new TransactionTemplate(transactionManager);
            template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

            ChunkSaveResult chunkResult = template.execute(status -> saveChunk(chunk));
            if (chunkResult != null) {
                success += chunkResult.result().getSuccessCount();
                ignored += chunkResult.result().getIgnoredCount();
                failed += chunkResult.result().getFailedCount();

                // ES 인덱싱은 트랜잭션 밖에서 수행
                if (!chunkResult.insertedNews().isEmpty()) {
                    newsIndexPort.indexAll(chunkResult.insertedNews());
                }
            }
        }

        return new NewsBatchSaveResult(success, ignored, failed);
    }

    private ChunkSaveResult saveChunk(List<NewsSaveRequest> chunk) {
        int success = 0;
        int ignored = 0;
        int failed = 0;
        List<News> insertedNews = new ArrayList<>();

        for (NewsSaveRequest request : chunk) {
            try {
                News news = News.create(
                        request.getOriginalUrl(),
                        request.getTitle(),
                        request.getContent(),
                        request.getPublishedAt(),
                        request.getKeywordId(),
                        request.getRegion()
                );

                boolean inserted = newsRepository.insertIgnoreDuplicate(news);
                if (inserted) {
                    success++;
                    insertedNews.add(news);
                } else {
                    ignored++;
                }
            } catch (Exception e) {
                log.warn("뉴스 저장 실패: {}", e.getMessage());
                failed++;
            }
        }

        return new ChunkSaveResult(new NewsBatchSaveResult(success, ignored, failed), insertedNews);
    }

    private record ChunkSaveResult(NewsBatchSaveResult result, List<News> insertedNews) {
    }

    private List<List<NewsSaveRequest>> splitByBatchSize(List<NewsSaveRequest> requests, int size) {
        int batch = size <= 0 ? 1000 : size;
        List<List<NewsSaveRequest>> chunks = new ArrayList<>();
        for (int i = 0; i < requests.size(); i += batch) {
            int end = Math.min(requests.size(), i + batch);
            chunks.add(requests.subList(i, end));
        }
        return chunks;
    }
}
