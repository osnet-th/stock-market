package com.thlee.stock.market.stockmarket.news.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KeywordTest {

    @Test
    void 키워드_생성_성공() {
        // given
        String keyword = "삼성전자";
        Long userId = 1L;

        // when
        Keyword result = Keyword.create(keyword, userId);

        // then
        assertThat(result.getKeyword()).isEqualTo(keyword);
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.isActive()).isTrue();
        assertThat(result.getCreatedAt()).isNotNull();
    }

    @Test
    void keyword가_null이면_예외_발생() {
        // when & then
        assertThatThrownBy(() -> Keyword.create(null, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("키워드는 필수");
    }

    @Test
    void keyword가_빈_문자열이면_예외_발생() {
        // when & then
        assertThatThrownBy(() -> Keyword.create("", 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("키워드는 필수");
    }

    @Test
    void keyword가_공백만_있으면_예외_발생() {
        // when & then
        assertThatThrownBy(() -> Keyword.create("   ", 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("키워드는 필수");
    }

    @Test
    void userId가_null이면_예외_발생() {
        // when & then
        assertThatThrownBy(() -> Keyword.create("삼성전자", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userId는 필수");
    }

    @Test
    void 키워드_비활성화() {
        // given
        Keyword keyword = Keyword.create("삼성전자", 1L);

        // when
        keyword.deactivate();

        // then
        assertThat(keyword.isActive()).isFalse();
    }

    @Test
    void 키워드_활성화() {
        // given
        Keyword keyword = Keyword.create("삼성전자", 1L);
        keyword.deactivate();

        // when
        keyword.activate();

        // then
        assertThat(keyword.isActive()).isTrue();
    }
}