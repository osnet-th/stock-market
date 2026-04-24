package com.thlee.stock.market.stockmarket.stocknote.domain.repository;

import com.thlee.stock.market.stockmarket.stocknote.domain.model.StockNoteTag;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.NoteDirection;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 기록별 태그 Repository 포트.
 *
 * <p>고정 enum 태그(TRIGGER/CHARACTER/SUPPLY) 와 자유 태그(CUSTOM) 가 {@code tag_source}
 * 로 구분되어 한 테이블에 저장된다. 사용자 스코프 필터 성능을 위해 {@code user_id}
 * denormalize 컬럼을 포함한다.
 */
public interface StockNoteTagRepository {

    List<StockNoteTag> saveAll(List<StockNoteTag> tags);

    List<StockNoteTag> findByNoteId(Long noteId);

    /** 다건 기록의 태그를 한 번에 조회 (N+1 회피 배치 fetch). 키는 noteId. */
    Map<Long, List<StockNoteTag>> findAllByNoteIds(Collection<Long> noteIds);

    void deleteByNoteId(Long noteId);

    /**
     * 태그 조합 매칭. 입력한 모든 태그 쌍을 포함하는 다른 기록의 noteId 목록 반환.
     *
     * <p>security 리뷰 권고(H-A03): 튜플 IN 바인딩 드라이버 편차 회피를 위해 동적 placeholder 로
     * 각 {@code (source, value)} 쌍을 개별 setParameter 한다.
     *
     * @param userId         스코프 사용자
     * @param tags           매칭 기준 태그 목록
     * @param excludeNoteId  자기 자신 제외용 noteId
     * @param directionFilter null 이면 방향 무관, 값이 있으면 해당 direction 만
     */
    List<Long> findNoteIdsMatchingAllTags(Long userId, List<TagPair> tags,
                                          Long excludeNoteId, NoteDirection directionFilter);

    record TagPair(String source, String value) { }
}