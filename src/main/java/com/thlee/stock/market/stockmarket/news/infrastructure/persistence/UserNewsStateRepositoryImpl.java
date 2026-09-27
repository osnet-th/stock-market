package com.thlee.stock.market.stockmarket.news.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.news.domain.model.UserNewsState;
import com.thlee.stock.market.stockmarket.news.domain.repository.UserNewsStateRepository;
import com.thlee.stock.market.stockmarket.news.infrastructure.persistence.mapper.UserNewsStateMapper;
import com.thlee.stock.market.stockmarket.news.infrastructure.persistence.repository.UserNewsStateJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * UserNewsStateRepository 구현체 (Adapter)
 */
@Repository
@RequiredArgsConstructor
public class UserNewsStateRepositoryImpl implements UserNewsStateRepository {

    private final UserNewsStateJpaRepository jpaRepository;

    @Override
    public UserNewsState save(UserNewsState state) {
        UserNewsStateEntity saved = jpaRepository.save(UserNewsStateMapper.toEntity(state));
        return UserNewsStateMapper.toDomain(saved);
    }

    @Override
    public Optional<UserNewsState> findByUserIdAndNewsId(Long userId, Long newsId) {
        return jpaRepository.findByUserIdAndNewsId(userId, newsId)
                .map(UserNewsStateMapper::toDomain);
    }

    @Override
    public List<UserNewsState> findByUserIdAndNewsIds(Long userId, List<Long> newsIds) {
        if (newsIds == null || newsIds.isEmpty()) {
            return List.of();
        }
        return jpaRepository.findByUserIdAndNewsIdIn(userId, newsIds).stream()
                .map(UserNewsStateMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> findReadOriginalUrls(Long userId, List<Long> keywordIds, int limit) {
        if (keywordIds == null || keywordIds.isEmpty() || limit <= 0) {
            return List.of();
        }
        return jpaRepository.findReadOriginalUrls(userId, keywordIds, PageRequest.of(0, limit));
    }

    @Override
    public int markAllRead(Long userId, List<Long> newsIds) {
        if (newsIds == null || newsIds.isEmpty()) {
            return 0;
        }

        // 상태 행은 상호작용이 있을 때만 생기므로, 없는 뉴스는 새로 만들고 있는 것은 갱신한다
        Map<Long, UserNewsStateEntity> existing = jpaRepository
                .findByUserIdAndNewsIdIn(userId, newsIds).stream()
                .collect(Collectors.toMap(UserNewsStateEntity::getNewsId, Function.identity(), (a, b) -> a));

        List<UserNewsState> toSave = new ArrayList<>();
        for (Long newsId : newsIds) {
            UserNewsStateEntity entity = existing.get(newsId);
            if (entity == null) {
                UserNewsState created = UserNewsState.create(userId, newsId);
                created.markRead();
                toSave.add(created);
            } else if (!entity.isRead()) {
                UserNewsState state = UserNewsStateMapper.toDomain(entity);
                state.markRead();
                toSave.add(state);
            }
        }

        if (toSave.isEmpty()) {
            return 0;
        }
        jpaRepository.saveAll(toSave.stream().map(UserNewsStateMapper::toEntity).toList());
        return toSave.size();
    }
}
