package com.thlee.stock.market.stockmarket.news.application;

import com.thlee.stock.market.stockmarket.news.application.dto.NewsBatchSaveResult;
import com.thlee.stock.market.stockmarket.news.application.dto.NewsDto;
import com.thlee.stock.market.stockmarket.news.application.dto.NewsSaveRequest;
import com.thlee.stock.market.stockmarket.news.domain.model.News;
import com.thlee.stock.market.stockmarket.news.domain.model.NewsPurpose;
import com.thlee.stock.market.stockmarket.news.domain.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
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
@Service
@RequiredArgsConstructor
public class NewsSaveService {

    private final NewsRepository newsRepository;
    private final PlatformTransactionManager transactionManager;

    @Value("${spring.jpa.properties.hibernate.jdbc.batch_size:1000}")
    private int batchSize;

    @Transactional
    public NewsDto save(NewsSaveRequest request, NewsPurpose purpose) {
        News news = News.create(
                request.getOriginalUrl(),
                request.getUserId(),
                request.getTitle(),
                request.getContent(),
                request.getPublishedAt(),
                purpose,
                request.getSearchKeyword()
        );

        News saved = newsRepository.save(news);
        return NewsDto.from(saved);
    }

    public NewsBatchSaveResult saveBatch(List<NewsSaveRequest> requests, NewsPurpose purpose) {
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

            NewsBatchSaveResult result = template.execute(status -> saveChunk(chunk, purpose));
            if (result != null) {
                success += result.getSuccessCount();
                ignored += result.getIgnoredCount();
                failed += result.getFailedCount();
            }
        }

        return new NewsBatchSaveResult(success, ignored, failed);
    }

    private NewsBatchSaveResult saveChunk(List<NewsSaveRequest> chunk, NewsPurpose purpose) {
        int success = 0;
        int ignored = 0;
        int failed = 0;

        for (NewsSaveRequest request : chunk) {
            try {
                News news = News.create(
                        request.getOriginalUrl(),
                        request.getUserId(),
                        request.getTitle(),
                        request.getContent(),
                        request.getPublishedAt(),
                        purpose,
                        request.getSearchKeyword()
                );

                boolean inserted = newsRepository.insertIgnoreDuplicate(news);
                if (inserted) {
                    success++;
                } else {
                    ignored++;
                }
            } catch (Exception ignoredException) {
                failed++;
            }
        }

        return new NewsBatchSaveResult(success, ignored, failed);
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
