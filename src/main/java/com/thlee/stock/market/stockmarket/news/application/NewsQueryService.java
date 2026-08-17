package com.thlee.stock.market.stockmarket.news.application;

import com.thlee.stock.market.stockmarket.common.response.PageResult;
import com.thlee.stock.market.stockmarket.news.application.dto.KeywordNewsFeedResponse;
import com.thlee.stock.market.stockmarket.news.application.dto.KeywordResponse;
import com.thlee.stock.market.stockmarket.news.application.dto.NewsDto;
import com.thlee.stock.market.stockmarket.news.domain.model.News;
import com.thlee.stock.market.stockmarket.news.domain.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 저장된 뉴스 조회 유스케이스 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NewsQueryService {

    private final NewsRepository newsRepository;
    private final KeywordService keywordService;

    /**
     * 여러 URL 중 이미 존재하는 것 필터링
     */
    public List<String> findExistingUrls(List<String> originalLinks) {
        return originalLinks.stream()
                .filter(url -> newsRepository.findByOriginalUrl(url).isPresent())
                .collect(Collectors.toList());
    }

    /**
     * 사용자의 활성 키워드 전체를 합친 최신 뉴스 피드 (#114 홈 대시보드 우측 패널).
     *
     * <p>기존 {@code GET /api/news} 는 {@code keywordId} 가 필수라 키워드 수만큼 호출해야 했다.
     * 활성 키워드가 없으면 조회 없이 빈 응답을 돌려준다.
     * {@code todayCount} 는 수집 시각(createdAt) 기준 오늘 자정 이후 건수다.
     */
    public KeywordNewsFeedResponse getKeywordNewsFeed(Long userId, int size) {
        List<KeywordResponse> keywords = keywordService.getActiveKeywordsByUser(userId);
        if (keywords.isEmpty()) {
            return KeywordNewsFeedResponse.empty();
        }

        List<Long> keywordIds = keywords.stream().map(KeywordResponse::getId).toList();
        Map<Long, String> nameById = keywords.stream()
                .collect(Collectors.toMap(KeywordResponse::getId, KeywordResponse::getKeyword, (a, b) -> a));

        List<News> latest = newsRepository.findLatestByKeywordIds(keywordIds, size);
        long todayCount = newsRepository.countByKeywordIdsSince(keywordIds, LocalDate.now().atStartOfDay());

        List<KeywordNewsFeedResponse.Item> items = latest.stream()
                .map(n -> new KeywordNewsFeedResponse.Item(
                        n.getId(),
                        nameById.get(n.getKeywordId()),
                        n.getTitle(),
                        n.getOriginalUrl(),
                        n.getPublishedAt()))
                .toList();

        return new KeywordNewsFeedResponse(keywords.size(), todayCount, items);
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
