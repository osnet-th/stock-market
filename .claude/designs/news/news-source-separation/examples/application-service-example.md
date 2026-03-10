# 애플리케이션 서비스 예시

## NewsSaveService (변경 후)

```java
package com.thlee.stock.market.stockmarket.news.application;

import com.thlee.stock.market.stockmarket.news.application.dto.NewsBatchSaveResult;
import com.thlee.stock.market.stockmarket.news.application.dto.NewsSaveRequest;
import com.thlee.stock.market.stockmarket.news.domain.model.News;
import com.thlee.stock.market.stockmarket.news.domain.model.NewsSource;
import com.thlee.stock.market.stockmarket.news.domain.model.NewsPurpose;
import com.thlee.stock.market.stockmarket.news.domain.model.Region;
import com.thlee.stock.market.stockmarket.news.domain.repository.NewsRepository;
import com.thlee.stock.market.stockmarket.news.domain.repository.NewsSourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NewsSaveService {

    private final NewsRepository newsRepository;
    private final NewsSourceRepository newsSourceRepository;
    private final PlatformTransactionManager transactionManager;

    @Value("${spring.jpa.properties.hibernate.jdbc.batch_size:1000}")
    private int batchSize;

    @Transactional
    public void save(NewsSaveRequest request, NewsPurpose purpose) {
        // 1. 뉴스 콘텐츠 저장 (중복 URL이면 기존 레코드 반환)
        News news = News.create(
                request.getOriginalUrl(),
                request.getTitle(),
                request.getContent(),
                request.getPublishedAt(),
                request.getRegion()
        );
        News savedNews = newsRepository.saveIgnoreDuplicate(news);

        // 2. 수집 출처 매핑 저장
        NewsSource newsSource = NewsSource.create(
                savedNews.getId(),
                request.getUserId(),
                purpose,
                request.getSourceId()
        );
        newsSourceRepository.insertIgnoreDuplicate(newsSource);
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
                // 1. 뉴스 콘텐츠 저장 (중복 URL이면 기존 레코드 반환)
                News news = News.create(
                        request.getOriginalUrl(),
                        request.getTitle(),
                        request.getContent(),
                        request.getPublishedAt(),
                        request.getRegion()
                );
                News savedNews = newsRepository.saveIgnoreDuplicate(news);

                // 2. 수집 출처 매핑 저장
                NewsSource newsSource = NewsSource.create(
                        savedNews.getId(),
                        request.getUserId(),
                        purpose,
                        request.getSourceId()
                );
                boolean inserted = newsSourceRepository.insertIgnoreDuplicate(newsSource);
                if (inserted) {
                    success++;
                } else {
                    ignored++;
                }
            } catch (Exception e) {
                e.printStackTrace();
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
```

## NewsQueryService (변경 후)

```java
package com.thlee.stock.market.stockmarket.news.application;

import com.thlee.stock.market.stockmarket.common.response.PageResult;
import com.thlee.stock.market.stockmarket.news.application.dto.NewsDto;
import com.thlee.stock.market.stockmarket.news.domain.model.News;
import com.thlee.stock.market.stockmarket.news.domain.model.NewsPurpose;
import com.thlee.stock.market.stockmarket.news.domain.repository.NewsRepository;
import com.thlee.stock.market.stockmarket.news.domain.repository.NewsSourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NewsQueryService {

    private final NewsRepository newsRepository;
    private final NewsSourceRepository newsSourceRepository;

    /**
     * 특정 사용자가 이미 수집한 URL 목록 조회
     */
    public List<String> findExistingUrlsByUser(List<String> originalLinks, Long userId) {
        return newsSourceRepository.findExistingUrlsByUser(originalLinks, userId);
    }

    /**
     * purpose + sourceId 기반 뉴스 조회 (NewsSource → News 조인)
     */
    public PageResult<NewsDto> getNewsBySource(NewsPurpose purpose, Long sourceId, int page, int size) {
        // 1. NewsSource에서 newsId 목록 조회 (페이징)
        PageResult<Long> newsIdPage = newsSourceRepository.findNewsIdsByPurposeAndSourceId(
                purpose, sourceId, page, size);

        if (newsIdPage.getContent().isEmpty()) {
            return new PageResult<>(List.of(), page, size, 0);
        }

        // 2. newsId로 News 조회
        List<News> newsList = newsRepository.findByIds(newsIdPage.getContent());

        List<NewsDto> dtoList = newsList.stream()
                .map(NewsDto::from)
                .collect(Collectors.toList());

        return new PageResult<>(dtoList, page, size, newsIdPage.getTotalElements());
    }
}
```

