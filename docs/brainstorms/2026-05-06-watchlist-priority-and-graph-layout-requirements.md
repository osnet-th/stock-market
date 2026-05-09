---
date: 2026-05-06
topic: watchlist-priority-and-graph-layout
issue: 42
branch: feat/issue-42-watchlist-priority-and-layout
---

# 관심지표 우선순위 및 그래프 레이아웃 개선

## Problem Frame

현재 사용자의 관심지표는 별도 정렬 컬럼이 없어 화면에 그려지는 순서가 일관되지 않다. 또한 GRAPH 표시 모드에서 다수의 관심지표가 한 행에 가로로 나열되어 한눈에 비교하기 어렵다. 사용자가 관심지표 표시 순서를 직접 제어하고, 그래프 표시도 화면 폭에 맞춰 여러 줄로 나뉘어 한눈에 보이도록 개선한다.

## Requirements

**우선순위 모델**
- R1. 관심지표 항목은 사용자별·`source_type`별로 정렬 우선순위를 갖는다. (전역 단일 정렬이 아님)
- R2. 새로 추가되는 관심지표는 동일 `source_type` 그룹의 맨 뒤에 위치한다.
- R3. `GET /api/favorites` 및 `GET /api/favorites/enriched` 응답은 `source_type` 그룹 내에서 우선순위 오름차순으로 정렬되어 반환된다.

**우선순위 편집 UX**
- R4. 사용자는 관심지표 영역에서 "순서 편집" 모드를 진입/종료할 수 있다.
- R5. 편집 모드는 같은 `source_type` × 같은 `displayMode` 컨테이너 단위로 진입한다. INDICATOR(카드) 컨테이너의 편집과 GRAPH 컨테이너의 편집은 서로 독립이다. 우선순위 컬럼은 두 모드가 공유하지만, 일괄 저장 페이로드에 포함되는 항목은 현재 편집 중인 컨테이너의 보이는 항목들로 한정된다.
- R6. 편집 모드는 "저장" 또는 "취소" 액션으로 종료한다. 저장 시 페이로드 항목들의 새 순서가 서버에 일괄 반영되고, 취소 시 드래그로 변경된 순서만 편집 진입 시점으로 되돌린다. 편집 중 다른 액션으로 변경된 멤버십(아래 R8)은 취소로 되돌리지 않는다.
- R7. 일괄 저장 시 동시성 정책은 "서버 결과 우선 + 보이는 항목들의 상대 순서만 갱신"으로 적용한다. 구체:
  - (a) 클라이언트 페이로드 내 ID 중 서버에 더 이상 존재하지 않는 것은 무시한다.
  - (b) 같은 `source_type`이지만 *다른 `displayMode`* 항목의 우선순위는 본 저장 호출로 변경하지 않는다. 구현 알고리즘: 트랜잭션 내에서 (i) 해당 `(user_id, source_type)`의 모든 행을 `SELECT ... FOR UPDATE`로 잠그고, (ii) 편집 컨테이너에 속한 항목들이 현재 점유한 priority 슬롯 집합 S를 수집한 뒤 (iii) 페이로드 순서대로 S의 값을 ASC 정렬하여 재할당하고 (iv) 다른 displayMode 항목의 priority는 그대로 유지. 결과적으로 `(user_id, source_type)` 그룹의 dense 0..N-1 invariant는 그룹 전체 집합 단위로 유지되며 per-displayMode 연속성은 보장되지 않는다.
  - (c) 같은 컨테이너인데 페이로드에 없으나 서버에 존재하는 항목(다른 탭에서 추가됨)은 해당 컨테이너의 맨 뒤로 부여한다.
  - (d) 두 사용자/탭이 같은 컨테이너의 같은 항목들을 다른 순서로 동시 저장하면 ordering intent는 last-writer-wins이다 (item membership은 항상 보존되지만 순서 충돌은 거부하지 않는다).
  - 일괄 적용은 트랜잭션 단위로 원자적이어야 한다.
- R8. 관심지표 추가·해제는 편집 모드 활성화와 무관하게 *즉시* 서버에 반영된다 (기존 동작 유지). 편집 모드 안에서 발생한 추가/해제도 즉시 서버에 반영되며 취소 액션의 영향을 받지 않는다 — 취소는 드래그한 순서 변경만 되돌린다. 표시 모드 전환은 편집 모드 종료(취소와 동일) 시도를 트리거하되, **dirty 상태(드래그로 변경된 미저장 순서가 존재)일 경우 confirm 다이얼로그로 사용자 확인을 받는다**. 사용자가 확인하면 미저장분 폐기 + 모드 전환, 거부하면 편집 모드 유지(모드 전환 취소). 페이지 이탈/새로고침에도 동일 패턴이 적용되어야 한다(deferred 항목과 일관 처리).

