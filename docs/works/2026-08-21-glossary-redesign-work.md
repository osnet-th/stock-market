# 용어 사전 마스터-디테일 리디자인 Work 기록

gate: docs/gates/2026-08-21-glossary-redesign-gates.md

Status: Done (2026-08-21)
Plan: docs/plans/2026-08-21-001-feat-glossary-redesign-plan.md

## Phase 1 — 백엔드

### 신규/변경 파일

| 파일 | 변경 |
|---|---|
| `glossary/domain/model/GlossaryTermContent.java` | 신설 — 구조화 콘텐츠 VO (abbreviation ≤200 / oneLine ≤300 / definition·scaleNote·example·takeaway ≤4000, 전부 선택), compact constructor 길이 검증 |
| `glossary/domain/model/GlossaryTerm.java` | `definition` 단일 필드 → `content`(VO) + `relatedTermIds` 추가. create/replace 시그니처 확장, `normalizeRelated`(null/자기참조/중복 제거 + 상한 20) |
| `glossary/infrastructure/persistence/GlossaryTermEntity.java` | 컬럼 5종 추가(abbreviation/one_line/scale_note/example/takeaway) + `@ElementCollection glossary_term_related` (`@OrderColumn(position)`, `@BatchSize(200)`, **FK NO_CONSTRAINT**) |
| `glossary/infrastructure/persistence/mapper/GlossaryMapper.java` | content VO/관계 리스트 왕복 매핑, 관계 방어 복사(**null 요소 필터** — 아래 실측 수정 2) |
| `glossary/infrastructure/persistence/GlossaryTermJpaRepository.java` | `findOwnedIds(userId, ids)` (IN, 빈 리스트는 어댑터 guard) + `deleteRelationsForTerm` (native, 양방향, clearAutomatically) |
| `glossary/infrastructure/persistence/GlossaryTermRepositoryImpl.java` | `findOwnedIds` 빈 입력 guard, `deleteByIdAndUserId` — 소유 확인된 삭제 성공 시에만 관계 정리 |
| `glossary/domain/repository/GlossaryTermRepository.java` | 포트에 `findOwnedIds` 추가, 삭제 계약 주석 갱신 |
| `glossary/application/dto/{Create,Update}GlossaryTermCommand.java` | 구조화 필드 + relatedTermIds, `content()` 헬퍼 |
| `glossary/presentation/dto/{Create,Update}GlossaryTermRequest.java` | @Size 검증 포함 필드 확장 (relatedTermIds @Size max 20) |
| `glossary/presentation/dto/GlossaryTermResponse.java` | 신규 필드 + relatedTermIds 노출 (list 응답도 동일 DTO 경유) |
| `glossary/application/GlossaryTermService.java` | `resolveRelatedRefs` — null/자기참조/중복 제거 후 `findOwnedIds` 로 소유 검증, 비소유/미존재는 **조용히 제거**(입력 순서 보존, 존재 여부 비노출) |
| `db/migration/glossary_term_detail_fields_2026_08_21.sql` | DBA 백업 DDL (ALTER ADD COLUMN ×5 + 관계 테이블 + 인덱스 + 롤백 주석) |

기존 API 하위호환: 엔드포인트/기존 필드/검색 파라미터(q, definitionQ) 의미 불변 — 필드 추가만.

### 실측 수정 (로컬 PG16 기동 후 발견)

1. **@ElementCollection FK 자동 생성 ↔ 삭제 순서 충돌 (High)**
   Hibernate 가 `glossary_term_related.term_id → glossary_term.id` FK 를 자동 생성.
   `deleteByIdAndUserId` 는 용어 row 를 먼저 지우므로(소유권 gate) 관계 보유 용어 삭제가
   FK 위반으로 실패한다. 저장소 컨벤션(FK 미사용)에 맞춰 `@ForeignKey(NO_CONSTRAINT)` 로 억제.
2. **@OrderColumn position 구멍 → 매퍼 NPE (High)**
   inbound 정리(native DELETE)가 다른 용어 컬렉션의 position 에 구멍을 내면 Hibernate 는
   그 자리를 null 로 채워 `[null, d]` 로 로드 → `List.copyOf` NPE (해당 용어 조회 전부 500).
   매퍼 방어 복사를 null 요소 필터로 교체. 임시 스모크 테스트로 **red(사전) → green(사후)** 재현 검증
   (검증 후 파일 삭제 — 테스트 미작성 정책 준수, validation 문서 참조).

## Phase 2 — 프론트

### 신규/변경 파일

| 파일 | 변경 |
|---|---|
| `static/partials/glossary.html` | 전면 교체 282 → 407줄 — 목업 구조 그대로 (헤더/목록 pane/디테일 pane/편집 인라인/카테고리 관리 팝오버/삭제 확인 다이얼로그) |
| `static/js/components/glossary.js` | 전면 교체 334 → 732줄 — 전량 로드(200건 루프), 초성 유틸, 통합 match, 초성/월별 그룹, 초안 localStorage(`glossaryDraft.v1`), 관계 편집, 카테고리 관리 |
| `static/css/custom.css` | `.gl-*` 클래스 추가 (shell 높이/버튼/세그/칩/행(.on 선택)/팝오버/입력/관계 카드) |

index.html / app.js / api.js 변경 없음 — 진입점 `glossaryLoad()` 이름 유지로 app.js dispatch 재사용.

### 구현 노트 (#117 선례 준수)

- `:style` 은 전부 **객체 바인딩** (문자열 바인딩은 정적 style 을 지우는 실버그)
- 목록 행 선택 배경은 인라인이 아닌 `.gl-row.on` 클래스 — 인라인 background 는 CSS `:hover` 를
  영구히 덮는다 (하네스 실측으로 확정 후 전환)
- 편집 블록은 `template x-if` — `glossary.form` 은 항상 객체(빈 폼)라 x-model 안전
- 편집 중 다른 행 선택/새 용어 진입 시에도 새 용어 초안은 stash 후 전환 (목업보다 보수적 — 유실 방지)
- 새 용어 초안은 입력 즉시 localStorage write-through ("자동 보관" 문구에 맞춤), 수정 편집은 보관 안 함(목업 동일)
- 좁은 화면 임계 1280px (목업 1080 + 사이드바 보정), pane 상태 유지
- XSS: 사용자 입력 렌더링 x-text 만

## Plan 대비 이탈

- 없음 (plan 체크리스트 전 항목 완료). 목업 대비 의도적 차이는 brainstorm '자율 결정' 12건 참조.
