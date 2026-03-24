package com.thlee.stock.market.stockmarket.economics.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.Getter;

import java.io.Serializable;
import java.util.Objects;

/**
 * ECOS 경제지표 메타데이터 JPA Entity
 */
@Entity
@Table(name = "ecos_indicator_metadata")
@IdClass(EcosIndicatorMetadataEntity.MetadataId.class)
@Getter
public class EcosIndicatorMetadataEntity {

    @Id
    @Column(name = "class_name", nullable = false, length = 100)
    private String className;

    @Id
    @Column(name = "keystat_name", nullable = false, length = 200)
    private String keystatName;

    @Column(name = "description", length = 500)
    private String description;

    protected EcosIndicatorMetadataEntity() {
    }

    public EcosIndicatorMetadataEntity(String className,
                                        String keystatName,
                                        String description) {
        this.className = className;
        this.keystatName = keystatName;
        this.description = description;
    }

    /**
     * 복합 PK 클래스
     */
    public static class MetadataId implements Serializable {
        private String className;
        private String keystatName;

        public MetadataId() {
        }

        public MetadataId(String className, String keystatName) {
            this.className = className;
            this.keystatName = keystatName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MetadataId that = (MetadataId) o;
            return Objects.equals(className, that.className)
                && Objects.equals(keystatName, that.keystatName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(className, keystatName);
        }
    }
}