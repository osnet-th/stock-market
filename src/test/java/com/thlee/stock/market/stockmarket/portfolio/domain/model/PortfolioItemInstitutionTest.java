package com.thlee.stock.market.stockmarket.portfolio.domain.model;

import com.thlee.stock.market.stockmarket.news.domain.model.Region;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.AssetType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PortfolioItem.updateInstitution — 금융기관 정규화와 길이 검사")
class PortfolioItemInstitutionTest {

    private static final String FIFTY_CHARS = "가".repeat(50);

    @Test
    @DisplayName("D1 앞뒤 공백을 지우고 저장한다")
    void trimsSurroundingSpaces() {
        PortfolioItem item = newItem();

        item.updateInstitution("  국민은행  ");

        assertThat(item.getInstitution()).isEqualTo("국민은행");
    }

    @Test
    @DisplayName("D2 전각 공백·줄바꿈 없는 공백·탭도 앞뒤에서 지우고 가운데 공백은 그대로 둔다")
    void trimsUnicodeSpacesAndKeepsInnerSpace() {
        PortfolioItem item = newItem();

        item.updateInstitution("　KB 국민은행 \t");

        assertThat(item.getInstitution()).isEqualTo("KB 국민은행");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    @DisplayName("D3 null·빈 값·공백만 있으면 미지정(null)으로 바꾼다")
    void blankBecomesUnassigned(String blank) {
        PortfolioItem item = newItem();
        item.updateInstitution("국민은행");

        item.updateInstitution(blank);

        assertThat(item.getInstitution()).isNull();
    }

    @Test
    @DisplayName("D4 공백을 지운 값이 50자면 저장한다")
    void acceptsFiftyCharacters() {
        PortfolioItem item = newItem();

        item.updateInstitution(FIFTY_CHARS);
        assertThat(item.getInstitution()).isEqualTo(FIFTY_CHARS);

        item.updateInstitution(" " + FIFTY_CHARS + " ");
        assertThat(item.getInstitution()).isEqualTo(FIFTY_CHARS);
    }

    @Test
    @DisplayName("D5 공백을 지운 값이 50자를 넘으면 거부하고 기존 값을 유지한다")
    void rejectsOverFiftyCharactersAndKeepsValue() {
        PortfolioItem item = newItem();
        item.updateInstitution("국민은행");

        assertThatThrownBy(() -> item.updateInstitution(FIFTY_CHARS + "가"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("금융기관은 50자 이하로 입력해 주세요.");
        assertThat(item.getInstitution()).isEqualTo("국민은행");
    }

    private static PortfolioItem newItem() {
        return PortfolioItem.create(1L, "비트코인", AssetType.CRYPTO, BigDecimal.valueOf(1_000_000), Region.DOMESTIC);
    }
}
