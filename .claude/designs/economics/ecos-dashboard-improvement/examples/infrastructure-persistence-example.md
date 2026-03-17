# 인프라 영속성 예시

## EcosIndicatorLatestEntity (변경)

```java
// 추가 필드
@Column(name = "data_value", length = 50)
private String dataValue;

@Column(name = "previous_data_value", length = 50)
private String previousDataValue;

// 생성자 확장
public EcosIndicatorLatestEntity(String className,
                                  String keystatName,
                                  String dataValue,
                                  String previousDataValue,
                                  String cycle,
                                  LocalDateTime updatedAt) {
    this.className = className;
    this.keystatName = keystatName;
    this.dataValue = dataValue;
    this.previousDataValue = previousDataValue;
    this.cycle = cycle;
    this.updatedAt = updatedAt;
}

/**
 * 값 갱신 — cycle 변경 시 이전값 보존
 * @param newDataValue 새 데이터 값
 * @param newCycle 새 주기
 * @param cycleChanged cycle이 변경되었는지 여부
 */
public void update(String newDataValue, String newCycle, boolean cycleChanged, LocalDateTime updatedAt) {
    if (cycleChanged) {
        this.previousDataValue = this.dataValue;  // 현재값을 이전값으로
    }
    this.dataValue = newDataValue;
    this.cycle = newCycle;
    this.updatedAt = updatedAt;
}
```

## EcosIndicatorLatestMapper (변경)

```java
// toDomain 변경
public static EcosIndicatorLatest toDomain(EcosIndicatorLatestEntity entity) {
    return new EcosIndicatorLatest(
        entity.getClassName(),
        entity.getKeystatName(),
        entity.getDataValue(),
        entity.getPreviousDataValue(),
        entity.getCycle(),
        entity.getUpdatedAt()
    );
}

// toEntity 변경
public static EcosIndicatorLatestEntity toEntity(EcosIndicatorLatest domain) {
    return new EcosIndicatorLatestEntity(
        domain.getClassName(),
        domain.getKeystatName(),
        domain.getDataValue(),
        domain.getPreviousDataValue(),
        domain.getCycle(),
        domain.getUpdatedAt()
    );
}
```