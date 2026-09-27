package com.thlee.stock.market.stockmarket.news.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.news.domain.model.CollectionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 키워드 수집 시도 이력 JPA Entity (#115)
 *
 * <p>키워드 수 × 수집 주기만큼 쌓이므로 보존 정책이 필요하다
 * ({@code KeywordCollectionHistoryCleanupScheduler} 가 기간이 지난 행을 지운다).
 */
@Entity
@Table(
        name = "keyword_collection_history",
        indexes = {
                @Index(name = "idx_kch_keyword_attempted",
                       columnList = "keyword_id, attempted_at DESC"),
                @Index(name = "idx_kch_attempted",
                       columnList = "attempted_at DESC")
        }
)
@Getter
public class KeywordCollectionHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "keyword_id", nullable = false)
    private Long keywordId;

    @Column(name = "attempted_at", nullable = false)
    private LocalDateTime attemptedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CollectionStatus status;

    @Column(name = "saved_count", nullable = false)
    private int savedCount;

    @Column(name = "ignored_count", nullable = false)
    private int ignoredCount;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    protected KeywordCollectionHistoryEntity() {
    }

    public KeywordCollectionHistoryEntity(Long id,
                                          Long keywordId,
                                          LocalDateTime attemptedAt,
                                          CollectionStatus status,
                                          int savedCount,
                                          int ignoredCount,
                                          String errorMessage) {
        this.id = id;
        this.keywordId = keywordId;
        this.attemptedAt = attemptedAt;
        this.status = status;
        this.savedCount = savedCount;
        this.ignoredCount = ignoredCount;
        this.errorMessage = errorMessage;
    }
}