## KeywordNewsBatchServiceImpl (변경 후 - filterNewNews 부분)

```java
// KeywordNewsBatchServiceImpl 내 filterNewNews 메서드만 변경

/**
 * 사용자별 중복 필터링
 * 동일 URL이라도 다른 사용자는 수집 가능
 */
private List<KeywordSearchContext> filterNewNews(List<KeywordSearchContext> searchedContexts) {
    // 사용자별로 그룹핑하여 각 사용자마다 독립적으로 중복 검사
    Map<Long, List<KeywordSearchContext>> byUser = searchedContexts.stream()
            .collect(Collectors.groupingBy(KeywordSearchContext::userId));

    List<KeywordSearchContext> result = new ArrayList<>();

    for (Map.Entry<Long, List<KeywordSearchContext>> entry : byUser.entrySet()) {
        Long userId = entry.getKey();
        List<KeywordSearchContext> userContexts = entry.getValue();

        // 해당 사용자가 이미 수집한 URL 조회
        List<String> urls = userContexts.stream()
                .map(context -> context.news().getUrl())
                .distinct()
                .toList();

        List<String> existingUrls = newsQueryService.findExistingUrlsByUser(urls, userId);
        Set<String> existingUrlSet = new HashSet<>(existingUrls);
        Set<String> selectedUrls = new HashSet<>();

        // 해당 사용자 기준으로 신규 URL만 필터링
        userContexts.stream()
                .filter(context -> !existingUrlSet.contains(context.news().getUrl()))
                .filter(context -> selectedUrls.add(context.news().getUrl()))
                .forEach(result::add);
    }

    return result;
}
```

## NewsSaveRequest (변경 없음 - userId, sourceId, region 유지)

`NewsSaveRequest`는 변경하지 않습니다. 이 DTO는 "누가 어떤 출처로 수집했는가" 정보를 전달하는 역할이며, `NewsSaveService`에서 News와 NewsSource로 분리 저장합니다.

## NewsDto (변경 후)

```java
package com.thlee.stock.market.stockmarket.news.application.dto;

import com.thlee.stock.market.stockmarket.news.domain.model.News;
import com.thlee.stock.market.stockmarket.news.domain.model.Region;

import java.time.LocalDateTime;

/**
 * 저장된 뉴스 응답 DTO
 * 뉴스 콘텐츠 + region 정보 포함
 */
public class NewsDto {
    private final Long id;
    private final String originalUrl;
    private final String title;
    private final String content;
    private final LocalDateTime publishedAt;
    private final LocalDateTime createdAt;
    private final Region region;

    public NewsDto(Long id,
                   String originalUrl,
                   String title,
                   String content,
                   LocalDateTime publishedAt,
                   LocalDateTime createdAt,
                   Region region) {
        this.id = id;
        this.originalUrl = originalUrl;
        this.title = title;
        this.content = content;
        this.publishedAt = publishedAt;
        this.createdAt = createdAt;
        this.region = region;
    }

    public static NewsDto from(News news) {
        return new NewsDto(
                news.getId(),
                news.getOriginalUrl(),
                news.getTitle(),
                news.getContent(),
                news.getPublishedAt(),
                news.getCreatedAt(),
                news.getRegion()
        );
    }

    public Long getId() { return id; }
    public String getOriginalUrl() { return originalUrl; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Region getRegion() { return region; }
}
```