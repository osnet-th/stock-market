# 포트폴리오 화면 대시보드형 재설계 게이트 로그

## 원칙
- 각 단계 시작 전 태형님에게 작업 내용을 제시한다.
- 다음 단계로 넘어갈지에 대한 판단은 태형님에게 넘긴다.
- 단계별 산출물은 이 게이트 로그를 `gate: docs/gates/2026-08-10-portfolio-dashboard-redesign-gates.md`로 참조한다.

## Stage Decisions
- start: approved (2026-08-10, 태형님 "시작해")
- brainstorm: approved (2026-08-10, 태형님 "진행해" — Open Questions 6건 회신 반영본 확정)
- issue: approved (2026-08-10, 태형님 "진행해" — GitHub Issue #110 등록, worktree feat/issue-110-portfolio-dashboard-redesign 생성)
- plan: approved (2026-08-10, 태형님 "진행해" — Phase 3·4 스키마/API 제안 포함 승인, Phase 1부터 착수)
- work: in progress (Phase 1 진행 중)
- review: pending
- validation: pending
- commit: pending
- push: pending

## Stage Log
- start: 2026-08-09, 태형님이 Claude 디자인 목업(`~/Downloads/포트폴리오 대시보드 (단일파일).html`)을 제시하며 "현재 포트폴리오 화면을 이걸로 변경하면 어떤 기능이 누락되는지" 확인 요청 → 번들 해제 후 기능 대조, 누락 항목 정리
- 2026-08-10: 태형님이 지적사항 반영본 v3(`~/Downloads/포트폴리오 대시보드 v3 (단일파일).html`) 제시 → 재대조, 반영/미반영 구분
- 2026-08-10: 태형님 "csv 내보내기 기능은 없어도 될거같아" → CSV 내보내기 범위 제외 확정
- 2026-08-10: 태형님 "저기에 동일하게 수정 버튼을 그대로 추가하는식으로 가는건 어때?" → 자산 수정 버튼 추가 + 자산군별 액션 노출 규칙 복원 방향 제안, 태형님 "시작해"로 brainstorm 착수 승인
- brainstorm: 문서 작성 완료 (2026-08-10, docs/brainstorms/2026-08-10-portfolio-dashboard-redesign-brainstorm.md)
- 2026-08-10: Open Questions 6건 태형님 회신 — 1) 뉴스 키워드 메뉴 완전 이관(등록만 잔류) 2) 연금 자산군 추가 3) 국내/해외는 표시 레벨만 분리 4) 신규 백엔드 4종 이번 범위 포함 5) 매도 이력 전체 기간 6) 재무상세 현재 필터 유지. brainstorm 문서에 반영 완료
- issue: 완료 (2026-08-10, docs/issues/2026-08-10-portfolio-dashboard-redesign-issue.md — GitHub Issue #110 https://github.com/osnet-th/stock-market/issues/110, label: enhancement)
  - worktree 생성: `scripts/create-worktree.sh --issue 110 feat/issue-110-portfolio-dashboard-redesign` (base main, .env 복사됨)
  - brainstorm·gate 문서를 worktree로 이동해 브랜치에서 함께 커밋되도록 정리
- plan: 문서 작성 완료 (2026-08-10, docs/plans/2026-08-10-001-feat-portfolio-dashboard-redesign-plan.md)
  - 4 Phase 분할: ①프론트 4탭 재구성 ②뉴스 이관 ③연금 자산군 ④신규 집계 4종
  - Phase 1~2는 프론트 전용, Phase 3~4는 Entity·API 변경 → Phase 착수 전 개별 승인 필요
  - 확인 사실: `portfolio.html` 696줄 + 4탭이면 1,500줄 초과 → 탭 단위 partial 분리(app.js `partialNames` 확장)
  - 확인 사실: 해외 주식 배당 데이터 소스 부재 → 이달 배당·이자는 국내 위주 집계, `basis`·`excludedCount`로 명시
- work Phase 1: 완료 (2026-08-10, docs/works/2026-08-10-portfolio-dashboard-redesign-work.md)
  - partial 4종 신규 + `portfolio.html` 셸 축소 + `portfolio.js` 헬퍼 재편(추가 20여종·제거 8종) + `app.js`/`index.html` 등록
  - 목 하네스 실측 16항목 PASS. `node` 미설치로 `node --check` 대신 브라우저 파싱 검증으로 대체
  - 실행 중 수정: 리밸런싱 총 이동 금액이 상위 버킷과 투자자산 내부 편차를 합산해 중복 계상 → 레벨 분리 후 재검증
  - Phase 2 진입 승인 (2026-08-10, 태형님 "다음 진행해")
- work Phase 2: 완료 (2026-08-10, docs/works/2026-08-10-portfolio-dashboard-redesign-work.md)
  - 뉴스 상태 7종·메서드 9종 제거(176줄), 키워드 등록 전용 모달 추가, `등록됨` 비활성으로 중복 등록 차단
  - 실행 중 수정: `getKeywordRegion`을 `stockDetail.country` → `item.region`(서버가 실제로 쓰는 값) 기준으로 정정
  - 잔여 참조 grep 0건, 하네스 실측 7항목 PASS
  - 해외뉴스 API 래퍼 처리: **유지 확정** (2026-08-10, 태형님 "유지해" — 백엔드 엔드포인트 존치·키워드 메뉴 편입 가능성. `api.js`에 사유 주석 추가)
  - Phase 3 진입 승인 (2026-08-10, 태형님 "진행해")
- work Phase 3: 완료 (2026-08-10, docs/works/2026-08-10-portfolio-dashboard-redesign-work.md)
  - `AssetType.PENSION` 신설(9종 → 10종), `PensionSubType`·`PensionDetail`·`PensionItemEntity`·DTO 3종·마이그레이션 SQL 추가
  - `AssetClassification.SAFE_TYPES` 편입으로 배분은 자동 반영, 납입 대상·미납 판정 확장, 평가액 분리(원금 vs 평가액)
  - `compileJava`·`compileTestJava` PASS, `test --tests "*Portfolio*"` 59 tests 0 failures
  - 하네스 실측 11항목 PASS (그룹·평가손익·행 액션·추가/수정 모달·도넛 범례)
  - 실행 중 수정: 프론트 `getEvalAmount()`에도 PENSION 분기 추가(서버만 고치면 목록이 원금으로 표시됨)
  - Phase 4 진입 승인 (2026-08-10, 태형님 "진행해")
- work Phase 4: 완료 (2026-08-10, docs/works/2026-08-10-portfolio-dashboard-redesign-work.md)
  - `portfolio_snapshot` 도메인·Entity·Repository·마이그레이션, `stock_purchase_history.fx_rate` 컬럼·마이그레이션
  - 신규 API 4종: `GET /summary`, `GET /income/summary`, `POST /snapshots`, `GET /snapshots`
  - 프론트 KPI 4장 실연동 + 자산 추이 라인차트 + 스냅샷 저장
  - `compileJava`·`compileTestJava`·`test --tests "*Portfolio*"` (59 tests) PASS, 하네스 실측 9항목 PASS
  - **실행 중 발견한 버그**: `renderTrendChart` 가 `salary.js` 의 동명 메서드에 덮어써져 차트가 렌더되지 않음 → `renderPortfolioTrendChart` 로 분리, 컴포넌트 전체 메서드 중복 스캔으로 재발 확인(0건)
  - **계획과 다른 결정 (태형님 확인 필요)**: 배당 집계를 KSD 일정 대신 `dividendYield × 평가액` 기반으로 변경, 이달 금액은 월 평균 환산(`basis` 로 명시)
  - review 단계 진입 승인 (2026-08-10, 태형님 "전자로 진행해")
- review: 완료 (2026-08-10, docs/reviews/2026-08-10-portfolio-dashboard-redesign-review.md)
  - High 2건 발견·수정·재검증: H1 초기 확장 섹션 키 불일치(주식 그룹만 접힌 채 시작), H2 분석 탭 이탈 시 재무 패널 Chart 인스턴스 누수
  - Medium 4건(평가 중복 실행·매수이력 N+1·환율 단건 조회·배당 산출 방식), Low 5건 제시
  - 2026-08-10 태형님 "다 고쳐줘" → M1·M2·M3 및 L1~L4 수정 완료, 재검증 11항목 PASS
    - M1: `/income/summary` 제거 후 `/summary` 에 income 중첩 → 평가 2회 → 1회
    - M2: `findByPortfolioItemIdIn` 배치 조회 도입 → 최대 2N 쿼리 → 1회
    - M3: 통화별 대표 종목 1회 조회로 환율 재사용 → 해외 종목 N개 → 통화 종류 수
    - L1 투자원금 상시 노출 / L2 매수 환율 초기화 허용 / L3 연금 키워드 제외 / L4 그룹 메모이제이션
    - L5(연금의 배당·이자 제외)는 이중 계상 방지 목적의 의도된 동작으로 유지
  - M4(배당 산출 방식)는 외부 API 캐싱·폴백 설계가 필요해 후속 이슈로 분리 (2026-08-10, 태형님 "지금 git issue 로 등록해줘" → GitHub Issue #113 등록)
  - 2026-08-10 태형님 "리뷰에서 수정이 필요한 부분은 없어?" → **수정 코드 자체를 2차 리뷰**해 새 결함 2건 발견·수정
    - R3: L2 수정 탓에 매수 이력 수정 시 기록된 환율이 지워짐 → 수정 폼에 환율 입력란 추가(해외 주식 한정)
    - R4: M3 수정 탓에 대표 종목 조회 실패 시 해당 통화 전체가 환차손익에서 누락 → 통화별 종목 순회 폴백
    - 추가: `loadPortfolio()` catch 경로의 그룹 캐시 무효화 누락 보완
  - validation 단계 진입 승인 (2026-08-11, 태형님 "진행하고 운영에 필수로 적용해야 하는 것만 알려줘")
- validation: 완료 (2026-08-11, docs/validations/2026-08-10-portfolio-dashboard-redesign-validation.md)
  - 실제 앱 기동(dev, :8082) + 로컬 도커 Postgres 로 검증. 스키마 3건 자동 생성 확인, 신규 API 4종 실호출 확인
  - **V1 배포 블로커 발견·조치**: `portfolio_item_asset_type_check` CHECK 제약이 `PENSION` 을 막아 연금 등록이 409 로 실패.
    `ddl-auto: update` 는 기존 CHECK 제약을 갱신하지 않는다 → `pension_detail_2026_08_10.sql` 에 CHECK 재생성 ALTER 를 추가하고 "운영 적용 필수" 로 표시
  - 연금 평가액 분리·안전자산 배분 편입·수정·납입 모두 실서버에서 정상
  - 테스트 데이터 정리 완료 (연금 항목·스냅샷 삭제, 앱 종료)
  - commit 단계 진입 대기

## 운영 배포 시 필수 적용
- **`pension_detail_2026_08_10.sql` 의 CHECK 제약 ALTER 2줄** — 이것만 수동 필수. 없으면 연금 등록 409 실패
- 나머지 3건(테이블 2개·컬럼 1개)은 `ddl-auto: update` 가 자동 처리 (실측 확인)
- 권장 순서: SQL 적용 → 앱 배포

## 잔여 승인/확인 항목
- ~~배당 집계 방식 변경~~ → 후속 이슈 #113 로 분리 완료 (https://github.com/osnet-th/stock-market/issues/113). 이번 범위는 `dividendYield` 기반 + `basis` 명시 유지
- ~~마이그레이션 실 DB 적용~~ → 로컬 검증 완료, 운영 필수 항목 1건으로 정리
- 스냅샷 자동 적재(스케줄러) 도입 여부 — 현재 수동 저장만
- 미검증: 운영 DB 반영, 환차손익 실값(현 데이터 전부 fx_rate 미기록), 자산 추이 라인차트 실렌더(스냅샷 1건), 매도/목표배분/분석 탭 실서버 화면, 모바일 반응형

## Approval Gate 항목
- **연금(IRP·연금저축) 자산군 신설** — `AssetType` enum에 `PENSION` 추가(9종 → 10종) + 상세 엔티티·마이그레이션. Entity 변경 → **태형님 승인 완료 (2026-08-10, "2도 추가해")**, 세부 설계는 plan에서 확정
- **매수 이력 환율 컬럼 추가** — `StockPurchaseHistoryEntity`에 매수 시점 환율 필드가 없어 환차손익 산출 불가 → 컬럼 추가 + 마이그레이션. Entity 변경 → 신규 백엔드 4종 포함 결정에 수반, plan에서 명시적으로 재확인
- **자산 추이 스냅샷 도메인 신설** — 신규 테이블·Entity·API. plan에서 스키마 확정 후 진행
- **배당·이자 집계 서비스 신설** — 신규 public API 추가. plan에서 응답 스키마 확정 후 진행
- **국내주식/해외주식 분리** — 표시 레벨만 분리로 확정되어 Entity·API 변경 없음 → 게이트 해당 없음
- 범위가 커서 plan 단계에서 프론트 재구성 / 자산군 추가 / 신규 집계 API를 단계로 분할 예정

## Notes
- 목업은 번들 HTML(React + `x-dc` 템플릿). 템플릿·로직 스크립트를 추출해 대조함.
  - 추출물: 스크래치패드 `v3_inner.html`(템플릿), `v3_markup.txt`(스타일 제거본), `v3_script.js`(로직)
- v3에서 뉴스 화면은 로직(`isNews`·`newsTabs`·`articles`·`pages`)만 존재하고 템플릿·탭이 없어 도달 불가 → 태형님 확인 결과 **의도된 이관**. 포트폴리오에는 키워드 등록만 남기고 기사 열람은 키워드 메뉴로 이관 (Open Question 1 해소)
- 선행 이슈 #107(포트폴리오 그래프 가독성 개선)의 편차 중심 배분 카드가 v3 목표 배분 탭에 계승됨.
