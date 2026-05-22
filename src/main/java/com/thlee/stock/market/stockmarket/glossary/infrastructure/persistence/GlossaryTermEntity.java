package com.thlee.stock.market.stockmarket.glossary.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 용어 사전 본체 Entity.
 *
 * <p>{@link GlossaryCategoryEntity} 와 연관관계 없이 ID 참조만 사용한다 (CLAUDE.md 규칙).
 * {@code category_id == null} 인 row 는 "미분류" 가상 카테고리로 노출된다 (R5).
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

    @Column(name = "definition", columnDefinition = "TEXT")
    private String definition;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected GlossaryTermEntity() {
    }

    public GlossaryTermEntity(Long id, Long userId, String name, String definition, Long categoryId,
                              LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.definition = definition;
        this.categoryId = categoryId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}