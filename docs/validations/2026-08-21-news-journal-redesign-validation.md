# 뉴스 기록 마스터-디테일 리디자인 Validation

gate: docs/gates/2026-08-21-news-journal-redesign-gates.md

원격 컨테이너에서 실행. 컨테이너에 PostgreSQL 16 이 있어 **로컬 PG 를 직접 기동해
실서버 검증까지** 수행했다 (dev 프로파일, `stocks` DB, ddl-auto=update 로 스키마 생성).

## 실행한 명령과 결과

### 1) 백엔드 빌드/테스트
- `./gradlew compileJava compileTestJava test` — **PASS** (BUILD SUCCESSFUL)
  - 최초 실행 시 `StockMarketApplicationTests`(컨텍스트 로드)가 PG 부재로 실패 →
    로컬 PG 기동 후 재실행 전체 PASS. 컨텍스트 로드가 신규 JPQL 파싱/바인딩 검증을 겸함
  - 외부 API 키는 dummy env 로 주입 (KAKAO/DART/ECOS 등 — 부트 placeholder 해소용)

### 2) 프론트 문법
- `node --check js/components/news-journal.js`, `node --check js/api.js` — **PASS**

### 3) 목 하네스 (Playwright + API 스텁, 사이드 이펙트 없음) — **61항목 PASS / 0 FAIL**
- 마운트·헤더 건수 / 월별 그룹·행 렌더 / 행 선택→디테일(WWH 불릿·기사 host·같은 분류) /
  통합 검색(디바운스·해제) / 임팩트 세그(필터+전체 기준 뱃지 유지) / 분류 드롭다운(검색·선택·다크
  상태) / 기간 지정 / 키워드 AND(2개→1건, 선택 바 문구, 해제) / 키워드 패널(추천 섹션·초성
  ㅂㄷㅊ→반도체·가나다 그룹·ESC 닫기) / 관계도(노드·엣지·짝 클릭→2키워드 필터) / 분류별 그룹 /
  초안 stash·localStorage·이어쓰기 / 새 기록 저장(POST·초안 소멸·목록/통계 갱신) / 수정(PUT) /
  삭제(DELETE·빈 선택 상태) / 빈 결과·필터 초기화 / 더 불러오기(3→6→8·버튼 소멸) /
  좁은 화면 단일 pane(전환·백 버튼) / 콘솔 오류 0건
- CDN 차단 환경이라 Alpine/Chart.js 등은 npm 동일 패키지로 로컬 서빙, Tailwind 는 검증용
  유틸 스텁 — **하네스 산물**: 타 화면 컴포넌트(부동산 `nextScheduledAt`·관심 지표·납입 리마인더)
  오류는 범용 스텁 `{}` 탓 (#114 검증 때와 동일 유형, 본 화면 오류 아님)

### 4) 실서버 API 스모크 (dev :8080 + 로컬 PG + 실제 JWT) — **전 시나리오 기대값 일치**
- 사건 3건 생성(POST 201) 후:
  - `q=물가`(WHY 본문) → 1건 / `q=반도체`(키워드 매칭) → 1건 / `q=투자`(분류명 매칭) → 1건
  - `q=100%` · `q=%` — **LIKE escape 정상** (리터럴 `%` 포함 행만 매칭)
  - `keywords=금리` → 2건, `keywords=금리&keywords=한국은행` → **AND 1건**
  - `GET /stats` → totalCount 3, impactCounts {1,1,1}, categories 건수, keywordEvents 발생일 내림차순
  - `keywords` 11개 → **400** (상한 검증)
- 무인증 `GET /stats` → 500 — **기존 `GET /events` 도 동일 500** (기존 동작, review R1)

### 5) 실서버 프론트 통합 (실제 정적 리소스 + 실 API + 실 PG) — **11항목 PASS / 0 FAIL**
- 실 DB 3건 로드·통계 뱃지 / 서버 q 검색 / 키워드 AND / **UI 생성→실 DB 반영(4건·통계 갱신)** /
  UI 삭제(3건 복귀) / 페이지 오류 0건

### 6) documented workflow harness
- `scripts/check-documented-workflow.sh --base main --through push` — commit/push 문서 작성 후
  최종 실행 (결과는 push 문서에 기록)

## 미검증 항목

- **카카오 로그인 실계정 플로우** — 컨테이너에 OAuth 콜백 불가. JWT 를 직접 서명해 대체
  (필터가 subject 만 principal 로 쓰는 것 확인). 운영 배포 후 실로그인 1회 확인 권장
- **운영 PG 대용량(수천 건) 성능** — q LIKE·stats 는 개인 규모 전제 (review R3)
- **모바일 실기기** — 뷰포트 1100px 시뮬레이션까지만 (좁은 화면 pane 전환 확인)
- 폰트/미세 스타일 실측 대비 — 하네스는 Tailwind 스텁이라 실서버 화면 직접 확인 권장

## 운영 적용 필수 항목

- **없음** — 마이그레이션 없음, 파라미터는 추가 전용, 기존 API 시그니처/응답 무변경
