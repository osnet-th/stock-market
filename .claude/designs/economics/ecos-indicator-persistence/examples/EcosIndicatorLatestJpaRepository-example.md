# EcosIndicatorLatestJpaRepository 구현 예시

```java
package com.thlee.stock.market.stockmarket.economics.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EcosIndicatorLatestJpaRepository
    extends JpaRepository<EcosIndicatorLatestEntity, EcosIndicatorLatestEntity.LatestId> {
}
```