**그래프 모드 레이아웃**
- R9. GRAPH 표시 모드의 관심지표는 화면 폭에 따라 한 행에 최대 4개 항목까지 배치되는 반응형 그리드를 사용한다. 본 규칙은 ECOS·글로벌 등 모든 `source_type`의 GRAPH 영역에 동일하게 적용된다. INDICATOR(카드) 모드의 기존 레이아웃은 본 작업으로 변경하지 않는다.
- R10. 5번째 항목부터는 자동으로 다음 행으로 줄바꿈되어 다중 행 비교가 가능해야 한다 (행 수가 뷰포트를 넘는 경우 일반 페이지 스크롤 허용).

## Success Criteria

- 관심지표 목록의 표시 순서가 새로고침/재진입 후에도 사용자가 지정한 순서를 유지한다.
- 사용자가 편집 모드에서 드래그로 순서를 변경하고 저장하면 다음 조회부터 변경된 순서로 표시된다.
- 다른 탭에서 신규 관심지표가 추가된 상태로 일괄 저장이 발생해도, 신규 항목이 사라지지 않고 컨테이너의 맨 뒤에 보존된다.
- 두 사용자/탭이 같은 컨테이너의 항목들을 다른 순서로 동시 저장한 경우, 마지막 저장의 순서가 서버 상태로 남는다 (의도적 last-writer-wins; 데이터 손실은 발생하지 않으나 ordering intent는 후입자 우선).
- GRAPH 모드에서 관심지표가 5개 이상일 때 자동으로 다음 행으로 줄바꿈되어 다중 행 비교가 가능하다.
- 화면 폭을 좁혔을 때 GRAPH 그리드가 4 → 3 → 2 → 1열로 자연스럽게 축소된다.

## Scope Boundaries

- 비목표: `source_type`을 가로지르는 전역 단일 정렬은 제공하지 않는다.
- 비목표: priority 컬럼은 `(user_id, source_type)` 단위로 displayMode 간 *공유*되며, GET 응답은 displayMode와 무관하게 priority ASC로 단일 시퀀스를 반환한다 (클라이언트가 displayMode로 split). 비목표: 한 화면에서 displayMode가 다른 항목들을 가로질러 *직접* reorder하는 인터랙션은 지원하지 않는다 — 사용자는 각 컨테이너에 진입해 별도 편집 세션으로 정렬하며, 컨테이너 편집의 결과는 R7(b)의 슬롯 보존 알고리즘에 의해 다른 displayMode 항목들의 priority를 변경하지 않는 범위로 한정된다.
- 비목표: 우선순위 자동 추천(예: 변동성 큰 순) 또는 Pin 방식 대체안은 본 작업에서 도입하지 않는다.
- 비목표: 모바일 전용 별도 인터랙션 패턴은 본 작업 범위 밖이며, 기본 드래그 앤 드롭 라이브러리의 터치 지원 수준만 따른다.
- 비목표: INDICATOR(카드) 모드의 가로 스크롤 레이아웃은 본 작업에서 변경하지 않는다 (현재 마크업이 `flex overflow-x-auto snap-x` 기반이며 본 작업은 GRAPH 모드의 그리드 도입에만 한정).

## Key Decisions

