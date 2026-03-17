# Infrastructure Persistence 예시

## EcosIndicatorMetadataEntity

```java
package com.thlee.stock.market.stockmarket.economics.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.economics.domain.model.EcosIndicatorMetadata;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "positive_direction", nullable = false, length = 20)
    private EcosIndicatorMetadata.PositiveDirection positiveDirection;

    @Column(name = "key_indicator", nullable = false)
    private boolean keyIndicator;

    protected EcosIndicatorMetadataEntity() {
    }

    public EcosIndicatorMetadataEntity(String className,
                                        String keystatName,
                                        String description,
                                        EcosIndicatorMetadata.PositiveDirection positiveDirection,
                                        boolean keyIndicator) {
        this.className = className;
        this.keystatName = keystatName;
        this.description = description;
        this.positiveDirection = positiveDirection;
        this.keyIndicator = keyIndicator;
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
```

## EcosIndicatorMetadataJpaRepository

```java
package com.thlee.stock.market.stockmarket.economics.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EcosIndicatorMetadataJpaRepository
    extends JpaRepository<EcosIndicatorMetadataEntity, EcosIndicatorMetadataEntity.MetadataId> {
}
```

## EcosIndicatorMetadataRepository (도메인 포트)

```java
package com.thlee.stock.market.stockmarket.economics.domain.repository;

import com.thlee.stock.market.stockmarket.economics.domain.model.EcosIndicatorMetadata;

import java.util.List;

public interface EcosIndicatorMetadataRepository {

    List<EcosIndicatorMetadata> findAll();

    long count();

    void saveAll(List<EcosIndicatorMetadata> metadataList);
}
```

## EcosIndicatorMetadataRepositoryImpl

```java
package com.thlee.stock.market.stockmarket.economics.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.economics.domain.model.EcosIndicatorMetadata;
import com.thlee.stock.market.stockmarket.economics.domain.repository.EcosIndicatorMetadataRepository;
import com.thlee.stock.market.stockmarket.economics.infrastructure.persistence.mapper.EcosIndicatorMetadataMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class EcosIndicatorMetadataRepositoryImpl implements EcosIndicatorMetadataRepository {

    private final EcosIndicatorMetadataJpaRepository jpaRepository;
    private final EcosIndicatorMetadataMapper mapper;

    @Override
    public List<EcosIndicatorMetadata> findAll() {
        return jpaRepository.findAll().stream()
            .map(mapper::toDomain)
            .toList();
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }

    @Override
    public void saveAll(List<EcosIndicatorMetadata> metadataList) {
        List<EcosIndicatorMetadataEntity> entities = metadataList.stream()
            .map(mapper::toEntity)
            .toList();
        jpaRepository.saveAll(entities);
    }
}
```

## EcosIndicatorMetadataMapper

```java
package com.thlee.stock.market.stockmarket.economics.infrastructure.persistence.mapper;

import com.thlee.stock.market.stockmarket.economics.domain.model.EcosIndicatorMetadata;
import com.thlee.stock.market.stockmarket.economics.infrastructure.persistence.EcosIndicatorMetadataEntity;
import org.springframework.stereotype.Component;

@Component
public class EcosIndicatorMetadataMapper {

    public EcosIndicatorMetadata toDomain(EcosIndicatorMetadataEntity entity) {
        return new EcosIndicatorMetadata(
            entity.getClassName(),
            entity.getKeystatName(),
            entity.getDescription(),
            entity.getPositiveDirection(),
            entity.isKeyIndicator()
        );
    }

    public EcosIndicatorMetadataEntity toEntity(EcosIndicatorMetadata domain) {
        return new EcosIndicatorMetadataEntity(
            domain.getClassName(),
            domain.getKeystatName(),
            domain.getDescription(),
            domain.getPositiveDirection(),
            domain.isKeyIndicator()
        );
    }
}
```