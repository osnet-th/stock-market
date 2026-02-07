package com.thlee.stock.market.stockmarket.news.application;

import com.thlee.stock.market.stockmarket.news.application.dto.RegisterKeywordRequest;
import com.thlee.stock.market.stockmarket.news.domain.model.Keyword;
import com.thlee.stock.market.stockmarket.news.domain.repository.KeywordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 키워드 유스케이스 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KeywordServiceImpl implements KeywordService{

    private final KeywordRepository keywordRepository;

    /**
     * 키워드 등록
     */
    @Transactional
    public Keyword registerKeyword(RegisterKeywordRequest request) {
        Keyword newKeyword = Keyword.create(request.getKeyword(), request.getUserId(), request.getRegion());
        return keywordRepository.save(newKeyword);
    }

    /**
     * 사용자별 키워드 목록 조회
     */
    public List<Keyword> getKeywordsByUserId(Long userId) {
        return keywordRepository.findByUserId(userId);
    }

    /**
     * 사용자별 활성화된 키워드 목록 조회
     */
    public List<Keyword> getActiveKeywordsByUserId(Long userId) {
        return keywordRepository.findByUserIdAndActive(userId, true);
    }

    /**
     * 활성화된 모든 키워드 조회 (스케줄러용)
     */
    public List<Keyword> getAllActiveKeywords() {
        return keywordRepository.findByActive(true);
    }

    /**
     * 키워드 비활성화
     */
    @Transactional
    public void deactivateKeyword(Long keywordId) {
        Keyword keyword = keywordRepository.findById(keywordId)
                .orElseThrow(() -> new IllegalArgumentException("키워드를 찾을 수 없습니다."));
        keyword.deactivate();
        keywordRepository.save(keyword);
    }

    /**
     * 키워드 활성화
     */
    @Transactional
    public void activateKeyword(Long keywordId) {
        Keyword keyword = keywordRepository.findById(keywordId)
                .orElseThrow(() -> new IllegalArgumentException("키워드를 찾을 수 없습니다."));
        keyword.activate();
        keywordRepository.save(keyword);
    }

    /**
     * 키워드 삭제
     */
    @Transactional
    public void deleteKeyword(Long keywordId) {
        Keyword keyword = keywordRepository.findById(keywordId)
                .orElseThrow(() -> new IllegalArgumentException("키워드를 찾을 수 없습니다."));
        keywordRepository.delete(keyword);
    }
}