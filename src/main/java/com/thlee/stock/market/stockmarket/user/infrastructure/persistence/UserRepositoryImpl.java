package com.thlee.stock.market.stockmarket.user.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.user.domain.model.Nickname;
import com.thlee.stock.market.stockmarket.user.domain.model.PhoneNumber;
import com.thlee.stock.market.stockmarket.user.domain.model.User;
import com.thlee.stock.market.stockmarket.user.domain.repository.OAuthAccountRepository;
import com.thlee.stock.market.stockmarket.user.domain.repository.UserRepository;
import com.thlee.stock.market.stockmarket.user.infrastructure.persistence.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * UserRepository 구현체 (Adapter)
 */
@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository userJpaRepository;
    private final OAuthAccountRepository oauthAccountRepository;

    @Override
    public User save(User user) {
        UserEntity entity = UserMapper.toEntity(user);
        UserEntity savedEntity = userJpaRepository.save(entity);

        // OAuthAccount IDs 조회
        List<Long> oauthAccountIds = user.getOauthAccountIds();

        return UserMapper.toDomain(savedEntity, oauthAccountIds);
    }

    @Override
    public Optional<User> findById(Long id) {
        return userJpaRepository.findById(id)
                .map(entity -> {
                    // OAuthAccount IDs 조회
                    List<Long> oauthAccountIds = oauthAccountRepository.findByUserId(id)
                            .stream()
                            .map(oauthAccount -> oauthAccount.getId())
                            .collect(Collectors.toList());

                    return UserMapper.toDomain(entity, oauthAccountIds);
                });
    }

    @Override
    public boolean existsByNickname(Nickname nickname) {
        return userJpaRepository.existsByNickname(nickname.getValue());
    }

    @Override
    public Optional<User> findByNicknameAndPhoneNumber(Nickname nickname, PhoneNumber phoneNumber) {
        return userJpaRepository.findByNicknameAndPhoneNumber(
                        nickname.getValue(),
                        phoneNumber.getValue()
                )
                .map(entity -> {
                    // OAuthAccount IDs 조회
                    List<Long> oauthAccountIds = oauthAccountRepository.findByUserId(entity.getId())
                            .stream()
                            .map(oauthAccount -> oauthAccount.getId())
                            .collect(Collectors.toList());

                    return UserMapper.toDomain(entity, oauthAccountIds);
                });
    }
}