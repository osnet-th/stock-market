# 하네스 절대경로 하드코딩 제거 작업 기록

gate: docs/gates/2026-09-27-harness-path-hardcoding-gates.md

## 변경 파일
| 파일 | 변경 |
| --- | --- |
| `scripts/start-issue-worktree.sh` | `ROOT`/`PARENT` 동적 산출 |
| `scripts/harness-stage-reminder.sh` | `_repo_root()` 추가, `ROOT`/`WORKTREE_PARENT` 동적 산출 |
| `scripts/checkpoint-guard.sh` | `ROOT` 동적 산출 |
| `scripts/test-gate-reminder.sh` | `ROOT` 산출 후 `PLANS_DIR` 파생 |
| `scripts/validate-plan.sh` | usage 예시 문자열 상대경로화 |
| `docs/ai/` 8개 | 실행 안내 상대경로화, 절대경로 전제 문장 정정 |
| `docs/analyzes/favorite/2026-04-21-global-favorite-review.md` | 소스 경로 표기 상대경로화 |
| `.gitignore` | `.context/compound-engineering/` 추가 |

## 구현 내용
루트 산출은 셸과 파이썬 모두 `git rev-parse --path-format=absolute --git-common-dir`의 상위 디렉터리를 사용한다. worktree 안에서 실행해도 공용 git 디렉터리는 메인 저장소의 것이므로 항상 메인 루트를 얻는다.

셸 스크립트는 `|| true`로 실패를 흡수해 빈 값이 되게 하고, 이후 기존 가드(`[ ! -d "$ROOT/.git" ]`, `[ -d "$PLANS_DIR" ] || exit 0`)가 그대로 처리한다. 파이썬 hook은 `try/except`로 감싸 매 턴 실행 중 예외가 세션을 막지 않게 했다.

## 작업공간 특이사항
- 대상 스크립트 자체가 고장난 부트스트랩 상황이라 worktree 생성은 동일 절차를 수동 수행했다.
- 착수 시 메인 저장소가 11커밋 뒤처져 있었고, 원격에 이미 병합된 문서 4건의 구버전 사본이 untracked 상태로 남아 `git pull`을 막고 있었다. 내용 대조 후 백업하고 제거해 최신화했다.
- 다른 세션이 메인 저장소 작업트리에서 같은 스크립트를 병행 수정한 사실을 확인해 보고했고, 태형님이 해당 변경을 되돌리고 본 브랜치 구현을 채택하기로 했다.
