# Brainstorm: 배포 스크립트 API 트리거 (배포 훅)

## 입력

- issue: TBD (등록 보류 — 태형님 판단)
- worktree: 없음 (main에서 documented 흐름으로 진행)
- source: 태형님 요청 2026-09-27 — "내부서버에서 돌리는데 포트포워딩이 안돼서 수동으로 sh 스크립트를 호출해서 진행한다. 이걸 API 호출해서 실행되도록 하려면 뭐가 젤 빠를까", "내가 주체인데 별도 API 호출하려고"
- 대상 스크립트: [scripts/deploy-on-server.sh](../../scripts/deploy-on-server.sh), [scripts/deploy-hubth-server.sh](../../scripts/deploy-hubth-server.sh)

## 이슈 상세 기반 이해

배포 절차 자체(git pull → gradle build → docker compose up --build → prune → 헬스체크)는 이미 `deploy-on-server.sh`로 확정되어 있다. 이번 작업은 **그 스크립트를 실행시키는 트리거만 교체**하는 것이며, 배포 단계·순서·DB 마이그레이션 정책은 변경하지 않는다.

## 문제 정의

1. **기존 CI 배포는 구조상 동작 불가.** [.github/workflows/deploy.yml](../../.github/workflows/deploy.yml)은 GitHub 클라우드 러너(`ubuntu-latest`)가 `appleboy/ssh-action`으로 내부 서버에 **SSH 인바운드 접속**하는 구조다. 포트포워딩이 없으므로 성립하지 않는다.
2. **결과적으로 배포가 위치에 묶여 있다.** 실제 배포는 태형님이 내부망에서 `deploy-hubth-server.sh`를 수동 실행하는 경로뿐이다. 외부에 있으면 배포할 수 없다.
3. **앱 내부에 배포 API를 둘 수 없다.** 앱 컨테이너(`hubth-app`)가 재빌드·재기동 대상이라 요청 응답 전에 자신이 죽고, 컨테이너 안에는 docker·gradle도 없다.

## 목표

- 태형님이 위치와 무관하게 HTTP 요청 1건으로 배포를 실행한다.
- 인바운드 포트 개방은 0으로 유지한다(기존 cloudflared 아웃바운드 터널 재활용).
- `deploy-on-server.sh`를 그대로 재사용하고 배포 절차는 손대지 않는다.
- 실행 상태와 로그를 같은 API로 확인한다(빌드가 수 분 걸려 요청-응답으로 결과를 받을 수 없음).

## 범위

- cloudflared ingress에 배포 전용 hostname 추가
- 호스트(컨테이너 밖) 훅 리스너 신설 — python3 표준 라이브러리, systemd 상시 가동
- 토큰 인증 + 동시 실행 차단 + 요청 즉시 202 응답 후 detach 실행
- 실행 로그 파일 기록 + 상태/로그 조회 엔드포인트
- `deploy-on-server.sh`의 저장소 밖 복사본 운영 방식 확정
- 동작하지 않는 기존 `deploy.yml` 처리 방향 결정

## 제외 범위

- 앱 내부 배포 엔드포인트, 관리자 화면 배포 버튼 (앱이 재기동 대상이라 불가)
- GitHub self-hosted runner 도입 (저장소가 PUBLIC — fork PR이 홈서버에서 임의 코드를 실행할 수 있음)
- DB 마이그레이션 자동화 (`deploy-on-server.sh` 주석대로 수동 선행 유지)
- 롤백 API, 배포 이력 DB 저장, 알림 연동
- 포트포워딩·공유기·방화벽 설정 변경
- 배포 절차(빌드 단계, 헬스체크 조건) 변경

## 현재 코드 · 환경 확인 완료 지점

### 서버 환경 실측 (2026-09-27)

| 항목 | 값 | 의미 |
|---|---|---|
| OS | Ubuntu 22.04.5 LTS | — |
| python3 | 3.10.12 | 표준 라이브러리만으로 리스너 구현 가능, **추가 설치 0** |
| systemd | 249 | 리스너 상시 가동 유닛 등록 가능 |
| docker0 | 172.17.0.1/16 | cloudflared 컨테이너 → 호스트 리스너 접근 경로 확보 |
| 9000 포트 | 미사용 | 리스너 포트로 사용 가능 |
| `~/deploy-on-server.sh` | **없음** | 현재는 저장소 안 경로만 존재 → 복사본 배치가 필요 |

