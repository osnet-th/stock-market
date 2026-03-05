# Joined Table 상속 전략 엔티티 예시

## 부모 엔티티 (공통)

```java
@Entity
@Table(name = "portfolio_item")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "asset_type", discriminatorType = DiscriminatorType.STRING)
@Getter
public abstract class PortfolioItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "item_name", nullable = false, length = 100)
    private String itemName;

    @Column(name = "asset_type", insertable = false, updatable = false, length = 20)
    private String assetType;

    @Column(name = "invested_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal investedAmount;

    @Column(name = "news_enabled", nullable = false)
    private boolean newsEnabled;

    @Column(name = "region", nullable = false, length = 20)
    private String region;

    @Column(name = "memo", length = 500)
    private String memo;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
```

## 자식 엔티티 - 주식

```java
@Entity
@Table(name = "stock_detail")
@DiscriminatorValue("STOCK")
@Getter
public class StockItemEntity extends PortfolioItemEntity {

    @Column(name = "sub_type", length = 20)
    private String subType; // ETF, INDIVIDUAL

    @Column(name = "ticker", length = 20)
    private String ticker;

    @Column(name = "exchange", length = 20)
    private String exchange;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "avg_buy_price", precision = 18, scale = 2)
    private BigDecimal avgBuyPrice;

    @Column(name = "dividend_yield", precision = 5, scale = 2)
    private BigDecimal dividendYield;
}
```

## 자식 엔티티 - 채권

```java
@Entity
@Table(name = "bond_detail")
@DiscriminatorValue("BOND")
@Getter
public class BondItemEntity extends PortfolioItemEntity {

    @Column(name = "sub_type", length = 20)
    private String subType; // GOVERNMENT, CORPORATE

    @Column(name = "maturity_date")
    private LocalDate maturityDate;

    @Column(name = "coupon_rate", precision = 5, scale = 2)
    private BigDecimal couponRate;

    @Column(name = "credit_rating", length = 10)
    private String creditRating;
}
```

## 자식 엔티티 - 부동산

```java
@Entity
@Table(name = "real_estate_detail")
@DiscriminatorValue("REAL_ESTATE")
@Getter
public class RealEstateItemEntity extends PortfolioItemEntity {

    @Column(name = "sub_type", length = 20)
    private String subType; // APARTMENT, OFFICETEL, LAND, COMMERCIAL

    @Column(name = "address", length = 200)
    private String address;

    @Column(name = "area", precision = 10, scale = 2)
    private BigDecimal area; // m2
}
```

## 생성되는 테이블 구조

```sql
-- 부모 테이블 (공통 필드)
CREATE TABLE portfolio_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    asset_type VARCHAR(20) NOT NULL,  -- discriminator
    user_id BIGINT NOT NULL,
    item_name VARCHAR(100) NOT NULL,
    invested_amount DECIMAL(18,2) NOT NULL,
    news_enabled BOOLEAN NOT NULL DEFAULT false,
    region VARCHAR(20) NOT NULL,
    memo VARCHAR(500),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_portfolio_item_user_id (user_id)
);

-- 자식 테이블 (주식 고유 필드만)
CREATE TABLE stock_detail (
    id BIGINT PRIMARY KEY,  -- portfolio_item.id와 동일
    sub_type VARCHAR(20),
    ticker VARCHAR(20),
    exchange VARCHAR(20),
    quantity INT,
    avg_buy_price DECIMAL(18,2),
    dividend_yield DECIMAL(5,2)
);

-- 자식 테이블 (채권 고유 필드만)
CREATE TABLE bond_detail (
    id BIGINT PRIMARY KEY,
    sub_type VARCHAR(20),
    maturity_date DATE,
    coupon_rate DECIMAL(5,2),
    credit_rating VARCHAR(10)
);

-- 자식 테이블 (부동산 고유 필드만)
CREATE TABLE real_estate_detail (
    id BIGINT PRIMARY KEY,
    sub_type VARCHAR(20),
    address VARCHAR(200),
    area DECIMAL(10,2)
);
```

## 조회 예시

```sql
-- 비중 계산: 부모 테이블만으로 가능 (JOIN 없음)
SELECT asset_type, SUM(invested_amount)
FROM portfolio_item
WHERE user_id = ?
GROUP BY asset_type;

-- 주식 상세 조회: JOIN 발생
SELECT p.*, s.*
FROM portfolio_item p
JOIN stock_detail s ON p.id = s.id
WHERE p.user_id = ? AND p.asset_type = 'STOCK';

-- 전체 목록 (상세 포함): JPA가 자동으로 타입별 JOIN 처리
List<PortfolioItemEntity> items = repository.findByUserId(userId);
// → StockItemEntity, BondItemEntity 등으로 자동 캐스팅
```
