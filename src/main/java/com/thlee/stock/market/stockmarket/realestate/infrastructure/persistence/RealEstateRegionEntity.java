package com.thlee.stock.market.stockmarket.realestate.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.realestate.domain.model.RegionLevel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 부동산 시장 데이터 지원 지역 (시도 + 시군구 + 읍면동) JPA Entity.
 * <p>
 * emd_code는 NULL 대신 빈 문자열("") sentinel로 정규화. region 단위 구분은 {@code level} 컬럼이 권위 —
 * 기존 emd_code 기반 sentinel 분기는 호출처에서 level predicate로 전환한다 (Unit 1.5).
 * <p>
 * {@code level} 컬럼은 region_code 길이에서 derive되는 derived persistent 필드.
 * write path는 {@link #syncLevel()} hook이 강제 동기화, read path는 {@link #verifyLevel()} hook이
 * drift fail-fast로 보장한다.
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
                @Index(name = "idx_re_region_code", columnList = "region_code"),
                @Index(name = "idx_re_region_level", columnList = "level")
        }
)
@IdClass(RealEstateRegionEntity.RegionId.class)
@Getter
@Slf4j
public class RealEstateRegionEntity {

    /**
     * drift ERROR 로그 throttle 캐시 — 동일 row PK는 JVM 라이프타임당 1회만 ERROR 로그.
     * row가 매 쿼리 hydration될 때마다 로그 storm을 방지. JVM 재시작 시 초기화 (운영 사고 가시성 유지).
     */
    private static final Set<String> DRIFT_LOGGED = ConcurrentHashMap.newKeySet();

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

    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false, length = 16)
    private RegionLevel level;

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
        // level은 @PrePersist hook이 derive (regionCode 기반)
    }

    /**
     * write path: region_code 길이에서 level을 강제 derive.
     * caller가 level 컬럼을 직접 설정해도 hook이 덮어씀.
     */
    @PrePersist
    @PreUpdate
    void syncLevel() {
        this.level = RegionLevel.fromCode(this.regionCode);
    }

    /**
     * read path: hydration 시 컬럼 값과 derive 결과 불일치를 자동 복구 + 경고 로그.
     * <p>
     * 이전 정책(fail-fast IllegalStateException)은 단일 row 손상으로 전체 service 차단 위험이
     * 있어 graceful로 변경. drift 발생 시 derive 값을 in-memory에 덮어쓰고 ERROR 로그로
     * 운영팀에 알린다. 영구 복구는 SQL UPDATE 또는 다음 write 시 @PreUpdate hook 수행.
     * <p>
     * region_code 자체가 비정상 길이인 경우(2/5 외)는 RegionLevel.fromCode가 throw하므로,
     * 그 케이스만 IllegalStateException으로 surface (데이터 손상 사례 — 부팅 차단 정당).
     */
    @PostLoad
    void verifyLevel() {
        RegionLevel derived;
        try {
            derived = RegionLevel.fromCode(this.regionCode);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "RealEstateRegionEntity 비정상 region_code=" + this.regionCode
                            + " — 데이터 audit 후 수정 필요.", e);
        }
        if (this.level != derived) {
            String driftKey = this.regionCode + "::" + (this.emdCode == null ? "" : this.emdCode);
            if (DRIFT_LOGGED.add(driftKey)) {
                // row 단위 첫 발견 시 1회 ERROR 로그 (JVM 라이프타임 동안 dedupe)
                log.error("[realestate.region] level drift: region_code={}, emd_code={}, column={}, derived={}"
                                + " — auto-correct in memory; 영구 복구는 SQL UPDATE 필요",
                        this.regionCode, this.emdCode, this.level, derived);
            } else {
                // 동일 row 재발 시 DEBUG 레벨로 강등 (storm 차단)
                log.debug("[realestate.region] level drift (recurring): region_code={}, emd_code={}",
                        this.regionCode, this.emdCode);
            }
            this.level = derived;
        }
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
