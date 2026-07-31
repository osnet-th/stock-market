# 포트폴리오 목표 자산 배분 비율 Work 기록 (#102)

gate: docs/gates/2026-07-30-asset-allocation-target-gates.md
plan: docs/plans/2026-07-30-001-feat-asset-allocation-target-plan.md

## 구현 요약

plan의 Phase 1~5 전체 구현 완료. 승인 게이트 대상 6건은 plan 승인(태형님 "진행해", 2026-07-30)으로 일괄 승인됨.

## 변경 파일

### Phase 1 — 목표 설정 백엔드
- `domain/model/enums/AllocationBucket.java` (신규) — SAFE/INVEST 버킷
- `domain/model/AssetClassification.java` (신규) — 고정 매핑 (CASH·BOND=안전, CRYPTO=제외)
- `domain/model/AllocationTarget.java` (신규) — 목표 설정 도메인 + 검증(합 100, 밴드 0~20, CRYPTO/안전유형 금지)
- `domain/repository/AllocationTargetRepository.java` (신규, 포트)
- `infrastructure/persistence/AllocationTargetEntity.java`·`AllocationTargetAssetEntity.java`·JpaRepository 2종·`AllocationTargetRepositoryImpl.java` (신규) — 자산군 행 전체 교체 방식 업서트
- `application/AllocationTargetService.java`·`application/dto/AllocationTargetResponse.java` (신규)
- `presentation/dto/AllocationTargetSaveRequest.java` (신규), `PortfolioController` — GET(미설정 204)/PUT `/api/portfolio/allocation/target`
- `db/migration/allocation_target_2026_07_30.sql` (백업 SQL)

### Phase 2 — 금 시세 연동 + GOLD 평가
- `domain/model/GoldDetail.java` (신규) — quantityGrams(>0)
- `domain/model/PortfolioItem.java` — goldDetail 필드·재구성 생성자 확장·`updateGoldDetail`(null=제거)
- `infrastructure/persistence/GoldItemEntity.java` — `quantity_grams` NUMERIC(12,3) nullable
- `infrastructure/persistence/mapper/PortfolioItemMapper.java` — GOLD 양방향 매핑
- `application/dto/GoldDetailResponse.java` (신규), `PortfolioItemResponse` — goldDetail 노출
- `PortfolioService.addGeneralItem/updateGeneralItem` — quantityGrams 파라미터 (GOLD 외 입력 시 400)
- `presentation/dto/GeneralItemAddRequest/UpdateRequest` — quantityGrams 필드
- `domain/service/GoldPriceProvider.java` (신규 포트), `infrastructure/goldprice/` — `KrxGoldPriceAdapter`(TTL 60분 캐시·실패 5분 backoff·stale 재사용·실패 시 empty), `GoldPriceProperties`, `dto/GoldPriceApiResponse`
- `PortfolioEvaluationService` — GOLD+중량 항목 존재 시 금 시세 1회 조회, evaluated = 중량 × 1g가 (실패 시 원금)
- `application.yml` — `gold.api.*` (DATAGOKR_SERVICE_KEY 재사용)
- `db/migration/gold_detail_quantity_grams_2026_07_30.sql` (백업 SQL)

### Phase 3 — 배분 현황 API
- `application/AllocationStatusService.java`·`application/dto/AllocationStatusResponse.java` (신규) — ACTIVE만(기존 repo 필터)·CRYPTO 제외 집계, 버킷(전체 대비)·투자 자산군(투자 총액 대비) 편차 금액/%p·밴드초과, 미설정 시 현재 비율만
- `PortfolioController` — GET `/api/portfolio/allocation/status`

### Phase 4 — 포트폴리오 페이지 UI
- `js/api.js` — getAllocationStatus/getAllocationTarget/saveAllocationTarget
- `js/components/portfolio.js` — allocationStatus 상태·loadPortfolio 병렬 조회(실패 무해화)·목표 설정 모달 상태/검증/저장·GOLD 중량 add/edit 반영·포맷 헬퍼
- `partials/portfolio.html` — "목표 자산 배분" 섹션(버킷 막대+목표 마커+편차+밴드 초과 배지+암호화폐 제외 안내+미설정 유도) + 목표 설정 모달(안전 비율 입력→투자 자동, 밴드, 자산군 합 100 실시간 검증)
- `partials/portfolio-add.html`·`portfolio-edit.html` — GOLD 보유 중량(g) 입력 (선택)

### Phase 5 — 대시보드
- `js/components/home.js` — allocationStatus 병렬 조회·버킷 헬퍼·경고 판정
- `partials/home.html` — 포트폴리오 카드: 목표 설정+평가액>0이면 총 평가액·안전/투자 현재/목표·"배분 밴드 초과" 배지, 아니면 기존 개수 표시 유지

## work 단계 자체 검증 (스모크)

- `./gradlew compileJava` — PASS (exit 0, 기존 realestate deprecation 경고만)
- `node --check` api.js / portfolio.js / home.js — PASS

## 알려진 제약 (validation 단계 확인 항목)

- KRX 금시세 API 실호출 미검증 — 세션 환경에 `DATAGOKR_SERVICE_KEY` 없음. 운영 키가 "금융위원회_일반상품시세정보"에 활용신청되어 있어야 함 (미구독 시 원금 평가 fallback으로 동작 자체는 유지)
- 포트폴리오 도넛/자산 비중 막대(클라이언트 계산)는 금 시세 미반영 (주식만 실시간) — 배분 섹션·대시보드는 서버 평가값 사용으로 금 시세 반영됨. 후속 개선 후보
- 브라우저 UI 동작 확인(목표 미설정/설정/밴드 초과/CRYPTO 보유/총액 0) 미수행 — validation 단계에서 확인
