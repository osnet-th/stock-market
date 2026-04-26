package com.thlee.stock.market.stockmarket.stocknote.domain.repository;

import com.thlee.stock.market.stockmarket.stocknote.domain.model.StockNote;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 주식 기록 본체 Repository 포트.
 *
 * <p>모든 조회는 {@code userId} 스코프로 제한되며, IDOR 방지를 위해
 * {@link #findByIdAndUserId} 패턴을 강제한다.
 */
public interface StockNoteRepository {

    StockNote save(StockNote note);

    /** 권한이 있는 사용자만 조회 가능. 404 통일을 위해 Optional 반환. */
    Optional<StockNote> findByIdAndUserId(Long id, Long userId);

    /**
     * 내부 async/배치 전용 조회. userId 스코프가 없으므로 Controller/Service 의 공개 경로에서는
     * 호출하지 말 것. 스냅샷 캡처/스케줄러가 이벤트 payload 의 noteId 로 노트 정보를 얻을 때 사용.
     */
    Optional<StockNote> findById(Long id);

    /** 다건 IN-batch 조회 (PatternMatch 등 N+1 회피용). 결과 키는 noteId. */
    Map<Long, StockNote> findAllByIds(Collection<Long> noteIds);

    /** 필터/페이징 리스트. 구현 레이어는 복합 인덱스 {@code (user_id, note_date DESC)} 를 활용한다. */
    List<StockNote> findList(Long userId, StockNoteListFilter filter);

    long countList(Long userId, StockNoteListFilter filter);

    /** 종목별 차트를 위한 기록점 조회 (noteDate 오름차순). */
    List<StockNote> findByUserAndStock(Long userId, String stockCode, LocalDate fromDate, LocalDate toDate);

    void deleteByIdAndUserId(Long id, Long userId);
}