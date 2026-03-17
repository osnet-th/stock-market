# ECOS 경제지표 대시보드 개선 설계

## 작업 리스트

- [x] EcosIndicatorLatest 도메인 모델 + Entity에 dataValue, previousDataValue 추가
- [x] KeyStatIndicator record에 previousDataValue 필드 추가 + API Adapter 수정
- [x] EcosIndicatorSaveService 배치 로직 변경 (이전값 보존 + 캐시 pre-merge)
- [x] ecos-indicator-metadata.yml + EcosIndicatorMetadataProperties 생성
- [x] EcosIndicatorService 메타데이터 머지 + IndicatorResponse DTO 확장
- [x] 프론트엔드 레이스컨디션 수정 (generation counter)
- [x] 카드 대시보드 UI + 테이블 변화 컬럼 + 툴팁 구현

## 배경

플랫 테이블로만 표시되는 ECOS 경제지표 화면을 카드 대시보드 + 상세 테이블 하이브리드로 개선.
상세 플랜: [docs/plans/2026-03-16-001-feat-ecos-indicator-dashboard-improvement-plan.md](/docs/plans/2026-03-16-001-feat-ecos-indicator-dashboard-improvement-plan.md)

## 핵심 결정

- **도메인 모델**: KeyStatIndicator에 `previousDataValue` 직접 추가 (별도 모델 불필요, 101개 규모)
- **이전값 저장**: `ecos_indicator_latest` 테이블에 `data_value`, `previous_data_value` 컬럼 추가
- **캐시**: 배치 시 previousDataValue를 pre-merge하여 캐시 적재
- **메타데이터**: YAML 설정 파일 + `@ConfigurationProperties` (spring.config.import)
- **메타데이터 머지**: application 레이어 (EcosIndicatorService)에서 요청 시 머지
- **프론트엔드**: generation counter로 레이스컨디션 해결, 순수 CSS 툴팁
- **description 필드 통합**: 별도 tooltip 필드 없이 단일 description 필드를 카드/테이블 양쪽에서 사용
- **테이블 그룹 헤더 제거**: className 기준 정렬만으로 충분

## 구현

### EcosIndicatorLatest 도메인 모델

위치: `economics/domain/model/EcosIndicatorLatest.java`

변경: `dataValue`, `previousDataValue` 필드 추가, `fromKeyStatIndicator()` 시그니처 변경

[예시 코드](./examples/domain-model-example.md)

### EcosIndicatorLatestEntity

위치: `economics/infrastructure/persistence/EcosIndicatorLatestEntity.java`

변경: `data_value`, `previous_data_value` 컬럼 추가, `updateCycle()` → `update()` 확장

[예시 코드](./examples/infrastructure-persistence-example.md)

### KeyStatIndicator record

위치: `economics/domain/model/KeyStatIndicator.java`

변경: `previousDataValue` nullable 필드 추가

[예시 코드](./examples/domain-model-example.md)

### EcosIndicatorAdapter (API → Domain 변환)

위치: `economics/infrastructure/korea/ecos/EcosIndicatorAdapter.java`

변경: KeyStatIndicator 생성 시 `previousDataValue = null`

[예시 코드](./examples/infrastructure-adapter-example.md)

### EcosIndicatorSaveService (배치 로직)

위치: `economics/application/EcosIndicatorSaveService.java`

변경:
- Latest 벌크 조회 시 dataValue도 함께 가져옴
- cycle 변경 시: 현재 dataValue → previousDataValue, 새 dataValue 저장
- cycle 미변경 시: dataValue 갱신, previousDataValue 유지
- putCacheByCategory에서 previousDataValue pre-merge

[예시 코드](./examples/application-service-example.md)

### ecos-indicator-metadata.yml + Properties

위치:
- `src/main/resources/ecos-indicator-metadata.yml`
- `economics/infrastructure/korea/ecos/config/EcosIndicatorMetadataProperties.java`

[예시 코드](./examples/infrastructure-config-example.md)

### EcosIndicatorService (메타데이터 머지)

위치: `economics/application/EcosIndicatorService.java`

변경: 캐시에서 조회한 지표에 메타데이터(description, positiveDirection, keyIndicator) 머지

[예시 코드](./examples/application-service-example.md)

### IndicatorResponse DTO 확장

위치: `economics/presentation/dto/IndicatorResponse.java`

변경: `previousDataValue`, `description`, `positiveDirection`, `keyIndicator` 필드 추가

[예시 코드](./examples/presentation-example.md)

### 프론트엔드

- `static/js/components/ecos.js` — generation counter, 카드 필터링, 정렬 메서드
- `static/js/app.js` — ecos state에 `_requestGeneration` 추가
- `static/index.html` — 카드 대시보드 + 테이블 변화 컬럼 + 툴팁

[예시 코드](./examples/frontend-example.md)

## 주의사항

- NULL previousDataValue는 "이전 관측값 없음"을 의미, 절대 0으로 취급하지 않음
- 히스토리 INSERT + latest UPSERT는 동일 @Transactional 내에서 수행 (기존 트랜잭션 유지)
- API fallback (캐시 miss) 시 previousDataValue는 null → 프론트 graceful fallback
- 첫 배포 후 data_value/previous_data_value 모두 NULL → 다음 배치에서 채워짐
- YAML에 `optional:` 접두사 사용 → 파일 부재 시에도 기동 가능