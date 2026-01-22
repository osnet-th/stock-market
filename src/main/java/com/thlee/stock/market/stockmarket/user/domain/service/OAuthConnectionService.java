package com.thlee.stock.market.stockmarket.user.domain.service;

import com.thlee.stock.market.stockmarket.user.domain.exception.DuplicateOAuthProviderException;
import com.thlee.stock.market.stockmarket.user.domain.model.OAuthAccount;
import com.thlee.stock.market.stockmarket.user.domain.model.OAuthProvider;
import com.thlee.stock.market.stockmarket.user.domain.model.User;
import com.thlee.stock.market.stockmarket.user.domain.repository.OAuthAccountRepository;
import com.thlee.stock.market.stockmarket.user.domain.repository.UserRepository;

import java.util.List;

/**
 * OAuth 연결 관리 도메인 서비스
 * User와 OAuthAccount 간의 연결을 관리하고 provider 중복을 검증합니다.
 */
public class OAuthConnectionService {
    private final UserRepository userRepository;
    private final OAuthAccountRepository oauthAccountRepository;

    public OAuthConnectionService(UserRepository userRepository, OAuthAccountRepository oauthAccountRepository) {
        this.userRepository = userRepository;
        this.oauthAccountRepository = oauthAccountRepository;
    }

    /**
     * 해당 provider 연결 가능 여부 확인
     */
    public boolean canConnectProvider(User user, OAuthProvider provider) {
        List<OAuthAccount> oauthAccounts = oauthAccountRepository.findByIdIn(user.getOauthAccountIds());

        return oauthAccounts.stream()
                .noneMatch(account -> account.isSameProvider(provider));
    }

    /**
     * OAuth 계정 연결
     */
    public void connectOAuthAccount(User user, OAuthAccount oauthAccount) {
        if (!canConnectProvider(user, oauthAccount.getProvider())) {
            throw new DuplicateOAuthProviderException(oauthAccount.getProvider());
        }

        // ID 생성을 위해 먼저 저장
        User savedUser = userRepository.save(user);
        OAuthAccount savedOAuthAccount = oauthAccountRepository.save(oauthAccount);

        // 연결 설정
        savedOAuthAccount.connectToUser(savedUser.getId());
        savedUser.addOAuthAccount(savedOAuthAccount.getId());

        // 연결 정보 저장
        userRepository.save(savedUser);
        oauthAccountRepository.save(savedOAuthAccount);
    }

    /**
     * 특정 provider 보유 여부 확인
     */
    public boolean hasProvider(User user, OAuthProvider provider) {
        List<OAuthAccount> oauthAccounts = oauthAccountRepository.findByIdIn(user.getOauthAccountIds());

        return oauthAccounts.stream()
                .anyMatch(account -> account.isSameProvider(provider));
    }

    /**
     * OAuth 계정 연결 해제
     */
    public void disconnectOAuthAccount(User user, Long oauthAccountId) {
        user.removeOAuthAccount(oauthAccountId);
        userRepository.save(user);
    }
}