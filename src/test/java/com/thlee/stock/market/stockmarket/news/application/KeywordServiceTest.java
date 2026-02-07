package com.thlee.stock.market.stockmarket.news.application;

import com.thlee.stock.market.stockmarket.news.application.dto.RegisterKeywordRequest;
import com.thlee.stock.market.stockmarket.news.domain.model.Keyword;
import com.thlee.stock.market.stockmarket.news.domain.model.KeywordRegion;
import com.thlee.stock.market.stockmarket.news.domain.repository.KeywordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KeywordServiceTest {

    @Mock
    private KeywordRepository keywordRepository;

    private KeywordServiceImpl keywordService;

    @BeforeEach
    void setUp() {
        keywordService = new KeywordServiceImpl(keywordRepository);
    }

    @Test
    void 키워드_등록_성공() {
        // given
        RegisterKeywordRequest request = new RegisterKeywordRequest("삼성전자", 1L, KeywordRegion.DOMESTIC);

        Keyword savedKeyword = new Keyword(1L, request.getKeyword(), request.getUserId(), true,
                request.getRegion(), LocalDateTime.now());
        when(keywordRepository.save(any(Keyword.class))).thenReturn(savedKeyword);

        // when
        Keyword result = keywordService.registerKeyword(request);

        // then
        assertThat(result.getKeyword()).isEqualTo(request.getKeyword());
        assertThat(result.getUserId()).isEqualTo(request.getUserId());
        assertThat(result.getRegion()).isEqualTo(request.getRegion());
        assertThat(result.isActive()).isTrue();
        verify(keywordRepository).save(any(Keyword.class));
    }

    @Test
    void 키워드_비활성화_성공() {
        // given
        Long keywordId = 1L;
        Keyword keyword = new Keyword(keywordId, "삼성전자", 1L, true, KeywordRegion.DOMESTIC, LocalDateTime.now());

        when(keywordRepository.findById(keywordId)).thenReturn(Optional.of(keyword));
        when(keywordRepository.save(any(Keyword.class))).thenReturn(keyword);

        // when
        keywordService.deactivateKeyword(keywordId);

        // then
        assertThat(keyword.isActive()).isFalse();
        verify(keywordRepository).save(keyword);
    }

    @Test
    void 키워드_활성화_성공() {
        // given
        Long keywordId = 1L;
        Keyword keyword = new Keyword(keywordId, "삼성전자", 1L, false, KeywordRegion.DOMESTIC, LocalDateTime.now());

        when(keywordRepository.findById(keywordId)).thenReturn(Optional.of(keyword));
        when(keywordRepository.save(any(Keyword.class))).thenReturn(keyword);

        // when
        keywordService.activateKeyword(keywordId);

        // then
        assertThat(keyword.isActive()).isTrue();
        verify(keywordRepository).save(keyword);
    }

    @Test
    void 키워드_삭제_성공() {
        // given
        Long keywordId = 1L;
        Keyword keyword = new Keyword(keywordId, "삼성전자", 1L, true, KeywordRegion.DOMESTIC, LocalDateTime.now());

        when(keywordRepository.findById(keywordId)).thenReturn(Optional.of(keyword));

        // when
        keywordService.deleteKeyword(keywordId);

        // then
        verify(keywordRepository).delete(keyword);
    }

    @Test
    void 존재하지_않는_키워드_비활성화_시_예외_발생() {
        // given
        Long keywordId = 999L;
        when(keywordRepository.findById(keywordId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> keywordService.deactivateKeyword(keywordId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("키워드를 찾을 수 없습니다");
    }

    @Test
    void 존재하지_않는_키워드_삭제_시_예외_발생() {
        // given
        Long keywordId = 999L;
        when(keywordRepository.findById(keywordId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> keywordService.deleteKeyword(keywordId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("키워드를 찾을 수 없습니다");
    }
}