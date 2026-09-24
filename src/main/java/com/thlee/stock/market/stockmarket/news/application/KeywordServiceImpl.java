package com.thlee.stock.market.stockmarket.news.application;

import com.thlee.stock.market.stockmarket.news.application.dto.KeywordResponse;
import com.thlee.stock.market.stockmarket.news.application.dto.KeywordStatsResponse;
import com.thlee.stock.market.stockmarket.news.application.dto.RegisterKeywordRequest;
import com.thlee.stock.market.stockmarket.news.application.dto.UpdateKeywordRequest;
import com.thlee.stock.market.stockmarket.news.domain.model.Keyword;
import com.thlee.stock.market.stockmarket.news.domain.model.Region;
import com.thlee.stock.market.stockmarket.news.domain.model.UserKeyword;
import com.thlee.stock.market.stockmarket.news.domain.repository.KeywordCollectionHistoryRepository;
import com.thlee.stock.market.stockmarket.news.domain.repository.KeywordCollectionSummary;
import com.thlee.stock.market.stockmarket.news.domain.repository.KeywordDailyNewsCount;
import com.thlee.stock.market.stockmarket.news.domain.repository.KeywordNewsCount;
import com.thlee.stock.market.stockmarket.news.domain.repository.KeywordRepository;
import com.thlee.stock.market.stockmarket.news.domain.repository.NewsRepository;
import com.thlee.stock.market.stockmarket.news.domain.repository.UserKeywordRepository;
import com.thlee.stock.market.stockmarket.news.domain.service.NewsIndexPort;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.PortfolioItem;
import com.thlee.stock.market.stockmarket.portfolio.domain.repository.PortfolioItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 키워드 유스케이스 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KeywordServiceImpl implements KeywordService {

    private final KeywordRepository keywordRepository;
    private final UserKeywordRepository userKeywordRepository;
    private final NewsRepository newsRepository;
    private final PortfolioItemRepository portfolioItemRepository;
    /** 키워드 수정 시 이관된 기사를 재색인한다 (#115) — 안 하면 스코프 검색에서 누락된다. */
    private final NewsIndexPort newsIndexPort;
    /** 마지막 성공·연속 실패 판정 근거 (#115 Phase 6). */
    private final KeywordCollectionHistoryRepository collectionHistoryRepository;

    @Override
    @Transactional
    public Keyword registerKeyword(RegisterKeywordRequest request) {
        return registerKeyword(request.getKeyword(), request.getRegion(), request.getUserId());
    }

    @Override
    @Transactional
    public Keyword registerKeyword(String keywordText, Region region, Long userId) {
        // 1. 기존 키워드 재사용 또는 신규 생성
        Keyword keyword = keywordRepository.findByKeywordAndRegion(keywordText, region)
                .orElseGet(() -> {
                    Keyword newKeyword = Keyword.create(keywordText, region);
                    return keywordRepository.save(newKeyword);
                });

        // 2. UserKeyword 구독 생성 (이미 존재하면 활성화)
        Optional<UserKeyword> existing = userKeywordRepository.findByUserIdAndKeywordId(userId, keyword.getId());
        if (existing.isPresent()) {
            UserKeyword subscription = existing.get();
            subscription.activate();
            userKeywordRepository.save(subscription);
        } else {
            UserKeyword subscription = UserKeyword.create(userId, keyword.getId());
            userKeywordRepository.save(subscription);
        }

        return keyword;
    }

    @Override
    public List<KeywordResponse> getKeywordsByUser(Long userId) {
        List<UserKeyword> subscriptions = userKeywordRepository.findByUserId(userId);
        return subscriptions.stream()
                .flatMap(sub -> keywordRepository.findById(sub.getKeywordId())
                        .map(kw -> KeywordResponse.from(kw, sub))
                        .stream())
                .collect(Collectors.toList());
    }

    @Override
    public List<KeywordResponse> getActiveKeywordsByUser(Long userId) {
        List<UserKeyword> subscriptions = userKeywordRepository.findByUserIdAndActive(userId, true);
        return subscriptions.stream()
                .flatMap(sub -> keywordRepository.findById(sub.getKeywordId())
                        .map(kw -> KeywordResponse.from(kw, sub))
                        .stream())
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> getSubscribedKeywordIds(Long userId) {
        return userKeywordRepository.findByUserId(userId).stream()
                .map(UserKeyword::getKeywordId)
                .toList();
    }

    @Override
    public KeywordStatsResponse getKeywordStats(Long userId) {
        List<Long> keywordIds = getSubscribedKeywordIds(userId);
        if (keywordIds.isEmpty()) {
            return KeywordStatsResponse.empty();
        }

        LocalDate today = LocalDate.now();
        LocalDate windowStart = today.minusDays(KeywordStatsResponse.SPARKLINE_DAYS - 1L);

        // 쿼리 3회로 전 키워드를 집계한다 (키워드마다 조회하면 N+1)
        Map<Long, KeywordNewsCount> totals = newsRepository.aggregateCountsByKeywordIds(keywordIds).stream()
                .collect(Collectors.toMap(KeywordNewsCount::keywordId, Function.identity(), (a, b) -> a));
        Map<Long, Map<LocalDate, Long>> dailyByKeyword = newsRepository
                .aggregateDailyCountsByKeywordIds(keywordIds, windowStart.atStartOfDay())
                .stream()
                .collect(Collectors.groupingBy(
                        KeywordDailyNewsCount::keywordId,
                        Collectors.toMap(KeywordDailyNewsCount::day, KeywordDailyNewsCount::dailyCount, (a, b) -> a)
                ));
        // 마지막 성공·연속 실패는 수집 이력이 근거다 (#115 Phase 6).
        // MAX(news.created_at) 은 "마지막으로 기사가 저장된 시각"이라 새 기사가 없던 성공을 놓친다.
        Map<Long, KeywordCollectionSummary> collectionByKeyword = collectionHistoryRepository
                .summarizeByKeywordIds(keywordIds).stream()
                .collect(Collectors.toMap(KeywordCollectionSummary::keywordId, Function.identity(), (a, b) -> a));

        long todayTotal = 0L;
        List<KeywordStatsResponse.Item> items = new ArrayList<>(keywordIds.size());
        for (Long keywordId : keywordIds) {
            Map<LocalDate, Long> byDay = dailyByKeyword.getOrDefault(keywordId, Map.of());

            // 수집이 없던 날은 행이 아예 없으므로 0 으로 메워 항상 7칸을 만든다
            List<Long> daily = new ArrayList<>(KeywordStatsResponse.SPARKLINE_DAYS);
            for (int i = 0; i < KeywordStatsResponse.SPARKLINE_DAYS; i++) {
                daily.add(byDay.getOrDefault(windowStart.plusDays(i), 0L));
            }

            long todayCount = byDay.getOrDefault(today, 0L);
            todayTotal += todayCount;

            KeywordNewsCount total = totals.get(keywordId);
            KeywordCollectionSummary collection = collectionByKeyword.get(keywordId);
            items.add(new KeywordStatsResponse.Item(
                    keywordId,
                    total != null ? total.totalCount() : 0L,
                    todayCount,
                    collection != null ? collection.lastSuccessAt() : null,
                    collection != null ? collection.failureStreak() : 0L,
                    daily
            ));
        }

        return new KeywordStatsResponse(todayTotal, items);
    }

    @Override
    public List<Keyword> getAllKeywords() {
        return keywordRepository.findAll();
    }

    @Override
    @Transactional
    public void activateUserKeyword(Long userId, Long keywordId) {
        UserKeyword subscription = userKeywordRepository.findByUserIdAndKeywordId(userId, keywordId)
                .orElseThrow(() -> new IllegalArgumentException("구독 정보를 찾을 수 없습니다."));
        subscription.activate();
        userKeywordRepository.save(subscription);
    }

    @Override
    @Transactional
    public void deactivateUserKeyword(Long userId, Long keywordId) {
        UserKeyword subscription = userKeywordRepository.findByUserIdAndKeywordId(userId, keywordId)
                .orElseThrow(() -> new IllegalArgumentException("구독 정보를 찾을 수 없습니다."));
        subscription.deactivate();
        userKeywordRepository.save(subscription);

        // 포트폴리오 항목의 newsEnabled도 OFF
        disablePortfolioNewsByKeywordId(userId, keywordId);
    }

    @Override
    @Transactional
    public void unsubscribeKeyword(Long userId, Long keywordId) {
        // 포트폴리오 항목의 newsEnabled OFF
        disablePortfolioNewsByKeywordId(userId, keywordId);

        // 1. UserKeyword 삭제
        userKeywordRepository.deleteByUserIdAndKeywordId(userId, keywordId);

        // 2. 해당 keyword 구독자가 0명이면 keyword + news 일괄 삭제
        boolean hasSubscribers = userKeywordRepository.existsByKeywordId(keywordId);
        if (!hasSubscribers) {
            newsRepository.deleteByKeywordId(keywordId);
            keywordRepository.deleteById(keywordId);
        }
    }

    /**
     * 키워드 수정 (#115) — 공유 리소스라 UPDATE 대신 <b>재구독</b>으로 처리한다.
     *
     * <p>절차
     * <ol>
     *   <li>이름·지역이 그대로면 활성 토글만 반영하고 끝낸다(불필요한 이관 방지)</li>
     *   <li>새 (이름, 지역) 이 이미 내 구독이면 병합이 되므로 막는다</li>
     *   <li>새 키워드를 find-or-create 하고 구독한다</li>
     *   <li><b>단독 구독자면</b> 기존 기사를 새 키워드로 이관한다 — 목업의 "기사 유지" 약속.
     *       다른 구독자가 있으면 그들의 기사이므로 옮기지 않는다</li>
     *   <li>기존 구독을 해제한다</li>
     * </ol>
     *
     * <p><b>포트폴리오 연동</b>: 5번의 {@code unsubscribeKeyword} 가
     * 같은 이름의 포트폴리오 항목 {@code newsEnabled} 를 끈다. 이는 기존 모델의 의도된 역방향 동기화다 —
     * 항목명으로 키워드를 만드는 구조(`PortfolioService.toggleNews`)라서, 이름을 바꾸면
     * 그 항목을 수집하던 키워드가 없어지므로 플래그를 켜 둔 채로 두면 "뉴스 ON 인데 아무것도 안 모임"이 된다.
     */
    @Override
    @Transactional
    public KeywordResponse updateKeyword(Long userId, Long keywordId, UpdateKeywordRequest request) {
        Keyword current = keywordRepository.findById(keywordId)
                .orElseThrow(() -> new IllegalArgumentException("키워드를 찾을 수 없습니다."));
        UserKeyword subscription = userKeywordRepository.findByUserIdAndKeywordId(userId, keywordId)
                .orElseThrow(() -> new IllegalArgumentException("구독 정보를 찾을 수 없습니다."));

        String newName = request.getKeyword() == null ? "" : request.getKeyword().trim();
        if (newName.isBlank()) {
            throw new IllegalArgumentException("키워드는 필수입니다.");
        }
        Region newRegion = request.getRegion();

        // 1. 이름·지역이 그대로면 활성 토글만 반영한다
        if (newName.equals(current.getKeyword()) && newRegion == current.getRegion()) {
            applyActiveState(userId, keywordId, request.isActive());
            UserKeyword updated = userKeywordRepository.findByUserIdAndKeywordId(userId, keywordId)
                    .orElse(subscription);
            return KeywordResponse.from(current, updated);
        }

        // 2. 새 (이름, 지역) 을 이미 구독 중이면 두 키워드가 합쳐져 버린다 — 사전에 막는다
        Optional<Keyword> existingTarget = keywordRepository.findByKeywordAndRegion(newName, newRegion);
        if (existingTarget.isPresent()
                && userKeywordRepository.findByUserIdAndKeywordId(userId, existingTarget.get().getId()).isPresent()) {
            throw new IllegalArgumentException("이미 등록된 키워드입니다.");
        }

        // 4번(이관) 판정을 구독 해제 전에 해야 한다 — 해제 후에는 단독 여부를 알 수 없다
        boolean soleSubscriber = userKeywordRepository.findByKeywordId(keywordId).size() <= 1;

        // 3. 새 키워드 구독
        Keyword renamed = registerKeyword(newName, newRegion, userId);

        // 4. 단독 구독자면 기사 이관 + ES 재색인
        if (soleSubscriber) {
            int moved = newsRepository.reassignKeywordId(keywordId, renamed.getId());
            if (moved > 0) {
                // ES 문서의 keywordId 가 옛 값으로 남으면 키워드 스코프 검색에서 누락된다.
                // 인덱서는 예외를 삼키므로 ES 장애가 수정 자체를 깨뜨리지는 않는다.
                newsIndexPort.indexAll(newsRepository.findAllByKeywordId(renamed.getId()));
            }
        }

        // 5. 기존 구독 해제 (기사는 이미 옮겼으므로 여기서 지워질 것이 없다)
        unsubscribeKeyword(userId, keywordId);

        applyActiveState(userId, renamed.getId(), request.isActive());
        UserKeyword newSubscription = userKeywordRepository.findByUserIdAndKeywordId(userId, renamed.getId())
                .orElseThrow(() -> new IllegalStateException("재구독 정보를 찾을 수 없습니다."));
        return KeywordResponse.from(renamed, newSubscription);
    }

    @Override
    @Transactional
    public void deactivateUserKeywords(Long userId, List<Long> keywordIds) {
        if (keywordIds == null || keywordIds.isEmpty()) {
            return;
        }
        for (Long keywordId : keywordIds) {
            deactivateUserKeyword(userId, keywordId);
        }
    }

    @Override
    @Transactional
    public void unsubscribeKeywords(Long userId, List<Long> keywordIds) {
        if (keywordIds == null || keywordIds.isEmpty()) {
            return;
        }
        for (Long keywordId : keywordIds) {
            unsubscribeKeyword(userId, keywordId);
        }
    }

    /**
     * {@code registerKeyword} 는 항상 구독을 활성 상태로 만든다 —
     * 수정 모달에서 `중단`을 선택했다면 여기서 되돌린다.
     */
    private void applyActiveState(Long userId, Long keywordId, boolean active) {
        if (active) {
            activateUserKeyword(userId, keywordId);
        } else {
            deactivateUserKeyword(userId, keywordId);
        }
    }

    private void disablePortfolioNewsByKeywordId(Long userId, Long keywordId) {
        keywordRepository.findById(keywordId).ifPresent(keyword -> {
            List<PortfolioItem> items = portfolioItemRepository
                    .findByUserIdAndItemNameAndNewsEnabled(userId, keyword.getKeyword(), true);
            for (PortfolioItem item : items) {
                item.disableNews();
                portfolioItemRepository.save(item);
            }
        });
    }
}