### 파일 확인

- [docker-compose.yml](../../docker-compose.yml): `app`, `nginx`, `elasticsearch`, `cloudflared` 4개 서비스 모두 `docker compose up -d --build` 대상 → **compose 안에 리스너를 두면 배포 중 자기 자신이 재생성된다.**
- [cloudflared/config.yml](../../cloudflared/config.yml): ingress는 `hubth.com`/`www.hubth.com` → `http://nginx:80`, 나머지는 404. 항목 추가만으로 경로 확장 가능.
- [nginx/default.conf](../../nginx/default.conf): `server_name hubth.com www.hubth.com` 단일 server 블록. nginx도 재생성 대상이므로 상태 조회를 nginx 경유로 두면 배포 중 끊긴다.
- [scripts/deploy-on-server.sh](../../scripts/deploy-on-server.sh): 서버에서 직접 실행 전제로 이미 작성됨. `REMOTE_DIR`, `SKIP_PULL`, `HEALTH_URL` 환경변수 지원, 헬스체크·실패 로그 출력까지 포함 → **훅에서 그대로 호출하면 된다. 스크립트 수정 불필요.**
- 주석에 "스크립트 파일은 아무 데나 둬도 된다"고 명시됨 → 저장소 밖 복사본 실행이 설계 의도와 일치.

## 후보 접근

| 방안 | 트리거 | 소요 | 인바운드 포트 | 리스크 |
|---|---|---|---|---|
| **A. cloudflared 배포 훅** | `curl POST` | 30~40분 | 불필요(기존 터널) | 인증·로그를 직접 구성 |
| B. GitHub self-hosted runner | `gh workflow run` / REST dispatch | 15분 | 불필요(러너 폴링) | **PUBLIC 저장소** → fork PR 임의 코드 실행으로 홈서버 침해 가능 |
| C. 서버 cron 폴링 | 트리거 파일 push | 5분 | 불필요 | 트리거→실행 지연, HTTP 응답·로그 없음, "API 호출" 요건 미충족 |

B는 구현량이 가장 적지만, 저장소가 public이라 홈서버가 걸린 리스크를 감수해야 한다. C는 태형님이 원한 "별도 API 호출" 형태가 아니다.

## 권장 접근

**A안 — 기존 Cloudflare Tunnel에 배포 전용 hostname을 추가하고, 호스트에 훅 리스너를 systemd로 둔다.**

```text
태형님 curl ──HTTPS──▶ Cloudflare edge ──기존 터널(아웃바운드)──▶ cloudflared 컨테이너
                                                                      │ 172.17.0.1:9000
                                                                      ▼
                                                        훅 리스너 (systemd, compose 밖)
                                                                      │ setsid + flock
                                                                      ▼
                                                        ~/deploy-on-server.sh  → 로그 파일
```

API 형태:

```text
POST /deploy   → 202 {"runId": "20260927-153000"}   (즉시 반환, 실행은 백그라운드)
GET  /status   → 실행 중 여부 + 최근 실행 결과 + 로그 tail
```

### 반드시 지켜야 할 제약 4가지

이 4개를 놓치면 배포가 자기 자신을 끊는다.

1. **리스너는 docker compose 밖(systemd)에 둔다.** compose 안에 두면 `docker compose up -d --build` 시점에 리스너 컨테이너가 재생성되며 실행이 중단된다.
2. **nginx를 경유하지 않고 cloudflared → `172.17.0.1:9000` 직결.** nginx도 재생성 대상이라 경유 시 상태 조회가 배포 중 끊긴다.
3. **요청은 즉시 202로 끊고 `setsid`로 detach.** 빌드가 수 분 걸려 Cloudflare 100초 타임아웃에 걸리고, 연결이 끊기면 자식 프로세스까지 죽을 수 있다.
4. **`flock`으로 동시 실행 차단.** 중복 호출 시 gradle 빌드와 compose 재생성이 겹치면 컨테이너 상태가 깨진다. 실행 중이면 409 반환.

### 추가 설계 포인트

