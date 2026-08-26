# 용어 사전 마스터-디테일 리디자인 Plan

gate: docs/gates/2026-08-21-glossary-redesign-gates.md

Status: Done (2026-08-21)
Issue: docs/issues/2026-08-21-glossary-redesign-issue.md (bootstrap-exception — 사후 등록)
Brainstorm: docs/brainstorms/2026-08-21-glossary-redesign-brainstorm.md

## 목표

업로드 목업(`4b044daa-*.html`)대로 용어 사전 화면을 구조화 학습 노트형 마스터-디테일로
전환하고, 목업 데이터 모델에 없는 백엔드 필드/기능(구조화 5필드 + 함께 볼 용어)을 보강한다.
기존 API 는 하위호환을 유지한다(파라미터/의미 변경 없음, 필드 추가만).

## 사전 확인 사실

- list 응답(`GlossaryTermResponse.from`)이 용어 전체 필드를 이미 포함 → 디테일 pane 추가 페치 불필요,
  전량 로드 후 클라이언트 사이드 검색/그룹이 성립
- 스키마 권위는 Entity(`ddl-auto: update`) — 컬럼 추가는 자동, rename 은 데이터 고아화라 금지.
  `db/migration/*.sql` 은 DBA 수동 적용 백업용 컨벤션 (기존 파일 헤더 명시)
- JPQL 벌크 DELETE(`deleteByIdAndUserId`)는 @ElementCollection 행을 지우지 않음 → native 정리 쿼리 필요
- `LikeEscaper`/`GlossaryTermSort`/카테고리 API 는 변경 불필요
- Alpine 문자열 `:style` 바인딩은 정적 style 을 통째로 대체하는 실버그(#117 발견) → 객체 바인딩만 사용
- 폰트(IBM Plex Sans KR/Mono)·`--dc-*` 토큰은 index.html/custom.css 에 이미 존재

## Phase 1 — 백엔드 (glossary 도메인)

- [x] `domain/model/GlossaryTermContent.java` 신설 — record VO
  - abbreviation(≤200) / oneLine(≤300) / definition(≤4000) / scaleNote(≤4000) / example(≤4000) / takeaway(≤4000)
  - compact constructor 에서 길이 검증(작은 helper 로 분리), null 허용(선택 필드)
- [x] `GlossaryTerm` 확장 — `definition` 단일 필드를 `content`(VO) 로 대체, `relatedTermIds` 추가
  - `create(userId, name, content, categoryId, relatedTermIds)` / `replace(...)` 시그니처 확장
  - relatedTermIds: null→빈 리스트, 중복 제거, 상한 20, 불변 보관. 자기 참조 제거는 service 책임(신규는 id 미정)
- [x] `GlossaryTermEntity` — 컬럼 5개 추가(abbreviation VARCHAR(200), one_line VARCHAR(300),
  scale_note/example/takeaway TEXT) + `@ElementCollection` `glossary_term_related` (+`@BatchSize`)
- [x] `GlossaryMapper` — content VO/관계 리스트 왕복 매핑
- [x] `GlossaryTermJpaRepository` — ① `findOwnedIds(userId, ids)` (관계 소유권 검증용)
  ② 관계 정리 native DELETE (`term_id = :id OR related_term_id = :id`)
- [x] `GlossaryTermRepositoryImpl` — 포트 구현 확장 (`deleteByIdAndUserId` 에서 관계 정리 선행)
- [x] `GlossaryTermRepository` 포트 — `findOwnedIds` 추가, 삭제 계약 주석에 관계 정리 명시
- [x] Create/Update Command + Request — 신규 필드 + `relatedTermIds`(@Size max 20) 추가, @Size 검증
- [x] `GlossaryTermResponse` — 신규 필드 + relatedTermIds 노출
- [x] `GlossaryTermService` — related 정규화(자기 참조/중복/비소유 id 제거) 후 도메인 전달
- [x] `db/migration/glossary_term_detail_fields_2026_08_21.sql` — ALTER ADD COLUMN + 관계 테이블 백업 DDL

주의: 서버 `q`(용어명)/`definitionQ`(definition) 검색 파라미터는 의미 불변 유지. 신 UI 는 미사용.

## Phase 2 — 프론트 전면 리라이트

- [x] `custom.css` — `.gl-*` 클래스 추가 (shell 높이, 칩, 세그, 행, 팝오버, 입력 — `.nj-*` 패턴 준용)
- [x] `partials/glossary.html` 전면 교체
  - 헤더: 제목 · 통합 검색(placeholder "용어 · 정의 · 예시까지 검색 · ㅅㅂㅈ 초성도 됩니다" · × 클리어)
    · '등록 N개 · 채움 필요 N개' · 초안 이어쓰기 칩 · [+ 용어 등록]
  - 목록 pane: 카테고리 칩(전체/미분류/카테고리별 건수 + '관리' 팝오버) · 결과 노트('N개 · 쿼리 초성')
    · 가나다순/최신순 세그 · 초성/월별 그룹(sticky) · 행(용어명·약어·한 줄·카테고리·N/4 작성·날짜)
    · 빈 결과('해당하는 용어가 없습니다' + '쿼리' 용어로 등록)
  - 디테일 pane: 보기(헤더·점프 칩·4섹션 카드·함께 볼 용어·미작성 nudge·빈 상태) /
    편집 인라인(용어명*·약어·카테고리 칩+새 카테고리 Enter·한 줄 정의 강조 박스·4 textarea
    (힌트·글자수·placeholder)·함께 볼 용어 추가/제거·닫기/저장·초안 안내)
  - 좁은 화면(<1280px): 단일 pane + '‹ 용어 목록 (N개)' 백 버튼, Escape 처리
- [x] `components/glossary.js` 전면 교체
  - 전량 로드(list API size 200 루프) + categories 로드
  - 초성 유틸(chosung/isCho/initial) · 통합 match · 그룹핑(초성/월) · 카테고리 건수
  - 상태: selId/mode/pane/query/cat/sort/draft/dirty, localStorage 초안(`glossary:draft:v1`)
  - CRUD: 저장(POST/PUT, 저장 후 선택 유지) · 삭제(confirm) · 카테고리 CRUD(관리 팝오버) 보존
  - XSS: x-text 만 사용(기존 정책 유지)
- [x] `api.js` — 기존 glossary 함수 재사용(변경 없음 확인)

## Phase 3 — 검증

- [x] `./gradlew compileJava compileTestJava test`
- [x] `node --check` (glossary.js)
- [x] Playwright 목 하네스(스크래치패드, API 스텁 + partial 인라인) — 로드/검색/초성/그룹/선택/
  편집/초안/카테고리/관계 시나리오 실측
- [x] `scripts/check-documented-workflow.sh --through push`

## 리스크

- `ddl-auto: update` 환경에서만 신규 컬럼 자동 생성 — 운영 DB 는 백업 SQL 수동 적용 필요
- 전량 로드는 개인 사전 규모 전제(수천 건 이상이면 증분 로드 재검토)
- definition 재해석: API 필드명은 definition 유지 — 프론트 라벨만 '풀이'
