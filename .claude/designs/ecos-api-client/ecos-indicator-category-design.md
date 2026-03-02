# ECOS 경제지표 카테고리 Enum 설계

- 작성일: 2026-02-20

---

## 작업 리스트

- [x] `EcosIndicatorCategory` Enum 구현
- [x] `KeyStatIndicator` 도메인 모델 구현
- [x] `EcosKeyStatResult` 도메인 모델 구현
- [x] `EcosIndicatorPort` 인터페이스 구현 (domain 계층)
- [x] `EcosIndicatorAdapter` 구현 (infrastructure 계층, Port 구현체)
- [x] Caffeine 의존성 추가 (build.gradle)
- [x] `EcosCacheConfig` 구현
- [x] `EcosIndicatorService` 구현 (application 계층)
- [x] application.yml 설정 추가
- [x] .env / .env.example 설정 추가
- [x] `IndicatorResponse` 응답 DTO 구현 (presentation 계층)
- [x] `CategoryResponse` 응답 DTO 구현 (presentation 계층)
- [x] `EcosIndicatorController` 구현 (presentation 계층)

---

## 배경

ECOS 100대 경제지표 API는 27개 CLASS_NAME을 반환하지만, 클라이언트에게 의미 있는 단위로 묶어 15개 카테고리로 제공

---

## 핵심 결정

- Enum 필드: `label` (API 응답 표시용), `classNames` (매핑할 CLASS_NAME 목록)
- `classNames`를 `Set<String>`으로 관리하여 CLASS_NAME 기반 역방향 조회 지원
- 위치: `economics/domain/model/EcosIndicatorCategory.java`

---

## 카테고리 매핑

| Enum 상수 | label | CLASS_NAME 매핑 |
|---|---|---|
| INTEREST_RATE | 금리 | 시장금리, 여수신금리 |
| MONEY_FINANCE | 통화/금융 | 통화량, 예금/대출금 |
| STOCK_BOND | 주식/채권 | 주식, 채권 |
| EXCHANGE_RATE | 환율 | 환율 |
| GROWTH_INCOME | 성장/소득 | 성장률, 소득, GDP대비 비율 |
| PRODUCTION | 생산 | 생산 |
| CONSUMPTION_INVESTMENT | 소비/투자 | 소비, 투자 |
| PRICE | 물가 | 소비자/생산자 물가, 수출입 물가 |
| EMPLOYMENT_LABOR | 고용/노동 | 고용, 노동 |
| SENTIMENT | 경기심리 | 경기순환지표, 심리지표 |
| EXTERNAL_ECONOMY | 대외경제 | 국제수지, 통관수출입, 대외채권/채무 |
| CORPORATE_HOUSEHOLD | 기업/가계 | 기업경영지표, 가계, 소득분배지표 |
| REAL_ESTATE | 부동산 | 부동산 가격 |
| POPULATION | 인구 | 인구 |
| COMMODITY | 원자재 | 국제원자재가격 |

---

## 캐싱 전략

- **방식**: Caffeine + Spring Cache (`@Cacheable`)
- **TTL**: 24시간 (통계 데이터 특성상 일 단위 갱신이면 충분)
- **캐시 키**: 카테고리별 캐싱 (EcosIndicatorCategory)
- **최대 항목**: 15 (카테고리 수)
- **적용 위치**: Application 계층 Service에서 `@Cacheable` 적용

### 설정

- `spring.cache.type: caffeine` 활성화
- `EcosCacheConfig`에서 캐시 매니저 Bean 등록
- 위치: `economics/infrastructure/korea/ecos/config/EcosCacheConfig.java`

[예시 코드](examples/EcosCacheConfig-example.md)

---

## 구현

### EcosIndicatorCategory

위치: `economics/domain/model/EcosIndicatorCategory.java`

[예시 코드](examples/EcosIndicatorCategory-example.md)

---

## Presentation 계층 (Controller API)

### 엔드포인트

| Method | URI | 설명 | 파라미터 |
|---|---|---|---|
| GET | `/api/economics/indicators` | 카테고리별 경제지표 조회 | `category` (required, EcosIndicatorCategory) |
| GET | `/api/economics/indicators/categories` | 전체 카테고리 목록 조회 | 없음 |

### 핵심 결정

- 응답 DTO를 presentation 계층에 배치 (도메인 모델 직접 노출 방지)
- `IndicatorResponse`: `KeyStatIndicator` → 클라이언트 응답용 record
- `CategoryResponse`: `EcosIndicatorCategory` → 카테고리 label + name 응답용 record
- 잘못된 category 값은 Spring의 `TypeMismatchException`으로 400 응답 처리

### EcosIndicatorController

위치: `economics/presentation/EcosIndicatorController.java`

[예시 코드](examples/EcosIndicatorController-example.md)

### IndicatorResponse

위치: `economics/presentation/dto/IndicatorResponse.java`

[예시 코드](examples/IndicatorResponse-example.md)

### CategoryResponse

위치: `economics/presentation/dto/CategoryResponse.java`

[예시 코드](examples/CategoryResponse-example.md)