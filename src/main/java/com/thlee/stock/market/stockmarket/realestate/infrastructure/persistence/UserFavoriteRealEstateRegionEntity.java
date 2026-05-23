package com.thlee.stock.market.stockmarket.realestate.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 사용자별 부동산 관심지역 entity.
 * <p>
 * emd_code는 NULL 대신 빈 문자열("") sentinel — 표현식 unique 회피로 ddl-auto: update 호환.
 * Unique: (user_id, sido_code, region_code, emd_code).
 */
@Entity
@Table(
        name = "user_favorite_real_estate_region",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_fav_re_region",
                columnNames = {"user_id", "sido_code", "region_code", "emd_code"}
        ),
        indexes = {
                @Index(name = "idx_user_fav_re_user", columnList = "user_id"),
                @Index(name = "idx_user_fav_re_region", columnList = "region_code")
        }
)
@Getter
public class UserFavoriteRealEstateRegionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "sido_code", nullable = false, length = 2)
    private String sidoCode;

    @Column(name = "region_code", nullable = false, length = 5)
    private String regionCode;

    @Column(name = "emd_code", nullable = false, length = 8)
    private String emdCode;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected UserFavoriteRealEstateRegionEntity() {
    }

    public UserFavoriteRealEstateRegionEntity(Long id,
                                              Long userId,
                                              String sidoCode,
                                              String regionCode,
                                              String emdCode,
                                              int displayOrder,
                                              LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.sidoCode = sidoCode;
        this.regionCode = regionCode;
        this.emdCode = emdCode;
        this.displayOrder = displayOrder;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
