# 용어 사전 마스터-디테일 리디자인 Push 기록

gate: docs/gates/2026-08-21-glossary-redesign-gates.md

Status: Done (2026-08-21)

## 대상

- remote: origin (github.com/osnet-th/stock-market)
- branch: `claude/glossary-ui-backend-update-dnbbk2` (원격 세션 지정 브랜치)
- 명령: `git push -u origin claude/glossary-ui-backend-update-dnbbk2` (네트워크 실패 시 2s/4s/8s/16s 백오프 재시도)

## 의도

- 태형님이 웹/로컬에서 브랜치를 받아 화면·코드를 검토할 수 있게 리디자인 결과를 원격에 반영.
- main 병합/PR 생성은 이 세션 범위 밖 — 태형님 결정 사항.

## 승인

- 태형님 개별 승인 없음 — 원격 자율 세션의 포괄 승인 해석 (게이트 로그 '세션 특성' 참조).
  지정 브랜치 외 push 없음.

## 결과

- push 완료 후 최종 응답에 커밋 해시와 함께 기록. (push 실패 시 본 문서를 갱신하고 재시도 내역을 남긴다)

## 후속

- ~~GitHub Issue 사후 등록 (#117 선례) — 세션 GitHub 자격 복구 시~~
  → **완료: #118** (https://github.com/osnet-th/stock-market/issues/118)
- 운영 DB 는 백업 SQL 수동 적용 필요 (validation 문서 참조)

## 2차 push (post-push 후속, 2026-08-21)

- 대상/명령 동일 (`git push -u origin claude/glossary-ui-backend-update-dnbbk2`)
- 내용: '채움 필요' 판정에 한 줄 정의 포함(`glNeedsFill`) + 후속 문서 갱신 + issue #118 반영
- 승인: 태형님 "진행해봐" 지시 (권장안 회신 후 명시적 진행 승인)

## main 병합 (2026-08-21)

- 승인: 태형님 "main 병합해" 지시
- 방식: `git checkout main && git merge --no-ff claude/glossary-ui-backend-update-dnbbk2`
  → `git push origin main` (선행 #110·#114·salary·#117 과 동일하게 PR 없이 직접 병합)
- merge commit: `6c2ea5c` — 충돌 없음 (26 files, +2102 −492)
- 사전 확인: 병합 트리 == 브랜치 검증 트리 (`git diff` 공백) → 브랜치 검증 결과 그대로 유효
- CI: `Harness Checks` run #102 **success**
- Issue: #118 자동 클로즈 (merge commit body `Closes #118`)

### 배포 상태

`deploy.yml` 은 `on: push: branches: [main]` 이지만 워크플로가 **`disabled_manually`** 상태라
main push 로 배포가 트리거되지 않았다 (#116 push 기록의 정정 내용과 동일 상황).
운영 반영이 필요하면 워크플로를 활성화하거나 서버에서 수동 배포해야 한다.
DB 는 `ddl-auto: update` 환경이면 신규 컬럼/테이블이 자동 생성되고,
수동 관리 DB 라면 `db/migration/glossary_term_detail_fields_2026_08_21.sql` 적용이 필요하다.

