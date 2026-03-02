# EcosIndicatorLatestEntity 구현 예시

```java
package com.thlee.stock.market.stockmarket.economics.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.Getter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * ECOS 경제지표 최신값 JPA Entity (변경 감지 비교용)
 */
@Entity
@Table(name = "ecos_indicator_latest")
@IdClass(EcosIndicatorLatestEntity.LatestId.class)
@Getter
public class EcosIndicatorLatestEntity {

    @Id
    @Column(name = "class_name", nullable = false, length = 100)
    private String className;

    @Id
    @Column(name = "keystat_name", nullable = false, length = 200)
    private String keystatName;

    @Column(name = "cycle", length = 20)
    private String cycle;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected EcosIndicatorLatestEntity() {
    }

    public EcosIndicatorLatestEntity(String className,
                                      String keystatName,
                                      String cycle,
                                      LocalDateTime updatedAt) {
        this.className = className;
        this.keystatName = keystatName;
        this.cycle = cycle;
        this.updatedAt = updatedAt;
    }

    /**
     * 값 갱신 (UPSERT 시 사용)
     */
    public void updateCycle(String cycle, LocalDateTime updatedAt) {
        this.cycle = cycle;
        this.updatedAt = updatedAt;
    }

    /**
     * 복합 PK 클래스
     */
    public static class LatestId implements Serializable {
        private String className;
        private String keystatName;

        public LatestId() {
        }

        public LatestId(String className, String keystatName) {
            this.className = className;
            this.keystatName = keystatName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LatestId latestId = (LatestId) o;
            return Objects.equals(className, latestId.className)
                && Objects.equals(keystatName, latestId.keystatName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(className, keystatName);
        }
    }
}
```