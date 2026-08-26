# 뉴스 기록 마스터-디테일 리디자인 Review

gate: docs/gates/2026-08-21-news-journal-redesign-gates.md

셀프 리뷰 (원격 자율 세션 — 리뷰어 별도 없음). Findings 심각도순.

## Findings

### F1 (High, 조치 완료) — Alpine 문자열 `:style` 이 x-show 의 display 를 지움
- 근거: `alpinejs/dist/cdn.js` `setStylesFromString` → `el.setAttribute('style', value)`
- 증상: 리사이즈(vw 변경) 시 닫힌 태그 패널이 재출현해 목록 클릭 가로챔 + 정적 style 소실로
  행 padding 등 목업 스타일 붕괴 (`partials/news-journal.html` 전반)
- 조치: 문자열 바인딩 35곳 전부 객체 바인딩 전환, 목 하네스 61항목 재실측 PASS

### F2 (Medium, 조치 완료) — 키워드 11개 선택 시 서버 400 유발
- 근거: `NewsEventListFilter.KEYWORDS_MAX=10` vs 프론트 `tagSel` 무상한
- 조치: `njToggleTag` 에서 10개 초과 추가 무시 (`news-journal.js:239`) — 서버 상한과 동기

### F3 (Low, 조치 완료) — 미사용 상태 `_njEntered`
- 조치: 제거

### F4 (Low, 조치 완료) — `findStats` 컨벤션(작은 메서드) 위반
- 조치: `impactCountsOf`/`categoryCountsOf`/`keywordEventsOf` 3헬퍼로 분리, 전체 테스트 재PASS

### F5 (Low, 수용) — pair 키 구분자가 제어문자 리터럴로 삽입됨
- 초안 작성 시 `` 이 소스에 리터럴로 들어가 diff/린트에 보이지 않는 문자가 남을 뻔함
- 조치: 명시적 `''` 이스케이프로 교체 (`news-journal.js` 3곳)

## 미조치 리스크 (범위 밖 / 수용)

- **R1 무인증 호출 dev 500** — `GET /stats` 무인증 시 500. **기존 `GET /events` 도 동일**(실측),
  운영은 security 레이어가 401 처리. #114 잔여 후보(공용 인증 핸들러)와 같은 계열 — 범위 밖
- **R2 레거시 링크 URL 스킴** — 백엔드가 URL 스킴을 검증하지 않아 과거 데이터에 비 http(s)
  링크가 있을 수 있음. 신규/수정 저장은 프론트가 `https?://` 강제. `rel="noopener noreferrer"` 적용
- **R3 q LIKE 검색은 인덱스 미사용** — 개인 기록 규모(수백~수천)에서 실측상 문제 없음.
  ES 전환은 범위 밖 (brainstorm 리스크 항목 그대로)
- **R4 같은 분류의 다른 기록·그룹 헤더 건수는 로드분 기준** — 더 불러오기 미완 시 일부만 반영
  (plan 결정 4 수용 범위)
- **R5 초안은 localStorage** — 기기 간 미동기, 브라우저 데이터 삭제 시 소실 (brainstorm 결정 3)
- **R6 목업의 좁은 화면 임계 1080 → 1280 보정** — 앱 사이드바 폭 반영 (work 문서 근거)

## Open Questions / Assumptions

- ~~'약재' 는 '악재' 오타로 가정 (투자 용어 기준). 목업 의도가 별도 개념이면 표기만 교체하면 됨~~
  → **해소**: 태형님이 오타로 확인 (2026-08-21, "오타야"). '악재' 유지 확정, 코드 변경 없음
- 초안 보관을 서버 저장으로 승격할지는 사용 패턴 확인 후 판단 (현재 단일 초안 localStorage)
- 삭제 confirm 은 목업과 달리 유지 (복구 수단 없음)

## Change Summary

- 백엔드: 리스트 API `q`/`keywords`(AND) 파라미터 추가(호환 유지) + `GET /api/news-journal/stats`
  신설. Entity/스키마 변경 없음
- 프론트: 뉴스 기록 화면 전면 교체 — 타임라인·모달 → 마스터-디테일·인라인 편집·초안 보관·
  키워드 필터/패널/관계도·월별/분류별 그룹·증분 로드
- `docs/policies/code-convention.md` 대조: 신규 Java 메서드 10줄 초과 없음(헬퍼 분리),
  중첩 2단계 이내. 프론트 `njSave` 등 검증 메서드는 기존 컴포넌트 관례(순차 guard clause) 유지
