package com.thlee.stock.market.stockmarket.realestate.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.Getter;

import java.io.Serializable;
import java.util.Objects;

/**
 * 부동산 시장 데이터 지원 지역 (시군구 + 읍면동) JPA Entity.
 * <p>
 * emd_code는 NULL 대신 빈 문자열("") sentinel로 정규화 — 시군구 단위 row와
 * 읍면동 단위 row를 표준 @UniqueConstraint로 구분한다 (ddl-auto: update 호환).
 */
@Entity
@Table(
        name = "real_estate_region",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_re_region_code_emd",
                columnNames = {"region_code", "emd_code"}
        ),
        indexes = {
                @Index(name = "idx_re_region_sido", columnList = "sido_code"),
                @Index(name = "idx_re_region_code", columnList = "region_code")
        }
)
@IdClass(RealEstateRegionEntity.RegionId.class)
@Getter
public class RealEstateRegionEntity {

    @Id
    @Column(name = "region_code", nullable = false, length = 5)
    private String regionCode;

    @Id
    @Column(name = "emd_code", nullable = false, length = 8)
    private String emdCode;

    @Column(name = "sido_code", nullable = false, length = 2)
    private String sidoCode;

    @Column(name = "sido_name", nullable = false, length = 30)
    private String sidoName;

    @Column(name = "sigungu_name", nullable = false, length = 50)
    private String sigunguName;

    @Column(name = "emd_name", length = 50)
    private String emdName;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    protected RealEstateRegionEntity() {
    }

    public RealEstateRegionEntity(String regionCode,
                                  String emdCode,
                                  String sidoCode,
                                  String sidoName,
                                  String sigunguName,
                                  String emdName,
                                  int displayOrder) {
        this.regionCode = regionCode;
        this.emdCode = emdCode;
        this.sidoCode = sidoCode;
        this.sidoName = sidoName;
        this.sigunguName = sigunguName;
        this.emdName = emdName;
        this.displayOrder = displayOrder;
    }

    public static class RegionId implements Serializable {
        private String regionCode;
        private String emdCode;

        public RegionId() {
        }

        public RegionId(String regionCode, String emdCode) {
            this.regionCode = regionCode;
            this.emdCode = emdCode;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RegionId that = (RegionId) o;
            return Objects.equals(regionCode, that.regionCode)
                    && Objects.equals(emdCode, that.emdCode);
        }

        @Override
        public int hashCode() {
            return Objects.hash(regionCode, emdCode);
        }
    }
}