- **실행 대상은 저장소 밖 복사본(`~/deploy-on-server.sh`)을 쓴다.** 저장소 안 경로를 실행하면 `git pull`이 실행 중인 스크립트 파일 자체를 교체해 bash가 잘린 내용을 읽을 수 있다. 복사본은 배포 훅이 pull 이후 자동 갱신하지 않고, 스크립트 변경 시 수동 동기화한다(변경 빈도가 낮음).
- 리스너는 python3 `http.server` 표준 라이브러리 단일 파일 — 서버에 추가 패키지 설치가 없다.
- 로그는 `~/deploy-logs/{runId}.log`로 남기고 보존 개수를 제한한다.

## 결정 사항

태형님 확인 완료 (2026-09-27):

- **접근**: A안 채택. B안(self-hosted runner)은 public 저장소 리스크로 배제, C안(cron 폴링)은 요건 불일치로 배제
- **트리거 주체**: 태형님 본인, 앱과 분리된 별도 API (앱 UI 버튼 아님)
- **인증**: **Cloudflare Access 서비스 토큰 + 훅 리스너 자체 Bearer 토큰 두 겹.** Access로 터널 앞단에서 차단하고, 리스너도 독립적으로 토큰을 검증한다
- **hostname**: **`deploy.hubth.com` 서브도메인 추가.** cloudflared ingress에서 호스트 리스너로 직결하고 nginx를 경유하지 않는다. Cloudflare DNS CNAME 등록 필요
- **기존 `deploy.yml`**: **빌드 검증만 남기고 SSH 배포 단계 제거.** push 시 gradle 빌드 검증은 유지한다
- **이슈 등록**: **보류.** `issue: TBD`로 plan을 진행한다. 사유 — 태형님 판단으로 지금은 GitHub 이슈를 만들지 않는다(초안은 이 문서 `등록 예정 이슈 초안`에 보관). 이슈 등록을 보류했으므로 worktree 전환도 하지 않고 `main`에서 documented 흐름으로 진행한다
- **하네스 스크립트 경로 불일치**: 이번 작업의 선행 조건이 **아니다.** 이슈 등록을 보류해 worktree 전환이 없어졌으므로 배포 훅 작업과 무관하다. 착수했던 스크립트 수정은 되돌렸고 별건으로 남긴다
- **실행 방식**: 비동기(202 + 상태 조회), 동기 응답으로 결과를 받지 않음
- **`deploy-on-server.sh`**: 내용 수정 없이 재사용
- **systemd 유닛 등록·시작**: `sudo`가 필요해 에이전트가 실행하지 않는다. 명령을 정리해 제시하고 태형님이 직접 실행한다

## 구현 중 확인된 제약 (범위 밖 · 별건 — 해결됨)

**하네스 스크립트 경로가 현재 머신과 불일치했다.** 아래 스크립트가 `/Users/thlee/Documents/personal/stock-market`을 하드코딩하고 있으나 해당 경로는 이 머신에 존재하지 않는다(현재 저장소는 `/Users/tang/Documents/workspace/stock-market`).

| 스크립트 | 영향 |
|---|---|
| `scripts/start-issue-worktree.sh` | 이슈 착수 Gate 실행 불가 (`Git repository not found`로 종료) |
| `scripts/harness-stage-reminder.sh` | UserPromptSubmit hook의 단계별 의무사항 주입 불가 |
| `scripts/checkpoint-guard.sh` | 체크포인트 가드 동작 불가 |
| `scripts/test-gate-reminder.sh` | plan 디렉토리 탐색 실패 |
| `scripts/validate-plan.sh` | 사용 예시 문구만 해당(인자로 경로를 받아 실제 동작에는 영향 없음) |

또한 `docs/ai/agent-harness.md` Mandatory Workflow 13·14번, `docs/ai/brainstorm-harness.md` 출력 경로 설명, `gates/github-issue-gate.md` 착수 Gate 설명도 같은 경로를 전제한다.

이번 배포 훅 작업의 범위가 아니므로 즉시 수정하지 않았다. **별도 이슈로 분리되어 이미 해결되었다** — 스크립트 5개와 문서 9곳이 `git rev-parse --path-format=absolute --git-common-dir` 기반 자동 판별로 교체되었다.

