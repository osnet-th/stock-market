# 포트폴리오 화면 대시보드형 재설계 Commit 기록 (#110)

gate: docs/gates/2026-08-10-portfolio-dashboard-redesign-gates.md

## 승인

태형님 "적용 했어 main 에 병합해줘" (2026-08-11) + AskUserQuestion 커밋 메시지 확인 → "이대로 진행"

## 커밋

- 브랜치: `feat/issue-110-portfolio-dashboard-redesign`
- 커밋: `63bd7fa`
- 규모: 59 files changed, 3,696 insertions(+), 892 deletions(-)

```
feat(portfolio): #110 포트폴리오 화면 대시보드형 재설계 — 4탭 구조·연금 자산군·자산추이/배당/CAGR/환차손익

- 4탭 재구성(보유 자산/매도 이력/목표 배분/분석) + 탭 partial 4종 분리, 행 액션에 수정 추가·자산군별 노출 규칙 복원
- 뉴스 열람을 키워드 메뉴로 이관, 포트폴리오에는 키워드 등록 모달만 유지(상태 7종·메서드 9종 제거)
- 연금 자산군(AssetType.PENSION) 신설 — 도메인·엔티티·API 2종·마이그레이션, 안전자산 버킷 편입, 원금/평가액 분리
- 신규 집계 4종 — 자산 추이 스냅샷·배당/이자·CAGR/보유일수·환차손익(stock_purchase_history.fx_rate)
- review 반영 — 초기 확장 섹션 키 불일치·재무 패널 차트 누수 수정, 평가 중복 실행 제거(2회→1회), 매수이력 N+1 제거, 환율 통화별 1회 조회
- portfolio_item asset_type CHECK 제약에 PENSION 추가(ddl-auto 미갱신 대상, 운영 적용 필수)

Plan 문서: docs/plans/2026-08-10-001-feat-portfolio-dashboard-redesign-plan.md
```

## 포함 파일

- `docs/` 7건 (brainstorm · issue · plan · work · review · validation · gate)
- 백엔드 신규 14건 / 수정 12건 (portfolio 도메인·application·infrastructure·presentation)
- 마이그레이션 SQL 3건
- 프론트엔드 신규 4건(탭 partial) / 수정 10건
- 테스트 수정 4건 (재구성 생성자 인자 추가)

## 제외 파일

- `.DS_Store`, `.claude/launch.json`, `.codex/`, `scripts/deploy-hubth-server.sh` — 이번 작업과 무관한 기존 untracked 파일
- 민감 파일(.env, credentials 등) 스테이징 여부를 grep 으로 확인 → 없음

## 커밋 전 검증

- `./gradlew compileJava` · `compileTestJava` PASS
- `./gradlew test --tests "*Portfolio*"` PASS (59 tests, 0 failures)
