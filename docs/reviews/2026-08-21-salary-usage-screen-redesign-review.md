# 월급 사용 비율 화면 목업 기반 재설계 Review 기록

- gate: docs/gates/2026-08-21-salary-usage-screen-redesign-gates.md
- 방식: 자율 세션 self review (구현 직후 전체 diff 재독 + 실측 검증)

## Findings (심각도 순)

명시적 findings 없음 — 아래는 리뷰 중 발견해 이번 작업 안에서 즉시 조치한 항목과 남은 리스크다.

### 리뷰 중 발견·조치 완료
1. [조치] 잘못된 enum 문자열 요청 body가 500으로 응답 — `HttpMessageNotReadableException` → 400
   핸들러 추가 (`GlobalExceptionHandler`). 실측으로 400 확인.
2. [조치] application 계층이 presentation DTO(SaveMonthlyRequest)를 직접 받도록 초안 작성했다가
   의존성 방향 위반으로 판단, `SaveMonthlyCommand`(application/dto) 도입 후 presentation에서 변환.
3. [조치] `SpendingConfigJpaRepository.findEffectiveAsOf` 네이티브 쿼리 select 목록에 budget 누락
   시 컬럼 매핑 오류 가능 — select 목록에 추가.
4. [조치] 1만 미만 diff가 '0만'으로 표시(예: '교통 예산 초과 0만') — `salaryMan`을 원 단위 fallback
   으로 개선, 예산 정확 일치 시 `±0`.
5. [조치] 다른 페이지 재진입 시 `loadSalaryInitial`이 미저장 편집 버퍼를 조용히 폐기 —
   dirty+로드완료 상태면 재로드 skip 가드 추가.
6. [조치] 레거시 개별 지출 upsert가 새 레코드 생성 시 상속 budget을 null로 유실 — 상속 budget
   이어받기로 보존.

### 남은 리스크 (수용)
1. `spending_config.amount`의 파생 저장 불변식(항목 합계)은 일괄 저장 API가 유일한 쓰기 경로일 때
   유지된다. 레거시 개별 upsert로 항목 보유 카테고리에 쓰면 불일치 가능 — 신규 UI는 개별 upsert를
   사용하지 않고, 화면은 항목 합계를 우선 계산하므로 표시 정합은 유지되며 다음 일괄 저장이 재유도한다.
2. 항목 세트는 월 단위 전체 스냅샷 — 카테고리 1곳만 바뀌어도 세트 전체가 신규 기록된다. 저장 UX가
   전체 폼 일괄 저장이라 의미상 자연스럽고 상속 계산이 단순해지는 트레이드오프로 수용.
3. `PUT /monthly`에 카테고리 일부만 보내는 클라이언트는 보낸 카테고리만 갱신되고 항목 세트는 보낸
   내용 전체로 스냅샷된다(전체 폼 계약). 실제 UI는 항상 8종 전체 전송.
4. 저축률 목표 50%는 프론트 상수 — 사용자 설정화는 후속 과제.

## Open Questions / Assumptions
- 커스텀 카테고리 추가/삭제(목업 `+ 카테고리`, 카테고리 ×)는 enum→테이블 전환 규모라 범위 제외.
  후속 이슈로 분리할지 태형님 확인 필요.
- 목업의 사이드바/헤더는 앱 셸 컨텍스트 표현으로 해석 — 기존 `_sidebar`/`_header` 유지.
- 접기/펼치기 초기 상태는 전체 접힘(목업은 여가·저축 열림) — 세션 내 상태는 유지된다.

## Change Summary
- 백엔드: 하위 항목 스냅샷 모델(`spending_item_set`/`spending_item`) + `spending_config.budget` +
  일괄 저장 API + 월별 응답(items·budget·previous)·추이(categoryTotals) additive 확장.
- 프론트: salary 화면을 목업과 동일 구성으로 전면 재구현(SVG 추이 3모드, 계층 편집 테이블,
  dirty 일괄 저장). Chart.js 제거.
- 컨벤션: 코드 규칙(작은 메서드·guard clause) 준수, 기존 Effective Date/NOOP 의미론·홈 대시보드
  소비 계약 무파괴.

---

## 범위 확장 리뷰 (커스텀 카테고리 + 목표 설정화)

### 리뷰 중 발견·조치 완료
7. [조치] **Hibernate enum CHECK 제약** — `@Enumerated(EnumType.STRING)`으로 생성된 테이블에
   `category IN (8종)` CHECK 제약이 존재, 커스텀 code INSERT가 409로 실패 (로컬 E2E에서 발견).
   `salary_category_check_drop_2026_08_21.sql` 수동 마이그레이션 신설. **운영 배포 시 필수 실행**
   — 이전 회신("운영 반영 시 따로 수정할 것 없음")을 뒤집는 사항으로 태형님께 명시 보고.
8. [조치] 신규 카테고리(key null) 복수 추가 시 x-for key 충돌 — 리본/지난달 대비/카테고리 행
   key를 로컬 uid 기반으로 전환.

### 남은 리스크 (수용)
5. 커스텀 code 생성이 timestamp(base36)+seq 기반 — 같은 밀리초의 동시 저장 충돌 시 UNIQUE
   제약 → 409 재시도 안내로 방어 (실사용 단일 사용자 환경에서 확률 무시 가능).
6. 삭제(비활성)는 해당 월부터 금액 0 레코드로 마감 — 과거 월 이력은 표시 규칙
   (활성 ∪ 그 월 금액 보유 비활성)으로 보존 확인. 동명 재추가 시 기존 code 재활성으로 이력 연결.
7. 저축(savings) 카테고리는 삭제 불가(400)·이름 고정. 커스텀 카테고리를 저축으로 지정하는
   기능은 미제공 (후속 여지).

### Change Summary (확장분)
- 카테고리 enum 고정 → 사용자 정의 테이블(시드 8종 + 커스텀 CRUD, soft delete, 이력 보존).
- 저축률 목표 프론트 상수 → 사용자 설정(`user_salary_setting`) + 인라인 편집.
- 수동 마이그레이션 1건 신설 (CHECK 제약 제거) — 운영 반영 체크리스트에 포함.
