package com.thlee.stock.market.stockmarket.news.application;

import com.thlee.stock.market.stockmarket.news.application.dto.KeywordResponse;
import com.thlee.stock.market.stockmarket.news.application.dto.RegisterKeywordRequest;
import com.thlee.stock.market.stockmarket.news.domain.model.Keyword;
import com.thlee.stock.market.stockmarket.news.domain.model.Region;
import com.thlee.stock.market.stockmarket.news.domain.model.UserKeyword;
import com.thlee.stock.market.stockmarket.news.domain.repository.KeywordRepository;
import com.thlee.stock.market.stockmarket.news.domain.repository.NewsRepository;
import com.thlee.stock.market.stockmarket.news.domain.repository.UserKeywordRepository;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.PortfolioItem;
import com.thlee.stock.market.stockmarket.portfolio.domain.repository.PortfolioItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
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
