package com.thlee.stock.market.stockmarket.user.application;

import com.thlee.stock.market.stockmarket.user.application.dto.AccountConnectionRequest;
import com.thlee.stock.market.stockmarket.user.application.dto.OAuthLoginRequest;
import com.thlee.stock.market.stockmarket.user.application.dto.OAuthLoginResponse;
import com.thlee.stock.market.stockmarket.user.application.dto.SignupCompleteRequest;
import com.thlee.stock.market.stockmarket.user.domain.model.*;
import com.thlee.stock.market.stockmarket.user.domain.repository.OAuthAccountRepository;
import com.thlee.stock.market.stockmarket.user.domain.repository.UserRepository;
import com.thlee.stock.market.stockmarket.user.domain.service.JwtTokenProvider;
import com.thlee.stock.market.stockmarket.user.domain.service.OAuthConnectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OAuthLoginServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OAuthAccountRepository oauthAccountRepository;

    @Mock
    private OAuthConnectionService oauthConnectionService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private KakaoOAuthService kakaoOAuthService;

    private OAuthLoginService oauthLoginService;

    @BeforeEach
    void setUp() {
        oauthLoginService = new OAuthLoginService(
                userRepository,
                oauthAccountRepository,
                oauthConnectionService,
                jwtTokenProvider,
                kakaoOAuthService
        );
    }

    @Test
    @DisplayName("기존 OAuth 계정으로 로그인 성공")
    void login_WithExistingOAuthAccount_Success() {
        // Given
        OAuthProvider provider = OAuthProvider.KAKAO;
        String issuer = "https://kauth.kakao.com";
        String subject = "12345";
        String email = "test@example.com";
        Long userId = 1L;
        Long oauthAccountId = 10L;

        OAuthLoginRequest request = new OAuthLoginRequest(provider, issuer, subject, email);

        // Repository에서 조회된 OAuthAccount (이미 userId 연결됨)
        OAuthAccount oauthAccount = new OAuthAccount(oauthAccountId, userId, provider, issuer, subject, email, LocalDateTime.now());

        // Repository에서 조회된 User (이미 ID 있음)
        User user = new User(userId, null, null, null,
                new ArrayList<>(), UserStatus.ACTIVE, UserRole.SIGNING_USER,
                LocalDateTime.now(), null);

        given(oauthAccountRepository.findByProviderAndIssuerAndSubject(provider, issuer, subject))
                .willReturn(Optional.of(oauthAccount));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(jwtTokenProvider.generateAccessToken(userId, UserRole.SIGNING_USER))
                .willReturn("access-token");
        given(jwtTokenProvider.generateRefreshToken(userId))
                .willReturn("refresh-token");

        // When
        OAuthLoginResponse response = oauthLoginService.login(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.role()).isEqualTo(UserRole.SIGNING_USER);
    }

    @Test
    @DisplayName("신규 OAuth 계정으로 로그인 시 SIGNING_USER 생성")
    void login_WithNewOAuthAccount_CreateSigningUser() {
        // Given
        OAuthProvider provider = OAuthProvider.GOOGLE;
        String issuer = "https://accounts.google.com";
        String subject = "67890";
        String email = "newuser@example.com";
        Long newUserId = 2L;
        Long newOAuthAccountId = 20L;

        OAuthLoginRequest request = new OAuthLoginRequest(provider, issuer, subject, email);

        given(oauthAccountRepository.findByProviderAndIssuerAndSubject(provider, issuer, subject))
                .willReturn(Optional.empty());

        // Repository에서 ID가 설정된 User를 반환
        User savedUser = new User(newUserId, null, null, null,
                new ArrayList<>(), UserStatus.ACTIVE, UserRole.SIGNING_USER,
                LocalDateTime.now(), null);
        given(userRepository.save(any(User.class))).willReturn(savedUser);

        // Repository에서 ID가 설정된 OAuthAccount를 반환
        OAuthAccount savedOAuthAccount = new OAuthAccount(newOAuthAccountId, newUserId,
                provider, issuer, subject, email, LocalDateTime.now());
        given(oauthAccountRepository.save(any(OAuthAccount.class))).willReturn(savedOAuthAccount);

        given(jwtTokenProvider.generateAccessToken(newUserId, UserRole.SIGNING_USER))
                .willReturn("new-access-token");
        given(jwtTokenProvider.generateRefreshToken(newUserId))
                .willReturn("new-refresh-token");

        // When
        OAuthLoginResponse response = oauthLoginService.login(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("new-access-token");
        assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
        assertThat(response.role()).isEqualTo(UserRole.SIGNING_USER);

        verify(userRepository, times(2)).save(any(User.class));  // newUser, savedUser 각각 저장
        verify(oauthAccountRepository).save(any(OAuthAccount.class));
    }

    @Test
    @DisplayName("가입 완료 처리 성공")
    void completeSignup_Success() {
        // Given
        Long userId = 1L;
        String name = "홍길동";
        String nicknameValue = "testuser";
        String phoneNumberValue = "01012345678";

        SignupCompleteRequest request = new SignupCompleteRequest(name, nicknameValue, phoneNumberValue);

        User signingUser = User.createSigning();
        Nickname nickname = new Nickname(nicknameValue);
        PhoneNumber phoneNumber = new PhoneNumber(phoneNumberValue);

        given(userRepository.findById(userId)).willReturn(Optional.of(signingUser));
        given(userRepository.existsByNickname(nickname)).willReturn(false);
        given(userRepository.save(any(User.class))).willReturn(signingUser);

        // When
        oauthLoginService.completeSignup(userId, request);

        // Then
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("가입 완료 시 닉네임 중복으로 실패")
    void completeSignup_WithDuplicateNickname_ThrowsException() {
        // Given
        Long userId = 1L;
        String name = "홍길동";
        String nicknameValue = "duplicate";
        String phoneNumberValue = "01012345678";

        SignupCompleteRequest request = new SignupCompleteRequest(name, nicknameValue, phoneNumberValue);

        User signingUser = User.createSigning();
        Nickname nickname = new Nickname(nicknameValue);

        given(userRepository.findById(userId)).willReturn(Optional.of(signingUser));
        given(userRepository.existsByNickname(nickname)).willReturn(true);

        // When & Then
        assertThatThrownBy(() -> oauthLoginService.completeSignup(userId, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("이미 사용중인 닉네임");
    }

    @Test
    @DisplayName("기존 계정 연결 성공")
    void connectAccount_Success() {
        // Given
        Long signingUserId = 1L;
        Long existingUserId = 2L;
        String nicknameValue = "existinguser";
        String phoneNumberValue = "01098765432";
        Long oauthAccountId = 10L;

        AccountConnectionRequest request = new AccountConnectionRequest(nicknameValue, phoneNumberValue);

        Nickname nickname = new Nickname(nicknameValue);
        PhoneNumber phoneNumber = new PhoneNumber(phoneNumberValue);

        // Repository에서 조회된 가입중 상태 사용자 (ID 포함)
        ArrayList<Long> signingUserOAuthIds = new ArrayList<>();
        signingUserOAuthIds.add(oauthAccountId);
        User signingUser = new User(signingUserId, null, null, null,
                signingUserOAuthIds, UserStatus.ACTIVE, UserRole.SIGNING_USER,
                LocalDateTime.now(), null);

        // Repository에서 조회된 기존 사용자 (ID 포함)
        User existingUser = new User(existingUserId, "기존사용자", nickname, phoneNumber,
                new ArrayList<>(), UserStatus.ACTIVE, UserRole.USER,
                LocalDateTime.now(), null);

        // Repository에서 조회된 OAuthAccount (ID 포함)
        OAuthAccount oauthAccount = new OAuthAccount(oauthAccountId, signingUserId,
                OAuthProvider.KAKAO, "https://kauth.kakao.com", "12345",
                "test@example.com", LocalDateTime.now());

        given(userRepository.findById(signingUserId)).willReturn(Optional.of(signingUser));
        given(userRepository.findByNicknameAndPhoneNumber(nickname, phoneNumber))
                .willReturn(Optional.of(existingUser));
        given(oauthAccountRepository.findById(oauthAccountId)).willReturn(Optional.of(oauthAccount));
        given(oauthConnectionService.canConnectProvider(any(User.class), any(OAuthProvider.class)))
                .willReturn(true);

        // When
        oauthLoginService.connectAccount(signingUserId, request);

        // Then
        verify(oauthConnectionService).connectOAuthAccount(any(User.class), any(OAuthAccount.class));
        verify(userRepository, times(2)).save(any(User.class));  // existingUser, signingUser 각각 저장
    }

    @Test
    @DisplayName("기존 계정 연결 실패 - 일치하는 사용자 없음")
    void connectAccount_WithNoMatchingUser_ThrowsException() {
        // Given
        Long signingUserId = 1L;
        String nicknameValue = "nonexistent";
        String phoneNumberValue = "01011111111";

        AccountConnectionRequest request = new AccountConnectionRequest(nicknameValue, phoneNumberValue);

        User signingUser = User.createSigning();
        Nickname nickname = new Nickname(nicknameValue);
        PhoneNumber phoneNumber = new PhoneNumber(phoneNumberValue);

        given(userRepository.findById(signingUserId)).willReturn(Optional.of(signingUser));
        given(userRepository.findByNicknameAndPhoneNumber(nickname, phoneNumber))
                .willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> oauthLoginService.connectAccount(signingUserId, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("일치하는 사용자를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("카카오 인가 코드로 로그인 성공 - 기존 계정")
    void loginWithKakao_WithExistingAccount_Success() {
        // Given
        String authorizationCode = "test-code-12345";
        OAuthProvider provider = OAuthProvider.KAKAO;
        String issuer = "https://kauth.kakao.com";
        String subject = "12345";
        String email = "test@kakao.com";
        Long userId = 1L;
        Long oauthAccountId = 10L;

        OAuthLoginRequest request = new OAuthLoginRequest(provider, issuer, subject, email);

        // KakaoOAuthService에서 OAuthLoginRequest 반환
        given(kakaoOAuthService.loginWithKakao(authorizationCode)).willReturn(request);

        // 기존 계정 조회
        OAuthAccount oauthAccount = new OAuthAccount(oauthAccountId, userId, provider, issuer, subject, email, LocalDateTime.now());
        User user = new User(userId, null, null, null,
                new ArrayList<>(), UserStatus.ACTIVE, UserRole.USER,
                LocalDateTime.now(), null);

        given(oauthAccountRepository.findByProviderAndIssuerAndSubject(provider, issuer, subject))
                .willReturn(Optional.of(oauthAccount));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(jwtTokenProvider.generateAccessToken(userId, UserRole.USER))
                .willReturn("access-token");
        given(jwtTokenProvider.generateRefreshToken(userId))
                .willReturn("refresh-token");

        // When
        OAuthLoginResponse response = oauthLoginService.loginWithKakao(authorizationCode);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.role()).isEqualTo(UserRole.USER);
    }

    @Test
    @DisplayName("카카오 인가 코드로 로그인 성공 - 신규 계정")
    void loginWithKakao_WithNewAccount_Success() {
        // Given
        String authorizationCode = "test-code-12345";
        OAuthProvider provider = OAuthProvider.KAKAO;
        String issuer = "https://kauth.kakao.com";
        String subject = "67890";
        String email = "newuser@kakao.com";
        Long newUserId = 2L;
        Long newOAuthAccountId = 20L;

        OAuthLoginRequest request = new OAuthLoginRequest(provider, issuer, subject, email);

        // KakaoOAuthService에서 OAuthLoginRequest 반환
        given(kakaoOAuthService.loginWithKakao(authorizationCode)).willReturn(request);

        // 신규 계정
        given(oauthAccountRepository.findByProviderAndIssuerAndSubject(provider, issuer, subject))
                .willReturn(Optional.empty());

        User savedUser = new User(newUserId, null, null, null,
                new ArrayList<>(), UserStatus.ACTIVE, UserRole.SIGNING_USER,
                LocalDateTime.now(), null);
        given(userRepository.save(any(User.class))).willReturn(savedUser);

        OAuthAccount savedOAuthAccount = new OAuthAccount(newOAuthAccountId, newUserId,
                provider, issuer, subject, email, LocalDateTime.now());
        given(oauthAccountRepository.save(any(OAuthAccount.class))).willReturn(savedOAuthAccount);

        given(jwtTokenProvider.generateAccessToken(newUserId, UserRole.SIGNING_USER))
                .willReturn("new-access-token");
        given(jwtTokenProvider.generateRefreshToken(newUserId))
                .willReturn("new-refresh-token");

        // When
        OAuthLoginResponse response = oauthLoginService.loginWithKakao(authorizationCode);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("new-access-token");
        assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
        assertThat(response.role()).isEqualTo(UserRole.SIGNING_USER);
    }
}