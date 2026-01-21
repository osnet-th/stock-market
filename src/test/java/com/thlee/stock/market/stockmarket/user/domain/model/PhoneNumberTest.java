package com.thlee.stock.market.stockmarket.user.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhoneNumberTest {

    @Test
    void 정상적인_전화번호_생성() {
        // given
        String value = "01012345678";

        // when
        PhoneNumber phoneNumber = new PhoneNumber(value);

        // then
        assertThat(phoneNumber.getValue()).isEqualTo(value);
    }

    @Test
    void 하이픈이_있는_전화번호_정규화() {
        // given
        String value = "010-1234-5678";

        // when
        PhoneNumber phoneNumber = new PhoneNumber(value);

        // then
        assertThat(phoneNumber.getValue()).isEqualTo("01012345678");
    }

    @Test
    void null_전화번호_거부() {
        // when & then
        assertThatThrownBy(() -> new PhoneNumber(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("전화번호");
    }

    @Test
    void 빈_문자열_전화번호_거부() {
        // when & then
        assertThatThrownBy(() -> new PhoneNumber(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("전화번호");
    }

    @Test
    void 숫자가_아닌_문자_포함_거부() {
        // when & then
        assertThatThrownBy(() -> new PhoneNumber("010abcd5678"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("숫자");
    }

    @Test
    void 너무_짧은_전화번호_거부() {
        // when & then
        assertThatThrownBy(() -> new PhoneNumber("0101234"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("10자 이상");
    }

    @Test
    void 너무_긴_전화번호_거부() {
        // when & then
        assertThatThrownBy(() -> new PhoneNumber("010123456789012"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("11자 이하");
    }

    @Test
    void 같은_값의_전화번호는_동등하다() {
        // given
        PhoneNumber phoneNumber1 = new PhoneNumber("01012345678");
        PhoneNumber phoneNumber2 = new PhoneNumber("010-1234-5678");

        // when & then
        assertThat(phoneNumber1).isEqualTo(phoneNumber2);
        assertThat(phoneNumber1.hashCode()).isEqualTo(phoneNumber2.hashCode());
    }

    @Test
    void 다른_값의_전화번호는_동등하지_않다() {
        // given
        PhoneNumber phoneNumber1 = new PhoneNumber("01012345678");
        PhoneNumber phoneNumber2 = new PhoneNumber("01087654321");

        // when & then
        assertThat(phoneNumber1).isNotEqualTo(phoneNumber2);
    }
}