**남은 누락**: `.claude/settings.json`의 hook command 2곳(`UserPromptSubmit`, `SessionStart`)은 여전히 이전 절대경로를 가리켜 두 hook이 실행되지 않는다. 이 역시 배포 훅 작업 범위가 아니며 별건으로 남긴다.

## 태형님 확인 필요

해소 완료 (2026-09-27):

1. ~~하네스 스크립트 경로 불일치 대응~~ → **이번 작업과 분리.** 이슈 등록 보류로 worktree 전환이 없어져 선행 조건이 아니게 되었다. 착수했던 스크립트 수정은 되돌리고 배포 훅 작업을 진행한다.
2. ~~이슈 제목·본문~~ → **이슈 등록 보류.** 초안은 아래에 보관하고 `issue: TBD`로 진행한다.

## 등록 예정 이슈 초안

**제목**: 내부 서버 배포를 HTTP API로 트리거하는 배포 훅 도입

**본문**:

```text
## 배경

내부 서버는 인바운드 포트 개방이 없어 기존 push 트리거 배포 워크플로가 동작하지 않는다.
현재 배포는 같은 네트워크에서 배포 스크립트를 수동 실행하는 경로뿐이고, 외부에서는 배포할 수 없다.

## 목표

- 위치와 무관하게 HTTP 요청 1건으로 배포를 실행한다.
- 인바운드 포트 개방은 0으로 유지한다(기존 Cloudflare Tunnel 재활용).
- 기존 서버 배포 스크립트를 수정 없이 재사용하고 배포 단계·순서는 바꾸지 않는다.
- 빌드가 수 분 걸리므로 실행 상태와 로그를 같은 API로 조회한다.

## 작업 범위

- Cloudflare Tunnel ingress에 배포 전용 서브도메인 추가
- 호스트(컨테이너 밖) 훅 리스너 신설 — python3 표준 라이브러리, systemd 상시 가동
- Cloudflare Access 서비스 토큰 + 리스너 Bearer 토큰 2단 인증
- 동시 실행 차단, 요청 즉시 202 응답 후 백그라운드 분리 실행
- 실행 로그 기록 및 상태/로그 조회 엔드포인트
- 동작하지 않는 기존 배포 워크플로를 빌드 검증만 남기도록 정리

## 제외 범위

- 앱 내부 배포 엔드포인트, 관리자 화면 배포 버튼 (앱이 재기동 대상이라 불가)
- GitHub self-hosted runner (public 저장소에서 fork PR 임의 코드 실행 위험)
- DB 마이그레이션 자동화, 롤백 API, 배포 이력 저장, 알림 연동
- 포트포워딩·방화벽 설정 변경, 배포 절차 자체 변경

## 완료 조건

- 외부 네트워크에서 HTTP 요청 1건으로 배포가 실행되고 202와 실행 ID를 받는다.
- 인증 없는 요청과 잘못된 토큰 요청이 거부된다.
- 배포 실행 중 중복 요청이 거부된다.
- 배포 중에도 상태 조회가 끊기지 않고, 완료 후 실행 결과와 로그를 조회할 수 있다.
- Entity·DB 스키마 변경이 없다.
```

## /ce:plan 입력 요약

- 확정 범위: cloudflared ingress에 `deploy.hubth.com` 추가, 호스트 훅 리스너(python3 + systemd) 신설, Access 서비스 토큰 + Bearer 2단 인증, 동시 실행 차단, 202 detach 실행, 로그 기록·상태 조회, 배포 스크립트 저장소 밖 복사본 운영, `deploy.yml`을 빌드 검증만 남기도록 정리
- 확정 제외: 앱 내부 엔드포인트, self-hosted runner, DB 마이그레이션 자동화, 롤백, 배포 이력 저장, 알림, 포트포워딩 변경, 배포 절차 변경
- 변경 없음: `deploy-on-server.sh` 내용, 배포 단계·순서, Entity·DB 스키마 (**DB Schema Review Gate 불필요**)
- 선행 조건: 없음. 하네스 스크립트 경로 문제는 별건으로 분리했다
- plan frontmatter: `issue: TBD` (등록 보류), `branch`/`worktree`는 `main` 기준
- 미해결 없음
