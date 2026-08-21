package com.thlee.stock.market.stockmarket.glossary.infrastructure.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 용어 사전 본체 Entity.
 *
 * <p>{@link GlossaryCategoryEntity} 와 연관관계 없이 ID 참조만 사용한다 (CLAUDE.md 규칙).
 * {@code category_id == null} 인 row 는 "미분류" 가상 카테고리로 노출된다 (R5).
 *
 * <p>함께 볼 용어({@code relatedTermIds})도 Entity 연관 없이 ID 값 컬렉션
 * ({@code glossary_term_related}) 으로만 보관한다. JPQL 벌크 DELETE 는 컬렉션 행을
 * 지우지 않으므로, 용어 삭제 시 {@link GlossaryTermJpaRepository#deleteRelationsForTerm(Long)}
 * 로 양방향(참조하는/참조받는) 정리를 별도 수행한다.
 */
@Entity
@Table(
        name = "glossary_term",
        indexes = {
                @Index(name = "idx_glossary_term_user_id", columnList = "user_id"),
                @Index(name = "idx_glossary_term_user_category", columnList = "user_id, category_id")
        }
)
@Getter
public class GlossaryTermEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    /** 약어 · 영문 (선택) */
    @Column(name = "abbreviation", length = 200)
    private String abbreviation;

    /** 한 줄 정의 — 목록/함께 볼 용어 카드 노출용 (선택) */
    @Column(name = "one_line", length = 300)
    private String oneLine;

    /** 풀이 본문 — 기존 '설명' 컬럼을 rename 없이 재해석 (선택) */
    @Column(name = "definition", columnDefinition = "TEXT")
    private String definition;

    /** 기준 · 읽는 법 (선택) */
    @Column(name = "scale_note", columnDefinition = "TEXT")
    private String scaleNote;

    /** 예시 (선택) */
    @Column(name = "example", columnDefinition = "TEXT")
    private String example;

    /** 투자 관점 (선택) */
    @Column(name = "takeaway", columnDefinition = "TEXT")
    private String takeaway;

    @Column(name = "category_id")
    private Long categoryId;

    /**
     * 함께 볼 용어 ID 목록 (같은 사용자 용어만 — application 계층 검증). 입력 순서 보존.
     *
     * <p>FK 미사용 (ID 참조 컨벤션 + 용어 row 를 먼저 지우는 삭제 순서가 FK 와 충돌).
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "glossary_term_related",
            joinColumns = @JoinColumn(name = "term_id"),
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT),
            indexes = @Index(name = "idx_glossary_term_related_target", columnList = "related_term_id")
    )
    @OrderColumn(name = "position")
    @Column(name = "related_term_id", nullable = false)
    @BatchSize(size = 200)
    private List<Long> relatedTermIds;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected GlossaryTermEntity() {
    }

    public GlossaryTermEntity(Long id, Long userId, String name,
                              String abbreviation, String oneLine, String definition,
                              String scaleNote, String example, String takeaway,
                              Long categoryId, List<Long> relatedTermIds,
                              LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.abbreviation = abbreviation;
        this.oneLine = oneLine;
        this.definition = definition;
        this.scaleNote = scaleNote;
        this.example = example;
        this.takeaway = takeaway;
        this.categoryId = categoryId;
        this.relatedTermIds = relatedTermIds;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
