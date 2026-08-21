# 월급 사용 비율 — 커스텀 카테고리 + 저축률 목표 설정화 Plan

- gate: docs/gates/2026-08-21-salary-usage-screen-redesign-gates.md
- issue: #116 (범위 확장 — 태형님 지시 "2도 지금 작업으로 진행해 / 3도 사용자 설정으로 바꿔")
- 선행: docs/plans/2026-08-21-001-feat-salary-usage-screen-redesign-plan.md (병합 전 동일 브랜치 연속 작업)

## 목표

1. 목업의 `+ 카테고리` / 카테고리 삭제(×)를 실제 동작하게 한다 — SpendingCategory enum 고정을
   사용자 정의 카테고리 테이블로 전환.
2. 저축률 목표(현재 프론트 상수 50%)를 사용자 설정으로 전환한다.

## 설계 요약

### 커스텀 카테고리
- 신규 테이블 `user_spending_category`: user_id · code(varchar20, UNIQUE per user) · name(40) ·
  color · savings(저축률 산입 플래그) · system(기본 8종 여부) · active(soft delete) · sort_order.
- **데이터 이관 불필요**: `@Enumerated(EnumType.STRING)`과 String 컬럼의 저장 형태가 동일 —
  Java 타입만 enum→String 전환, 기존 행의 'FOOD' 등은 시드 코드와 그대로 매칭.
- 시드: 사용자별 최초 접근 시 enum 8종을 기본 카테고리로 lazy 삽입 (code=enum name).
- 삭제 = soft delete + 해당 월부터 금액 0 레코드·항목 제거 스냅샷 (과거 월 이력 보존).
  월별 표시 규칙: active ∪ (그 월 유효 금액 > 0 인 inactive) — 과거 월은 삭제된 카테고리도 보임.
- 저축(savings) 카테고리는 삭제 불가(400). 커스텀 카테고리만 이름 변경 가능, 색은 서버 팔레트 순환.
- 추가/삭제/이름변경도 **일괄 저장 payload에 포함** (전체 폼 스냅샷 계약 유지):
  payload에 code 없으면 신규 생성(동명 inactive 있으면 재활성), payload에서 빠진 active 카테고리는
  비활성+0 처리. 되돌리기가 구조 변경까지 원복.
- 추이 응답에 `categories` 메타(코드·이름·색·savings) 추가 — `구성` 모드가 동적 카테고리로 동작.
- 레거시 개별 upsert/delete의 category 경로변수는 String 전환 + 사용자 카테고리 존재 검증.

### 저축률 목표
- 신규 테이블 `user_salary_setting`: user_id UNIQUE · saving_target_pct(int 0~100).
- 월별 응답에 `savingTarget`(미설정 시 50) 포함, 일괄 저장 payload `savingTarget`(null=유지)로 upsert.
- UI: 저축률 다크 카드의 "목표 N%"를 인라인 입력으로 — dirty 추적, 추이 목표선·인사이트에 즉시 반영.

## 체크리스트

### 백엔드
- [x] UserSpendingCategory 도메인(+기본 8종 시드 팩토리)·포트·엔티티·JPA·매퍼·Impl
- [x] SalarySetting 도메인·포트·엔티티·JPA·매퍼·Impl
- [x] SpendingConfig/SpendingItem(+Set)/엔티티/리포지토리 category String 전환
- [x] SalaryCategoryService — 시드·구조 반영(생성/재활성/이름변경/비활성)·메타 조회
- [x] SalaryService — 표시 규칙, savings 플래그 기반 저축률, 추이 메타, savingTarget, 구조 포함 일괄 저장
- [x] DTO — line(+color/savings/system), monthly(+savingTarget), trend(+categories), request(category nullable·name·savingTarget), CategoryMetaResponse
- [x] 레거시 엔드포인트 String 전환 + 존재 검증
- [x] SpendingCategory enum → 시드 카탈로그 역할로 주석 갱신 (상수 유지)

### 프론트
- [x] 버퍼를 응답 메타 기반 동적 카테고리로 전환 (SALARY_CATEGORIES 상수 제거)
- [x] + 카테고리 / 카테고리 ×(savings 제외) / 커스텀 이름 인라인 편집
- [x] 저축률 목표 인라인 입력 (다크 카드) — 추이 목표선·인사이트 연동
- [x] 구성 모드·범례를 추이 categories 메타 기반 동적 렌더
- [x] 저장 payload에 구조(신규 name·삭제 생략)와 savingTarget 반영

### 검증
- [x] 기존 데이터(enum 코드 행) 호환 — 시드 후 그대로 매칭되는지 E2E
- [x] 카테고리 추가→저장→이름변경→삭제→과거 월 이력 보존 E2E
- [x] savings 카테고리 삭제 거부, 목표 저장·반영 E2E
- [x] compileJava·test·브라우저 렌더 확인

## 리스크
- enum→String 전환은 wire 포맷 동일(JSON 'FOOD')이라 홈 대시보드 등 소비처 무영향.
- 커스텀 code는 varchar(20) 내 생성('U'+base36) — ddl-auto가 기존 컬럼 길이를 바꾸지 않는 제약 준수.
- 재활성 정책: 같은 이름의 inactive 카테고리를 새로 추가하면 기존 code 재활성(이력 연결).

## 후속 확장 (태형님 지시 "사소한 후속 저것도 지금 처리해줘")

- [x] 커스텀 카테고리 저축 지정 — payload `savings`(Boolean, null=미변경), 커스텀만 반영
  (system은 시드 값 고정). 삭제 보호는 기본 저축·투자(system+savings)로 한정 —
  커스텀 저축 카테고리는 자유 삭제.
- [x] 카테고리 순서 변경 — 일괄 저장 payload 순서가 sort_order로 authoritative 반영
  (`reorderToPayload`, 변경분만 저장). UI는 카테고리 행 좌측 ▲▼ 버튼(호버 시 표시,
  Alpine 배열 재배치 — SortableJS는 입력 밀집 행과 드래그 충돌이 커서 배제).
- [x] 문구 일반화 — 저축 카테고리가 복수가 될 수 있어 '저축·투자 N' → '저축 N',
  인사이트 '저축 카테고리로 옮기면'.
