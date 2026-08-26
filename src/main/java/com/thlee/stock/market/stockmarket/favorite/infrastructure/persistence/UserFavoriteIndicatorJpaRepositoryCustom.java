package com.thlee.stock.market.stockmarket.favorite.infrastructure.persistence;

import java.util.Map;

/**
 * 가변 길이 입력에 의존하는 동적 SQL이 필요한 메서드를 분리한 fragment.
 * Spring Data JPA의 custom-impl 패턴(`*Custom` 인터페이스 + `*Impl` 클래스)으로 main 리포지토리에 자동 합쳐진다.
 */
public interface UserFavoriteIndicatorJpaRepositoryCustom {

    /**
     * 주어진 (id, priority) 쌍에 따라 priority를 일괄 갱신.
     * Postgres `UNIQUE(user_id, source_type, priority) DEFERRABLE INITIALLY DEFERRED` 가정 하에 mid-tx 중복 priority 허용.
     * 실제 SQL은 항목 수에 따라 단일 CASE WHEN UPDATE 또는 chunk 분할로 발행된다.
     * 호출 후 EntityManager L1 cache는 flush + clear되어 후속 invariant 재조회가 stale 결과를 반환하지 않는다.
     *
     * @param idToPriority id별 새 priority. 비어있으면 즉시 0 반환.
     * @return 영향받은 row 수.
     */
    int bulkUpdatePriority(Map<Long, Integer> idToPriority);
}