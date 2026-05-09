package com.thlee.stock.market.stockmarket.favorite.application;

import com.thlee.stock.market.stockmarket.favorite.domain.model.FavoriteIndicatorSourceType;
import com.thlee.stock.market.stockmarket.favorite.domain.repository.FavoriteIndicatorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관심지표 INSERT를 별도 트랜잭션 경계로 격리한다.
 *
 * <p>FavoriteIndicatorService.toggle 의 retry 루프는 SQLState 23505 발생 시 같은
 * 트랜잭션을 재사용하면 PostgreSQL이 'transaction aborted (25P02)' 상태로 만들어
 * 후속 statement가 모두 거부된다. 별도 빈으로 분리해 {@link Propagation#REQUIRES_NEW}
 * 로 호출하면 매 attempt가 독립 트랜잭션으로 commit/rollback되어 retry 의도가 살아난다.
 *
 * <p>또한 INSERT는 단일 statement(`INSERT ... SELECT MAX(priority)+1`)이고 부수효과 없는
 * idempotent-ish operation이므로 outer 트랜잭션과 분리해도 안전하다.
 */
@Service
@RequiredArgsConstructor
public class FavoritePriorityInserter {

    private final FavoriteIndicatorRepository favoriteIndicatorRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void insert(Long userId,
                       FavoriteIndicatorSourceType sourceType,
                       String indicatorCode) {
        favoriteIndicatorRepository.insertWithNextPriority(userId, sourceType, indicatorCode);
    }
}