- 정렬 단위는 `source_type`별: 화면 영역이 이미 그룹별로 분리되어 있어 그룹 경계를 넘는 정렬이 의미가 없음.
- 신규 항목 기본 위치는 그룹 맨 뒤(priority 큰 쪽): `created_at` 기반 backfill도 ASC 매핑으로 priority 작은 값 = 오래된 것, 큰 값 = 최신.
- 일괄 저장 + 서버 결과 우선 동시성: 다중 탭에서 멤버십 데이터 손실은 절대 발생하지 않음. ordering intent는 last-writer-wins로 의도적으로 단순화 (G1) — ETag/version 거부보다 사용자 흐름이 부드럽고, ordering intent 충돌은 매우 드문 시나리오로 판단.
- 컨테이너 단위 편집 (G3): R5 결정. 두 displayMode가 같은 priority 컬럼을 공유하지만 편집 UI는 컨테이너별로 진입하며, 일괄 저장은 보이는 항목들의 상대 순서만 갱신. 사용자가 한 번에 보이지 않는 항목과의 상대 위치를 의도적으로 조작하는 시나리오는 비목표.
- 추가/해제는 편집 모드와 독립 (G2): 즉시 서버 반영. 취소는 드래그 순서만 되돌림. R8과 R7로 자연 수렴.
- 출시 시점은 단일 — 머지 단위는 분리 가능: 사용자 시나리오상 우선순위와 그래프 다중 행 비교가 같은 화면에서 함께 체감되어야 하므로 사용자에게 노출되는 시점은 동기화한다 (예: feature flag 또는 동시 머지). 단 PR 단위는 GRAPH 그리드(R9~R10) PR과 priority(R1~R8) PR로 분리해도 무방 — 리뷰 부담을 줄이고 GRAPH 그리드 PR이 priority 컬럼 의존 없이 독립적으로 검증 가능하다는 이점이 있음.
- DnD 방식 채택(Pin 대체안 거부): 이슈 #42 본문이 명시적으로 "우선순위를 지정하여 순서를 지정"하는 것을 요구하고, "관심지표 추가 순서가 의도와 다르다"는 점이 핵심 불편이므로 직접 정렬이 가장 직관적.
- Entity 변경(priority 컬럼 추가)은 Approval Gate 대상 → planning 단계 시작 시 사용자에게 별도 승인 요청 필요. 잠정 타입: `INT NOT NULL DEFAULT 0` (단, 본 코드베이스 컨벤션상 default 부여 후 backfill은 nullable 단계 거쳐 NOT NULL 마이그레이션이 안전).

## Dependencies / Assumptions

- `user_favorite_indicator` 테이블에 우선순위 컬럼 추가가 필요하다는 가정 (실제 컬럼 명칭/타입은 planning에서 확정).
- 본 repo의 schema 관리는 `spring.jpa.hibernate.ddl-auto=update` + `src/main/resources/db/migration/*.sql` 하이브리드 패턴이다 (Flyway/Liquibase 자동 실행기 없음 — 운영자가 수동으로 SQL 실행). 권장 흐름: (1) 우선순위 컬럼을 nullable 또는 default 0 으로 Entity에 추가 → ddl-auto가 컬럼 생성, (2) 운영자가 `db/migration/`의 backfill SQL을 실행해 `created_at` 기준 우선순위 부여, (3) 필요 시 NOT NULL 제약 추가. backfill 전 짧은 시간 동안 기존 행이 priority 동률 상태가 되며 이때 R3 정렬은 tiebreaker(예: id ASC)로 결정된다.
- 기존 `displayMode` 컬럼과는 독립적으로 동작.
- 프론트엔드는 Alpine.js + CDN 스크립트 로드 방식이므로 (npm 빌드 파이프라인 없음), DnD 라이브러리 선택 시 UMD/CDN 배포 가능 여부와 Alpine 반응성과의 공존(예: SortableJS onEnd → underlying array splice)을 충족해야 함.
- 가설 ("5+ 보유 사용자가 의미 있는 비율로 존재") 검증은 dev DB 표본 부족(2026-05-07: user 1·favorites 3)으로 본 brainstorm 단계에서 수행하지 못함. 가설은 수용하되 R9~R10이 출시된 후 prod DB 또는 사용자 행동 로그로 검증할 것 (후행 조치 — 본 작업 범위 밖). 검증 결과가 가설을 지지하지 않을 경우 R9~R10의 4열 상한 재산정 또는 단순화를 별도 이슈로 다룸.

## Outstanding Questions

### Resolve Before Planning
- (없음 — 보유 분포 spike는 dev DB에 표본이 없어(2026-05-07 시점 user 1명·favorites 3건) 통계적 검증 불가. 사용자 결정으로 가설을 수용하고 진행. 후행 조치는 Dependencies/Assumptions 참조.)

