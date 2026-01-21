package com.thlee.stock.market.stockmarket.user.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    @Test
    void 가입중_상태의_사용자_생성() {
        // when
        User user = User.createSigning();

        // then
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getRole()).isEqualTo(UserRole.SIGNING_USER);
        assertThat(user.getName()).isNull();
        assertThat(user.getNickname()).isNull();
        assertThat(user.getPhoneNumber()).isNull();
        assertThat(user.getOauthAccountIds()).isEmpty();
        assertThat(user.getCreatedAt()).isNotNull();
    }

    @Test
    void 가입_완료_처리() {
        // given
        User user = User.createSigning();
        String name = "태형";
        Nickname nickname = new Nickname("태형닉네임");
        PhoneNumber phoneNumber = new PhoneNumber("01012345678");

        // when
        user.completeSignup(name, nickname, phoneNumber);

        // then
        assertThat(user.getName()).isEqualTo(name);
        assertThat(user.getNickname()).isEqualTo(nickname);
        assertThat(user.getPhoneNumber()).isEqualTo(phoneNumber);
        assertThat(user.getRole()).isEqualTo(UserRole.USER);
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void 이미_가입_완료된_사용자는_다시_가입_완료_불가() {
        // given
        User user = User.createSigning();
        user.completeSignup("태형", new Nickname("태형닉네임"), new PhoneNumber("01012345678"));

        // when & then
        assertThatThrownBy(() -> user.completeSignup("민수", new Nickname("민수닉네임"), new PhoneNumber("01087654321")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 가입 완료");
    }

    @Test
    void 활성화_상태_확인() {
        // given
        User user = User.createSigning();

        // when & then
        assertThat(user.isActive()).isTrue();
    }

    @Test
    void 삭제_상태_확인() {
        // given
        User user = User.createSigning();
        user.delete();

        // when & then
        assertThat(user.isActive()).isFalse();
    }

    @Test
    void 일반_사용자는_리소스_접근_가능() {
        // given
        User user = User.createSigning();
        user.completeSignup("태형", new Nickname("태형닉네임"), new PhoneNumber("01012345678"));

        // when & then
        assertThat(user.canAccessResource()).isTrue();
    }

    @Test
    void 가입중_사용자는_리소스_접근_불가() {
        // given
        User user = User.createSigning();

        // when & then
        assertThat(user.canAccessResource()).isFalse();
    }

    @Test
    void 삭제된_사용자는_리소스_접근_불가() {
        // given
        User user = User.createSigning();
        user.completeSignup("태형", new Nickname("태형닉네임"), new PhoneNumber("01012345678"));
        user.delete();

        // when & then
        assertThat(user.canAccessResource()).isFalse();
    }

    @Test
    void 닉네임과_전화번호_일치_확인() {
        // given
        User user = User.createSigning();
        Nickname nickname = new Nickname("태형닉네임");
        PhoneNumber phoneNumber = new PhoneNumber("01012345678");
        user.completeSignup("태형", nickname, phoneNumber);

        // when
        boolean matches = user.matchesForConnection(nickname, phoneNumber);

        // then
        assertThat(matches).isTrue();
    }

    @Test
    void 닉네임이_다르면_false() {
        // given
        User user = User.createSigning();
        user.completeSignup("태형", new Nickname("태형닉네임"), new PhoneNumber("01012345678"));

        // when
        boolean matches = user.matchesForConnection(new Nickname("다른닉네임"), new PhoneNumber("01012345678"));

        // then
        assertThat(matches).isFalse();
    }

    @Test
    void 전화번호가_다르면_false() {
        // given
        User user = User.createSigning();
        user.completeSignup("태형", new Nickname("태형닉네임"), new PhoneNumber("01012345678"));

        // when
        boolean matches = user.matchesForConnection(new Nickname("태형닉네임"), new PhoneNumber("01087654321"));

        // then
        assertThat(matches).isFalse();
    }

    @Test
    void OAuth계정_추가() {
        // given
        User user = User.createSigning();
        Long oauthAccountId = 1L;

        // when
        user.addOAuthAccount(oauthAccountId);

        // then
        assertThat(user.getOauthAccountIds()).contains(oauthAccountId);
    }

    @Test
    void OAuth계정_제거() {
        // given
        User user = User.createSigning();
        Long oauthAccountId = 1L;
        user.addOAuthAccount(oauthAccountId);

        // when
        user.removeOAuthAccount(oauthAccountId);

        // then
        assertThat(user.getOauthAccountIds()).doesNotContain(oauthAccountId);
    }

    @Test
    void 사용자_삭제() {
        // given
        User user = User.createSigning();

        // when
        user.delete();

        // then
        assertThat(user.getStatus()).isEqualTo(UserStatus.DELETED);
        assertThat(user.getDeletedAt()).isNotNull();
    }

    @Test
    void 이미_삭제된_사용자는_다시_삭제_불가() {
        // given
        User user = User.createSigning();
        user.delete();

        // when & then
        assertThatThrownBy(() -> user.delete())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 삭제");
    }
}