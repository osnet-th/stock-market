# 용어 사전 마스터-디테일 리디자인 Issue 기록

gate: docs/gates/2026-08-21-glossary-redesign-gates.md

## GitHub Issue
- status: created
- issue_number: #118
- issue_url: https://github.com/osnet-th/stock-market/issues/118
- title: [enhancement] 용어 사전 마스터-디테일 리디자인 — 구조화 필드(풀이·기준·예시·투자 관점)·함께 볼 용어·초성 검색
- label: enhancement
- 경위: 최초 시도 시 GitHub MCP 자격 증명 오류(Bad credentials)로 실패해 구현이 선행됐다
  (2026-08-21 뉴스 기록 리디자인 세션 #117 과 동일 증상). 세션 GitHub 연결 복구 후
  **사후 등록** (2026-08-21, push 이후) — #117 과 동일한 처리.

## 근거
- brainstorm: docs/brainstorms/2026-08-21-glossary-redesign-brainstorm.md (Status: Decided)
- 태형님이 목업 HTML(업로드 `4b044daa-*.html`) 제시 —
  "이거 대로 용어 사전에 대한 화면 변경해주고 부족한 부분은 백엔드 코드 수정해서 보강해줘"

## Branch
- branch: claude/glossary-ui-backend-update-dnbbk2 (원격 세션 지정 브랜치 — worktree 스크립트 미사용)
- base: main

## 작업 범위 요약
1. 백엔드 확장 (glossary 도메인, 기존 API 하위호환)
   - `GlossaryTerm` 구조화 필드: abbreviation(약어·영문) / oneLine(한 줄 정의) /
     definition(풀이로 재해석·유지) / scaleNote(기준·읽는 법) / example(예시) / takeaway(투자 관점)
   - 함께 볼 용어: `glossary_term_related` @ElementCollection(ID 참조) —
     소유권 검증·자기참조/중복 제거·상한 20·삭제 시 양방향 정리
   - Request/Command/Response 확장, SQL 백업 파일(`db/migration/`) 추가
2. 프론트 전면 리라이트 (`partials/glossary.html`, `components/glossary.js`)
   - 마스터-디테일 + 인라인 보기/편집(모달 제거), 새 용어 초안 localStorage 보관·이어쓰기 칩
   - 통합 검색 + 한글 초성 검색, 가나다(초성)/최신(월별) 그룹 목록(sticky)
   - 카테고리 칩(건수)·미분류, 카테고리 관리 팝오버(이름 변경·삭제 영향 미리보기 보존)
   - 디테일 4섹션 컬러 카드(-/• 서브불릿), 점프 칩, 미작성 nudge(N/4 작성·채우기), 함께 볼 용어
   - 좁은 화면 단일 pane 전환(임계 1280px)
3. 검색/그룹/건수는 전량 로드 후 클라이언트 사이드 (개인 사전 규모)

## 참고 자료
- 목업: 업로드 `4b044daa-*.html` (번들 → 디자인 캔버스 템플릿 35KB + 상태 로직 추출 대조)
- 선행: #114 (`--dc-*` 토큰·IBM Plex), #117 (뉴스 기록 마스터-디테일 — 패턴·실버그 회피 계승)
- 원 이슈: #43 (개인 용어 사전 최초 구현)
