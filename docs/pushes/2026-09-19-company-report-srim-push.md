# 기업 리포트 S-RIM 푸시·병합 기록

gate: docs/gates/2026-09-19-company-report-srim-gates.md
issue: https://github.com/osnet-th/stock-market/issues/122

승인: 태형님 "됐어 커밋하고 main 에 병합해줘" + AskUserQuestion "전부 진행".

## 브랜치 푸시
- remote/branch: `origin` / `codex/issue-122-company-report-srim`
- upstream 신규 설정 (`push -u`). 기존 원격 브랜치 없음.
- 결과: `[new branch]` 생성 성공.

## main 병합
- worktree에서는 main을 checkout할 수 없어 main 워크트리(`/Users/tang/Documents/workspace/stock-market`)에서 수행했다.
- 병합 전 로컬 main이 origin/main보다 **22커밋 뒤처져** 있어 `git pull origin main`으로 최신화(`255b405`).
- 사전 충돌 분석: origin/main이 base 이후 144개 파일을 바꿨으나 우리 변경과 겹치는 것은 `src/main/resources/static/js/api.js` 1건뿐이었고, origin/main은 Company Report 구역을 건드리지 않아 자동 병합을 예상했다. main 워크트리의 `.gitignore` 로컬 수정도 origin/main과 겹치지 않아 pull이 안전했다.
- 결과: 충돌 없음. 병합 커밋 `6c682b0`.

## 병합 후 검증 (푸시 전)
우리 브랜치가 22커밋 이전 main에서 갈라져 나왔으므로 텍스트 병합 성공만으로는 부족하다고 보고 실행했다.
- `./gradlew compileJava` — 성공.
- `./gradlew test` — **클래스 24 / 테스트 134건 / 실패 0 / 오류 0**. main이 새로 추가한 테스트 17건을 포함해 전부 통과.

## main 푸시
- `git push origin main` → `255b405..6c682b0` 성공.
- 푸시 후 `main...origin/main` 차이 0/0 확인.

## 남은 사항
- 로컬 dev 서버가 터미널 탭 c1에서 계속 실행 중이다(포트 8080). 필요 없으면 Ctrl-C로 종료.
- 마이그레이션 SQL `company_report_srim_2026_09_19.sql`은 운영 DB에 적용하지 않았다. 배포 시 별도 적용이 필요하다. (로컬 dev DB에는 ddl-auto가 컬럼을 생성했다.)
- Issue #122 클로즈는 태형님 확인 후 진행한다.
