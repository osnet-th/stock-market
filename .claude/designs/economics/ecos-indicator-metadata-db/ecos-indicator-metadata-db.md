# ECOS 경제지표 메타데이터 DB화 설계

## 작업 리스트

- [x] EcosIndicatorMetadata 도메인 모델 생성
- [x] EcosIndicatorMetadataEntity JPA Entity 생성
- [x] EcosIndicatorMetadataJpaRepository 생성
- [x] EcosIndicatorMetadataRepository 도메인 포트 + 구현체 생성
- [x] EcosIndicatorMetadataMapper (Entity <-> Domain) 생성
- [x] EcosIndicatorMetadataService 애플리케이션 서비스 생성
- [x] EcosIndicatorMetadataInitializer 초기 데이터 시딩 (yml → DB)
- [x] EcosIndicatorController 메타데이터 조회 로직 변경
- [x] IndicatorResponse.from() 시그니처 변경
- [x] EcosIndicatorMetadataProperties 및 yml import 제거

## 배경

`ecos-indicator-metadata.yml`의 경제지표 메타데이터(description, positiveDirection, keyIndicator)는 API 키 같은 설정값이 아니라 **도메인 데이터**. DB화하여 런타임 관리 가능하게 전환.

## 핵심 결정

- **PK**: `className + keystatName` 복합키 (`@IdClass`) — 기존 `EcosIndicatorLatestEntity`와 동일 패턴
- **초기 데이터 시딩**: `ApplicationRunner`로 앱 시작 시 DB가 비어있으면 기존 yml 값을 INSERT
- **PositiveDirection enum**: 기존 `EcosIndicatorMetadataProperties.PositiveDirection`을 도메인 모델로 이동
- **yml 파일 유지**: 시딩 소스로만 사용, `@ConfigurationProperties` 바인딩은 제거
- **캐싱**: 메타데이터는 자주 변하지 않으므로 서비스 레벨에서 `Map` 캐싱 (Caffeine 불필요, 앱 시작 시 로딩 + 변경 시 갱신)

## 구현

### 도메인 모델

위치: `economics/domain/model/EcosIndicatorMetadata.java`

[예시 코드](./examples/domain-model-example.md)

### Entity + Repository

위치: `economics/infrastructure/persistence/`
- `EcosIndicatorMetadataEntity.java`
- `EcosIndicatorMetadataJpaRepository.java`
- `EcosIndicatorMetadataRepositoryImpl.java`
- `mapper/EcosIndicatorMetadataMapper.java`

도메인 포트: `economics/domain/repository/EcosIndicatorMetadataRepository.java`

[예시 코드](./examples/infrastructure-persistence-example.md)

### 애플리케이션 서비스

위치: `economics/application/EcosIndicatorMetadataService.java`

[예시 코드](./examples/application-service-example.md)

### 초기 데이터 시딩

위치: `economics/infrastructure/korea/ecos/config/EcosIndicatorMetadataInitializer.java`

- `ApplicationRunner` 구현
- DB 비어있으면 yml 파일을 `@ConfigurationProperties`가 아닌 `YamlPropertiesFactoryBean`으로 직접 파싱하여 INSERT
- 이미 데이터가 있으면 skip

[예시 코드](./examples/infrastructure-initializer-example.md)

### Controller 변경

위치: `economics/presentation/EcosIndicatorController.java`

- `EcosIndicatorMetadataProperties` 의존성 → `EcosIndicatorMetadataService`로 교체
- `metadataProperties.toMap()` → `metadataService.getMetadataMap()`

[예시 코드](./examples/presentation-example.md)

### 제거 대상

- `EcosIndicatorMetadataProperties.java` 삭제
- `application.yml`에서 `ecos-indicator-metadata.yml` import 제거
- `ecos-indicator-metadata.yml` 파일은 시딩 소스로 유지 (import만 제거)

## 주의사항

- `IndicatorResponse.from()`이 기존에 `EcosIndicatorMetadataProperties.IndicatorMeta`를 받았으므로 도메인 모델로 타입 변경 필요
- 시딩은 **DB가 비어있을 때만** 실행 (기존 데이터 덮어쓰기 방지)
- `PositiveDirection` enum은 `EcosIndicatorMetadata` 내부에 정의하되, Entity에서는 `@Enumerated(EnumType.STRING)` 사용