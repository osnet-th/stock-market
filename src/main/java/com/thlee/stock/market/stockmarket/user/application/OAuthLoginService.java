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
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * OAuth 소셜 로그인 유스케이스 처리
 */
@Service
public class OAuthLoginService {

    private final UserRepository userRepository;
    private final OAuthAccountRepository oauthAccountRepository;
    private final OAuthConnectionService oauthConnectionService;
    private final JwtTokenProvider jwtTokenProvider;
    private final KakaoOAuthService kakaoOAuthService;

    public OAuthLoginService(
            UserRepository userRepository,
            OAuthAccountRepository oauthAccountRepository,
            OAuthConnectionService oauthConnectionService,
            JwtTokenProvider jwtTokenProvider,
            KakaoOAuthService kakaoOAuthService
    ) {
        this.userRepository = userRepository;
        this.oauthAccountRepository = oauthAccountRepository;
        this.oauthConnectionService = oauthConnectionService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.kakaoOAuthService = kakaoOAuthService;
    }

    /**
     * OAuth 로그인 처리
     * - 기존 계정이 있으면 로그인
     * - 신규 계정이면 SIGNING_USER 생성
     */
    public OAuthLoginResponse login(OAuthLoginRequest request) {
        OAuthProvider provider = request.provider();
        String issuer = request.issuer();
        String subject = request.subject();
        String email = request.email();

        // provider + issuer + subject로 OAuth 계정 조회
        Optional<OAuthAccount> existingAccount = oauthAccountRepository
                .findByProviderAndIssuerAndSubject(provider, issuer, subject);

        if (existingAccount.isPresent()) {
            // 기존 계정으로 로그인
            OAuthAccount oauthAccount = existingAccount.get();
            Long userId = oauthAccount.getUserId();

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

            String accessToken = jwtTokenProvider.generateAccessToken(userId, user.getRole());
            String refreshToken = jwtTokenProvider.generateRefreshToken(userId);

            return new OAuthLoginResponse(accessToken, refreshToken, userId, user.getRole());
        } else {
            // 신규 계정 생성 (SIGNING_USER)
            User newUser = User.createSigning();
            User savedUser = userRepository.save(newUser);

            OAuthAccount newOAuthAccount = OAuthAccount.create(provider, issuer, subject, email);
            newOAuthAccount.connectToUser(savedUser.getId());
            OAuthAccount savedOAuthAccount = oauthAccountRepository.save(newOAuthAccount);

            savedUser.addOAuthAccount(savedOAuthAccount.getId());
            userRepository.save(savedUser);

            Long userId = savedUser.getId();
            String accessToken = jwtTokenProvider.generateAccessToken(userId, savedUser.getRole());
            String refreshToken = jwtTokenProvider.generateRefreshToken(userId);

            return new OAuthLoginResponse(accessToken, refreshToken, userId, savedUser.getRole());
        }
    }

    /**
     * 카카오 인가 코드로 로그인 처리
     *
     * @param authorizationCode 카카오 인가 코드
     * @return OAuthLoginResponse
     */
    public OAuthLoginResponse loginWithKakao(String authorizationCode) {
        OAuthLoginRequest request = kakaoOAuthService.loginWithKakao(authorizationCode);
        return login(request);
    }

    /**
     * 가입 완료 처리
     */
    public void completeSignup(Long userId, SignupCompleteRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        Nickname nickname = new Nickname(request.nickname());
        PhoneNumber phoneNumber = new PhoneNumber(request.phoneNumber());

        // 닉네임 중복 확인
        if (userRepository.existsByNickname(nickname)) {
            throw new RuntimeException("이미 사용중인 닉네임입니다.");
        }

        user.completeSignup(request.name(), nickname, phoneNumber);
        userRepository.save(user);
    }

    /**
     * 기존 계정 연결 처리
     */
    public void connectAccount(Long signingUserId, AccountConnectionRequest request) {
        User signingUser = userRepository.findById(signingUserId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        Nickname nickname = new Nickname(request.nickname());
        PhoneNumber phoneNumber = new PhoneNumber(request.phoneNumber());

        // 닉네임과 전화번호로 사용자 조회
        User existingUser = userRepository.findByNicknameAndPhoneNumber(nickname, phoneNumber)
                .orElseThrow(() -> new RuntimeException("일치하는 사용자를 찾을 수 없습니다."));

        // 가입중 상태 사용자의 OAuth 계정을 기존 사용자에게 연결
        Long oauthAccountId = signingUser.getOauthAccountIds().get(0);
        OAuthAccount oauthAccount = oauthAccountRepository.findById(oauthAccountId)
                .orElseThrow(() -> new RuntimeException("OAuth 계정을 찾을 수 없습니다."));

        // provider 중복 검증 및 연결
        if (!oauthConnectionService.canConnectProvider(existingUser, oauthAccount.getProvider())) {
            throw new RuntimeException("이미 동일한 OAuth 제공자가 연결되어 있습니다.");
        }

        oauthConnectionService.connectOAuthAccount(existingUser, oauthAccount);
        userRepository.save(existingUser);

        // 가입중 사용자 삭제
        signingUser.delete();
        userRepository.save(signingUser);
    }
}