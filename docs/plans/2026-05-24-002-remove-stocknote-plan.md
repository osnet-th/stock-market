---
id: 2026-05-24-002
type: plan
title: stocknote 도메인 전면 제거
status: completed
created: 2026-05-24
related:
  - .claude/analyzes/stocknote/2026-05-24-stocknote-deletion-impact.md (Unit 9 에서 삭제됨)
---

# stocknote 도메인 전면 제거 plan

## 배경

투자 노트 기능 운영 종료. 도메인 전체 삭제. 영향도 분석은 `.claude/analyzes/stocknote/2026-05-24-stocknote-deletion-impact.md` 참고.

- 백엔드는 외부 import/주입/이벤트 의존 0건 → 패키지 통째 삭제 가능
- 프론트엔드는 SPA 라우팅, 홈 대시보드 카드, portfolio-deposit-financial 슬롯, api.js 13개 메서드에 묶임
- DB 마이그레이션 0건 (JPA `ddl-auto`). 운영 DB DROP은 별도 작업
- 외부 Java 5개 파일에 stocknote Javadoc 주석 존재(동작 무관)

## 범위 (In Scope)

1. 프론트엔드 진입점/라우팅 제거
2. 홈 대시보드 "투자 노트 카드" 제거
3. `static/js/api.js` 의 stocknote 메서드 블록 제거
4. 프론트엔드 전용 파일 삭제 (`stocknote.html`, `stocknote.js`)
5. 백엔드 `stocknote/` 패키지 통째 삭제

## 범위 결정 (확정)

- **A. 운영 DB DROP** — 본 PR 미포함. 별도 운영자 작업
- **B. `StockPriceService.getDailyHistory()` dead method** — **유지** (건드리지 않음)
- **C. 외부 Java 5개 파일의 stocknote Javadoc 주석 정리** — **포함**
- **D. 직접 stocknote 문서 정리** — **포함** (삭제)
- **E. `.claude/designs/stocknote/`, `.claude/analyzes/stocknote/`** — **포함** (삭제)

## 작업 체크리스트

### Unit 1. 프론트엔드 진입점/라우팅 제거
- [x] `static/index.html` `<div data-partial="stocknote">` 제거
- [x] `static/index.html` `<script src="/js/components/stocknote.js">` 제거
- [x] `static/partials/portfolio-deposit-financial.html` `<div data-partial="stocknote">` 제거
- [x] `static/js/app.js`
  - [x] `validPages` 멤버 `'stocknote'` 제거
  - [x] `menus` 항목 제거
  - [x] `...StocknoteComponent` 스프레드 제거
  - [x] `partialNames` 멤버 `'stocknote'` 제거
  - [x] `cleanupRegistry.stocknote` 엔트리 제거
  - [x] navigateTo cleanup 분기 제거
  - [x] switch case `'stocknote'` 제거

### Unit 2. 홈 대시보드 카드 제거
- [x] `static/js/components/dashboardSummary.js`
  - [x] `note: null` property 제거
  - [x] `loading.note` / `error.note` 초기화 제거
  - [x] `API.getStockNoteList({size:3})` 태스크 블록 제거
  - [x] 헤더 주석 "4개 fetch" → "3개 fetch" 갱신
- [x] `static/partials/home.html`
  - [x] "투자 노트 카드" 블록 제거
  - [x] grid `lg:grid-cols-4` → `lg:grid-cols-3` 조정

### Unit 3. API 클라이언트 정리
- [x] `static/js/api.js` `// ==================== Stock Note ====================` 섹션(12개 메서드) 제거

### Unit 4. 프론트엔드 전용 파일 삭제
- [x] `static/partials/stocknote.html` 삭제
- [x] `static/js/components/stocknote.js` 삭제

### Unit 5. 백엔드 패키지 삭제
- [x] `src/main/java/com/thlee/stock/market/stockmarket/stocknote/` 디렉터리 전체 삭제 (82 파일)

### Unit 6. 빌드 검증
- [x] `./gradlew compileJava` BUILD SUCCESSFUL
- [x] `./gradlew build -x test` BUILD SUCCESSFUL
- [ ] 애플리케이션 기동 smoke (선택)

### Unit 7. (C) 외부 Java 5개 파일의 Javadoc 주석 정리
- [x] `newsjournal/presentation/NewsJournalExceptionHandler.java` stocknote 언급 제거
- [x] `stock/application/StockPriceService.java` stocknote 언급 제거 (메서드 자체는 B 정책 — 유지)
- [x] `stock/domain/model/DailyPrice.java` stocknote 언급 제거
- [x] `stock/domain/service/StockPricePort.java` stocknote 언급 제거
- [x] `stock/infrastructure/stock/kis/config/KisRestClientConfig.java` stocknote 언급 일반화

### Unit 8. (D) 직접 stocknote 문서 정리
- [x] `docs/plans/2026-04-23-001-feat-stock-note-plan.md` 삭제
- [x] `docs/brainstorms/2026-04-23-stock-note-brainstorm.md` 삭제
- [x] `docs/solutions/architecture-patterns/stocknote-chartjs-mixed-line-scatter.md` 삭제
- [x] `docs/solutions/architecture-patterns/external-http-per-item-transaction-isolation-2026-04-26.md` 삭제 (메타 `module: stocknote, scheduler, async`)
- [x] `.claude/designs/stock/kis-restclient-timeout/kis-restclient-timeout.md` — 파일 본질은 KIS 설정이라 보존. stocknote 워커 언급만 일반화

### Unit 9. (E) `.claude/` 산하 stocknote 자료 삭제
- [x] `.claude/designs/stocknote/` 디렉터리 전체 삭제 (13개)
- [x] `.claude/analyzes/stocknote/` 디렉터리 전체 삭제 (11개, 본 plan의 근거 분석 문서 포함)

## 리스크 / 회귀 위험

- 백엔드는 외부 의존 0건이라 컴파일/기동 회귀 위험 낮음
- 프론트엔드 home.html grid 칼럼 조정 시 레이아웃 시각 검증 필요
- 운영 DB DROP 누락 시 고아 테이블 잔존 (별도 작업으로 처리)
- 활성 사용자가 있다면 데이터 손실 — 사전 공지 여부 사용자 확인 필요

## Open Questions

- 브랜치/PR 전략 확정 전 (현재 main 작업트리). 새 브랜치 `chore/remove-stocknote` 권장
