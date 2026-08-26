package com.thlee.stock.market.stockmarket.glossary.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 용어 사전 카테고리 Entity.
 *
 * <p>{@code (user_id, name)} unique 로 중복 등록을 방지한다.
 * race 시 application 계층은 {@code DataIntegrityViolationException} 을 그대로
 * GlobalExceptionHandler 의 409 로 노출한다.
 */
@Entity
@Table(
        name = "glossary_category",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_glossary_category_user_name",
                        columnNames = {"user_id", "name"})
        },
        indexes = {
                @Index(name = "idx_glossary_category_user_id", columnList = "user_id")
        }
)
@Getter
public class GlossaryCategoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected GlossaryCategoryEntity() {
    }

    public GlossaryCategoryEntity(Long id, Long userId, String name,
                                  LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}