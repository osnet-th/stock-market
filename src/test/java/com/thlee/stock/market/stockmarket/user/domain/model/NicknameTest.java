package com.thlee.stock.market.stockmarket.user.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NicknameTest {

    @Test
    void 정상적인_닉네임_생성() {
        // given
        String value = "태형";

        // when
        Nickname nickname = new Nickname(value);

        // then
        assertThat(nickname.getValue()).isEqualTo(value);
    }

    @Test
    void null_닉네임_거부() {
        // when & then
        assertThatThrownBy(() -> new Nickname(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("닉네임");
    }

    @Test
    void 빈_문자열_닉네임_거부() {
        // when & then
        assertThatThrownBy(() -> new Nickname(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("닉네임");
    }

    @Test
    void 공백만_있는_닉네임_거부() {
        // when & then
        assertThatThrownBy(() -> new Nickname("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("닉네임");
    }

    @Test
    void 너무_짧은_닉네임_거부() {
        // when & then
        assertThatThrownBy(() -> new Nickname("a"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2자 이상");
    }

    @Test
    void 너무_긴_닉네임_거부() {
        // given
        String longNickname = "a".repeat(21);

        // when & then
        assertThatThrownBy(() -> new Nickname(longNickname))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("20자 이하");
    }

    @Test
    void 같은_값의_닉네임은_동등하다() {
        // given
        Nickname nickname1 = new Nickname("태형");
        Nickname nickname2 = new Nickname("태형");

        // when & then
        assertThat(nickname1).isEqualTo(nickname2);
        assertThat(nickname1.hashCode()).isEqualTo(nickname2.hashCode());
    }

    @Test
    void 다른_값의_닉네임은_동등하지_않다() {
        // given
        Nickname nickname1 = new Nickname("태형");
        Nickname nickname2 = new Nickname("민수");

        // when & then
        assertThat(nickname1).isNotEqualTo(nickname2);
    }
}