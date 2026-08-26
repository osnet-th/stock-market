# 용어 사전 마스터-디테일 리디자인 Review 기록

gate: docs/gates/2026-08-21-glossary-redesign-gates.md

Status: Done (2026-08-21)
대상: work 산출물 전체 (docs/works/2026-08-21-glossary-redesign-work.md)
방식: 셀프 리뷰(적대적 재독) + 로컬 PG 실측 + Playwright 목 하네스 실측

## Findings (심각도순)

### F1 — High — @ElementCollection FK 자동 생성이 삭제 순서와 충돌 [조치 완료]
- 근거: `GlossaryTermEntity.java` 관계 컬렉션 / 로컬 PG 생성 스키마 실측
  (`glossary_term_related.term_id → glossary_term.id` FK 확인)
- 증상: `deleteByIdAndUserId` 가 용어 row 를 먼저 지우는 소유권-gate 순서라, 관계를 가진
  용어 삭제가 FK 위반으로 실패. 저장소 컨벤션(Entity 연관/FK 미사용)에도 위배.
- 조치: `@ForeignKey(ConstraintMode.NO_CONSTRAINT)` (GlossaryTermEntity.java:88) + 백업 SQL 은 원래 FK 없음.
- 재검증: 로컬 DB 재생성 후 FK 부재 확인, 스모크 테스트 outbound 삭제 통과.

### F2 — High — @OrderColumn position 구멍 → 매퍼 NPE (조회 500) [조치 완료]
- 근거: `GlossaryMapper.copyRelated` / Hibernate @OrderColumn 로드 시맨틱
- 증상: inbound 정리(native DELETE)가 다른 용어 컬렉션 position 에 구멍을 내면
  `[null, d]` 로 로드되어 `List.copyOf` NPE → 해당 용어 목록/상세 조회 전부 실패.
- 조치: 방어 복사를 null 요소 필터로 교체 (GlossaryMapper.java:66-71). 다음 save 에서 position 재정렬.
- 재검증: 임시 스모크 테스트 시나리오(c→[a,d] 에서 a 삭제 후 c 조회)로
  **수정 전 red(NullPointerException) → 수정 후 green** 확인. (스모크 파일은 정책상 미커밋 — 삭제)

### F3 — Medium — 목록 행 인라인 background 바인딩이 CSS :hover 를 영구 차단 [조치 완료]
- 근거: `glossary.html` 목록 행 / `custom.css` `.gl-row:hover`
- 증상: 인라인 style 은 stylesheet 를 항상 이기므로 선택/비선택 배경을 :style 로 주면 hover 피드백 소실.
- 조치: 선택 상태를 `.gl-row.on` 클래스로 전환, `:hover` 규칙을 `.on` 뒤에 배치해
  선택 행에도 hover 적용(목업 동일). #117 의 ":style 문자열 바인딩" 교훈의 확장 사례.

### F4 — Low — Alpine pane 전환이 클릭 직후 단언보다 늦게 반영 [테스트 측 조치]
- 근거: 하네스 debug2 실측 (클릭 직후 stale, ~100ms 내 정상)
- 판단: 앱 동작 정상(반응성 플러시 타이밍). 하네스 단언을 조건 대기(waitForFunction)로 교체.
  실사용 체감 영향 없음.

### F5 — Low — 초안 localStorage 키가 사용자 구분 없음 [기록 — 잔여 리스크]
- `glossaryDraft.v1` 단일 키. 같은 브라우저에서 계정을 바꿔 쓰면 새 용어 초안이 공유된다.
  저장 전 초안(새 용어)뿐이라 서버 데이터 노출은 아니며, 단일 사용자 앱 성격상 수용.
  필요 시 키에 사용자 식별자 suffix 추가로 해소 가능.

### F6 — Low — 전량 로드 상한 가드 [기록 — 잔여 리스크]
- 200건 × 최대 51페이지(≈1만 건) 루프 가드. 개인 사전 규모 전제(brainstorm 자율 결정 4).
  초과 규모가 되면 증분 로드로 재설계 필요 — 현재는 도달 비현실적.

## code-convention 점검

- 메서드 크기: 신규/변경 Java 메서드 대부분 5줄 이내. 예외 — `GlossaryTerm.normalizeRelated`(9줄,
  단일 책임 스트림 정규화), `GlossaryTermService.resolveRelatedRefs`(5줄) + `sanitizeRelatedInput`(분리),
  Request/Command/Mapper 의 다인자 생성자 호출은 mapper/converter 예외 조항 해당.
- 중첩: 전부 1단계 이하 (guard clause 우선).
- 의존성 방향: presentation → application → domain ← infrastructure 유지.
  domain 은 Java 표준만 사용(GlossaryTermContent/GlossaryTerm). VO 생성은 application 경계(`cmd.content()`).
- Entity ID-참조 원칙: 관계는 Long 컬렉션 — 연관관계 미도입, FK 미사용.

## Open Questions / Assumptions

1. 한 줄 정의를 소프트 필수(빈 값 저장 허용 + 빨간 힌트 + 목록 fallback)로 둔 판단 — 목업은 `*` 표기지만
   저장은 막지 않고, 레거시 데이터(one 없음) 호환 필요. 하드 필수로 바꿀지 태형님 확인.
2. `definition` 컬럼/API 필드명을 유지한 채 화면 라벨만 '풀이'로 재해석 — rename 마이그레이션을
   원하시면 별도 작업(수동 DDL + 전 계층 rename) 필요.
3. 카테고리 관리(이름 변경/삭제)는 목업에 없어 '관리' 팝오버로 보존 — 위치/노출 방식 취향 확인.
4. 목업의 삭제는 즉시 실행이지만 confirm 유지(복구 불가 데이터) — 제거 원하시면 1줄 수정.

## Change Summary

백엔드: 용어에 구조화 콘텐츠 VO(약어/한 줄 정의/풀이/기준/예시/투자 관점)와 함께 볼 용어
ID 컬렉션을 추가하고(하위호환 필드 확장), related 소유권 검증과 삭제 시 양방향 정리를 넣었다.
실측으로 FK 충돌·position 구멍 NPE 2건을 잡았다. 프론트: 용어 사전 화면을 목업대로
마스터-디테일 + 인라인 편집 + 통합/초성 검색 + 그룹 목록 + 초안 보관 구조로 전면 교체했다.