### Deferred to Planning
- [Affects R1, R6, R7][Technical] 우선순위 컬럼 타입은 정수 인덱스(`priority INT`, `(user_id, source_type, priority)` 단위로 0..N-1)로 잠정 채택. planning 첫 단계에서 Entity Approval Gate 진입 시 최종 확정. 동률 발생 시 tiebreaker는 `id ASC`.
- [Affects R2, R7][Technical] 신규 항목 추가(`POST /api/favorites`)의 priority 부여 동시성: `INSERT ... SELECT COALESCE(MAX(priority),-1)+1 ... WHERE user_id=? AND source_type=?`를 단일 statement로 실행 + Postgres `UNIQUE(user_id, source_type, priority) DEFERRABLE INITIALLY DEFERRED` 제약 + 23505 충돌 시 1회 재시도 패턴으로 잠정. R7 일괄 저장의 swap-update에서도 동일 제약이 DEFERRABLE이어야 mid-transaction 위반이 허용됨. 최종 형태는 planning에서 확정.
- [Affects R7][Technical] R7 일괄 저장 페이로드의 엣지 케이스: 빈 페이로드(no-op vs 400), 중복 ID(첫 항목 채택 vs 400), 다른 source_type ID 혼입, 인증 사용자와 ID 소유자 불일치 — 각각의 거부/수용 정책 명시 필요.
- [Affects R3][Technical] `created_at` ASC 기준 backfill SQL 작성 (`db/migration/`).
- [Affects R5][Needs research] Alpine.js + CDN 환경에서 동작하는 DnD 라이브러리 선정 (SortableJS UMD 잠정). **Planning 진입 전 30분 spike 권장**: 현 `favorite.js`에 SortableJS를 1개 그룹에 붙여 reactive splice가 깜박임 없이 동작하는지 검증 (Alpine x-for + SortableJS DOM mutation 충돌은 알려진 함정).
- [Affects R7][Technical] 일괄 저장 엔드포인트 설계: `PUT /api/favorites/order` 신규 vs 기존 컬렉션 PUT. 페이로드 형태(전체 목록 vs 변경분).
- [Affects R6, R7][UX] 일괄 저장 실패 시 회복 흐름: 편집 모드 유지 + 재시도 vs 폐기 후 종료. 토스트 메시지 문구.
- [Affects R8][UX] 편집 중 페이지 이탈/새로고침 시 미저장 변경분 처리: R8 dirty-state confirm과 동일 패턴(beforeunload 경고) 적용. 다이얼로그 문구 확정만 planning에서.
- [Affects R5][UX] 드래그 핸들 위치(카드 전체 vs 전용 핸들). 카드 전체일 경우 기존 클릭 액션과의 충돌 해소 방안.
- [Affects R5][UX] 모바일/터치에서 일반 스크롤과 드래그 시작 구분(long-press, 전용 핸들 등).
- [Affects R5, R6][Needs research, a11y] 키보드 접근성: 드래그 진행/완료/실패의 aria-live announce, 편집 모드 진입 시 포커스 이동 정책. DnD 라이브러리 기본 지원 수준에 따라 본 작업 범위 vs 별도 이슈 분리 결정.
- [Affects R5][UX] 그룹 항목이 0~1개일 때 편집 모드 진입 가능 여부 (1개일 땐 진입 가능하지만 드래그 불가, 0개는 진입 불가가 자연스러움).
- [Affects R9][Technical] CSS 그리드 vs Flexbox 접근. 현재 GRAPH 모드 마크업은 별도 CSS 파일이 아니라 `static/partials/home.html`과 `static/js/components/favorite.js` 인라인 스타일에 분포함이 확인됨 (`flex gap-4 overflow-x-auto pb-2 snap-x snap-mandatory` 패턴). 그리드 적용 시 기존 클래스 교체 필요.
- [Affects R9, R10][Technical] GRAPH 그리드 도입 시 기존 `snap-x snap-mandatory` / `overflow-x-auto` 클래스 처리: 그리드는 가로 오버플로가 없으므로 snap이 무의미 — 제거가 자연스러움. 단, 카드 내부에 가로 시계열 등이 있다면 셀 내부 snap은 별도 결정.
- [Affects R5][UX] 글로벌 영역에서 `failed=true` 상태의 GRAPH displayMode 항목이 INDICATOR 컨테이너에 fallback 렌더링되는 기존 동작(`home.html` 확인)과 컨테이너 단위 편집 정의 충돌: 해당 항목이 INDICATOR 편집 페이로드에 포함될지 제외될지 명시 필요. 잠정 권장: 편집 페이로드에서 제외(R7(a) "서버에서 다른 컨테이너에 속함"으로 무시 처리).
- [Affects R5, R7][UX] 컨테이너 편집이 다른 displayMode 표시 순서에 미치는 결과를 사용자에게 surfacing할지 여부: 편집 진입 시 헤더 힌트 vs 저장 후 토스트 vs 표시 안 함 — R7(b) 슬롯 보존으로 영향이 minimal하나 0은 아님.
- [Affects R9][Technical] 구체 브레이크포인트(예: <640px 1열, <960px 2열, <1280px 3열, ≥1280px 4열) 확정.
- [Affects R4~R8][UX] 편집 모드 UI 트리거 위치(컨테이너별 헤더 옆 버튼), 저장/취소 버튼 배치(sticky 푸터 vs 인라인), 드래그 중 placeholder, 저장 진행/실패 시 시각 피드백.

## Next Steps
→ `/ce:plan` for structured implementation planning
