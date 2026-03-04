# ECOS 경제지표 초기 시딩 로직 설계

## 작업 리스트

- [x] `EcosIndicatorRepository`에 `existsAny()` 메서드 추가
- [x] `EcosIndicatorJpaRepository`에 `existsFirstBy()` 메서드 추가
- [x] `EcosIndicatorRepositoryImpl`에 `existsAny()` 구현
- [x] `EcosIndicatorSaveService.fetchAndSave()`에 초기 시딩 분기 추가

## 배경

현재 `fetchAndSave()`는 `ecos_indicator_latest` 테이블의 `cycle` 값과 API 응답을 비교하여 변경분만 히스토리에 저장. 하지만 히스토리 테이블(`ecos_indicator`)이 완전히 비어있는 초기 상태에서도 동일 로직이 적용되어, latest에 데이터가 없으면 모든 지표가 "신규"로 판정되어 저장은 되지만, latest에 이미 데이터가 있고 cycle이 동일하면 히스토리가 하나도 쌓이지 않는 상태가 발생할 수 있음.

**초기 시딩 요구사항**: 히스토리가 비어있으면 현재 API 조회 값을 전체 저장 + latest 갱신 후 종료.

## 핵심 결정

- **판단 기준**: `ecos_indicator` (히스토리) 테이블에 데이터 존재 여부
- **존재 확인 방법**: `count()` 대신 `EXISTS` 쿼리 사용 (성능 최적화, 전체 카운트 불필요)
- **초기 시딩 시 동작**: 유효 지표 전체를 히스토리에 INSERT + latest UPSERT + 캐시 적재 → 종료
- **기존 로직 영향 없음**: 히스토리가 있으면 기존 cycle 비교 로직 그대로 수행

## 구현

### EcosIndicatorRepository (도메인)

위치: `economics/domain/repository/EcosIndicatorRepository.java`

- `boolean existsAny()` 추가 — 히스토리 데이터 존재 여부 확인

### EcosIndicatorJpaRepository (인프라)

위치: `economics/infrastructure/persistence/EcosIndicatorJpaRepository.java`

- `boolean existsFirstBy()` 추가 — Spring Data JPA 자동 생성 EXISTS 쿼리

### EcosIndicatorRepositoryImpl (인프라)

위치: `economics/infrastructure/persistence/EcosIndicatorRepositoryImpl.java`

- `existsAny()` → `jpaRepository.existsFirstBy()` 위임

### EcosIndicatorSaveService.fetchAndSave() 수정

위치: `economics/application/EcosIndicatorSaveService.java`

변경 흐름:
```
1. API 조회
2. 캐시 적재
3. 히스토리 존재 확인 (existsAny)
4-A. 히스토리 없음 (초기 시딩):
    → 유효 지표 전체 히스토리 INSERT
    → latest 전체 UPSERT
    → return
4-B. 히스토리 있음 (기존 로직):
    → latest 조회 → cycle 비교 → 변경분만 히스토리 INSERT
    → latest 전체 UPSERT
    → return
```

[예시 코드](examples/fetchAndSave-modified-example.md)

## 주의사항

- `existsFirstBy()`는 Spring Data JPA가 `SELECT EXISTS(SELECT 1 FROM ...)` 최적화 쿼리 생성
- 초기 시딩은 앱 최초 실행 또는 히스토리 테이블 비운 후 1회만 발생
- 초기 시딩 시에도 `cycle`이 null인 지표는 `validIndicators()` 필터링으로 제외됨