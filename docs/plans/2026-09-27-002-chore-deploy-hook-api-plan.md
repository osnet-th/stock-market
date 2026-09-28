---
issue: TBD
status: active
branch: chore/deploy-hook-api
worktree: /Users/tang/Documents/workspace/stock-market
test_plan_status: none
schema_plan_status: none
allowed_paths:
  - scripts/deploy-hook-listener.py
  - scripts/deploy-hook.service
  - cloudflared/config.yml
  - docker-compose.yml
  - .github/workflows/deploy.yml
  - docs/plans/2026-09-27-002-chore-deploy-hook-api-plan.md
  - docs/brainstorms/2026-09-27-deploy-hook-api-brainstorm.md
blocked_paths:
  - src/**
  - nginx/default.conf
  - scripts/deploy-on-server.sh
  - scripts/deploy-hubth-server.sh
  - build.gradle
  - .env
---

# 배포 훅 API 도입 — 내부 서버 배포를 HTTP 요청으로 트리거

## 배경 / 현재 상태 / 문제점

내부 서버는 인바운드 포트 개방이 없다. 그래서 세 가지가 동시에 성립한다.

1. **기존 CI 배포가 구조상 동작하지 않는다.** `.github/workflows/deploy.yml`은 GitHub 클라우드 러너가 `appleboy/ssh-action`으로 서버에 SSH 인바운드 접속하는 구조다. 포트 개방이 없어 성립할 수 없다.
2. **배포가 위치에 묶여 있다.** 실제 배포 경로는 태형님이 내부망에서 `scripts/deploy-hubth-server.sh`를 수동 실행하는 것뿐이다. 외부에서는 배포할 수 없다.
3. **앱 내부에 배포 API를 둘 수 없다.** `docker-compose.yml`의 `app` 컨테이너가 재빌드·재기동 대상이라 응답 전에 자신이 죽고, 컨테이너 안에는 docker·gradle도 없다.

반면 아웃바운드 터널은 이미 있다. `cloudflared/config.yml`이 `hubth.com`/`www.hubth.com`을 `nginx:80`으로 넘기고 있어, 포트 개방 없이 외부 HTTP를 받을 통로가 확보되어 있다. 이 통로에 배포 전용 경로를 더하는 것이 이번 작업이다.

배포 절차 자체는 `scripts/deploy-on-server.sh`로 이미 확정되어 있다(git pull → gradle build → compose up --build → prune → 헬스체크). 이 스크립트는 서버에서 직접 실행하는 전제로 작성되어 있고 `REMOTE_DIR`·`SKIP_PULL`·`HEALTH_URL`을 지원한다. **내용을 수정하지 않고 그대로 호출한다.**

### 서버 환경 실측 (2026-09-27)

| 항목 | 값 | 판단 |
|---|---|---|
| OS | Ubuntu 22.04.5 LTS | — |
| python3 | 3.10.12 | 표준 라이브러리로 리스너 구현, 추가 패키지 설치 없음 |
| systemd | 249 | 리스너 상시 가동 유닛 등록 가능 |
| 9000 포트 | 미사용 | 리스너 포트로 사용 |
| 홈 디렉토리 배포 스크립트 복사본 | 없음 | 배치 필요 |

## 목표

- 위치와 무관하게 HTTP 요청 1건으로 배포를 실행한다.
- 인바운드 포트 개방은 0으로 유지한다.
- 빌드가 수 분 걸리므로 실행 상태와 로그를 같은 API로 조회한다.
- 배포 단계·순서는 바꾸지 않는다.

## 설계

### 구성

```text
태형님 curl ──HTTPS──▶ Cloudflare edge(Access 검증) ──기존 터널──▶ cloudflared 컨테이너
                                                                        │ host.docker.internal:9000
                                                                        ▼
                                                    deploy-hook-listener.py (systemd, compose 밖)
                                                                        │ Bearer 검증 → flock → 세션 분리
                                                                        ▼
                                                        ~/deploy-on-server.sh → ~/deploy-logs/{runId}.log
```

### 반드시 지킬 제약 4가지

이 4개를 놓치면 배포가 자기 자신을 끊는다.

1. **리스너는 compose 밖(systemd)에 둔다.** compose 안에 두면 `docker compose up -d --build` 시점에 리스너 컨테이너가 재생성되며 실행이 중단된다.
2. **nginx를 경유하지 않는다.** nginx도 재생성 대상이라 경유 시 배포 중 상태 조회가 끊긴다. cloudflared에서 호스트로 직결한다.
3. **요청은 즉시 202로 끊고 배포를 새 세션으로 분리 실행한다.** 빌드 시간이 edge 타임아웃을 넘고, 연결이 끊기면 자식 프로세스까지 죽을 수 있다. 구현은 `start_new_session=True`(setsid(2) 호출)를 쓰고 외부 `setsid` 명령에 의존하지 않는다.
4. **`flock`으로 중복 실행을 막는다.** 빌드와 컨테이너 재생성이 겹치면 상태가 깨진다. 실행 중이면 409를 반환한다.

### API 계약

| 메서드 | 경로 | 응답 | 비고 |
|---|---|---|---|
| POST | `/deploy` | `202 {"runId": "..."}` | 실행은 백그라운드. 중복 시 `409`, 인증 실패 시 `401` |
| GET | `/status` | `200 {"running": bool, "lastRun": {...}, "logTail": [...]}` | 배포 중에도 응답해야 한다 |

그 외 경로와 메서드는 `404`로 응답한다.

### 인증 (2단)

- **1단 — Cloudflare Access 서비스 토큰**: `CF-Access-Client-Id`/`CF-Access-Client-Secret` 헤더를 edge에서 검증한다. 리스너가 인터넷에 직접 노출되지 않는다. Zero Trust 대시보드 설정은 태형님이 수행한다.
- **2단 — 리스너 Bearer 토큰**: 리스너가 `Authorization: Bearer ...`를 독립 검증한다. 토큰은 서버의 `~/.deploy-hook.env`(권한 0600)에서 읽는다. **토큰 값은 저장소·plan·로그·커밋에 넣지 않는다.**

### 파일 배치

| 파일 | 위치 | 이유 |
|---|---|---|
| `scripts/deploy-hook-listener.py` | 저장소 + 서버 홈으로 복사 | 버전 관리하되 실행은 저장소 밖 복사본 |
| `scripts/deploy-hook.service` | 저장소 + 서버 `/etc/systemd/system/`으로 복사 | 동일 |
| `~/deploy-on-server.sh` | 서버 홈 (저장소 밖 복사본) | 저장소 안 경로를 실행하면 `git pull`이 실행 중인 스크립트 파일을 교체해 bash가 잘린 내용을 읽을 수 있다 |
| `~/.deploy-hook.env` | 서버 홈, 0600 | 토큰 보관 |

복사본은 자동 갱신하지 않고 스크립트 변경 시 수동 동기화한다(변경 빈도가 낮다).

### 도달 경로 (실측 확정)

서버 실측 결과로 확정했다.

| 항목 | 실측값 | 의미 |
|---|---|---|
| docker 버전 | 28.4.0 | `host-gateway` 지원(20.10+) |
| `cloudflared` 네트워크 | compose 전용 네트워크, 게이트웨이 `172.20.0.1` | 기본 브리지가 아니므로 `docker0` IP를 그대로 쓸 수 없다 |
| `app`의 `host.docker.internal` | `172.17.0.1` | `host-gateway`는 **기본 브리지(docker0) IP**로 해석된다 |
| `cloudflared` 이미지 | 셸·`cat` 없음 | 컨테이너 내부에서 도달성을 직접 확인할 수 없다. 실제 HTTP 요청으로만 검증 가능 |

확정 사항:

- `cloudflared`에 `extra_hosts: host.docker.internal:host-gateway`를 추가하고 ingress는 `http://host.docker.internal:9000`을 쓴다. 게이트웨이 IP를 직접 박으면 네트워크 재생성 시 깨진다.
- **리스너 바인딩은 `BIND_HOST=172.17.0.1`을 권장한다.** `0.0.0.0`이면 LAN 전체에 열려 Bearer 토큰만이 방어선이 된다. 브리지 IP에만 바인딩하면 LAN에서는 접근할 수 없다.
- ~~브리지 간 경로가 호스트 방화벽에 막힐 가능성~~ → **해소 (2026-09-28 실측).** `172.17.0.1`에 바인딩한 리스너에 `app` 컨테이너(같은 compose 네트워크, 같은 `extra_hosts`)에서 `host.docker.internal`로 요청해 도달을 확인했다. `cloudflared`도 같은 경로이므로 `BIND_HOST=172.17.0.1`로 확정한다.

## 작업 리스트

- [x] 1. `scripts/deploy-hook-listener.py` 작성 — python3 표준 라이브러리, Bearer 검증 · flock 중복 차단 · 새 세션 분리 실행 · 202 즉시 응답 · `/status` 조회 · 로그 파일 기록(최근 20개 유지)
- [x] 2. `scripts/deploy-hook.service` 작성 — systemd 유닛(`Restart=always`, 실행 사용자 지정, EnvironmentFile로 토큰 주입)
- [x] 3. `cloudflared` → 호스트 리스너 도달 경로 실측 확인 후 `docker-compose.yml`의 `cloudflared`에 `extra_hosts` 적용
- [x] 4. `cloudflared/config.yml` ingress에 `deploy.hubth.com` → 리스너 항목 추가 (nginx 경유하지 않음)
- [x] 5. `.github/workflows/deploy.yml` 정리 — SSH 배포 단계 제거, push 시 gradle 빌드 검증만 유지
- [x] 6. 서버 반영 절차 정리 — 태형님이 직접 실행할 명령 목록 작성(아래 `서버 반영 절차`)
- [ ] 7. 검증 — 인증 실패 거부, 중복 요청 거부, 배포 실행·완료, 배포 중 상태 조회 유지
  - [x] 7-1. 서버 실환경 리스너 검증 (2026-09-28, 가짜 배포 스크립트, 임시 포트 19000) — 401·404·202·**409**·상태 조회·완료 판정·토큰 로그 미노출·컨테이너→호스트 도달 전부 통과. 테스트 산출물 정리 완료
  - [ ] 7-2. Cloudflare Access 검증 — 토큰 없음 403 ✓ (2026-09-28), 서비스 토큰 통과 ✓ (서버 ingress 미반영으로 404, 예상된 상태)
  - [ ] 7-3. 외부 → edge → 터널 → 리스너 종단 검증 (PR 병합·배포·systemd 등록 후)

## 요구사항 원장

| ID | 요구사항 | 출처 | 이번 범위 | 근거 |
|---|---|---|---|---|
| REQ-1 | 외부에서 HTTP 요청 1건으로 배포 실행, 202와 실행 ID 반환 | 태형님 요청 | 포함 | — |
| REQ-2 | 인바운드 포트 개방 없이 동작 | 태형님 요청 | 포함 | 기존 터널 재활용 |
| REQ-3 | Access 서비스 토큰 + Bearer 2단 인증, 무인증·오토큰 거부 | brainstorm 확인 1 | 포함 | — |
| REQ-4 | 배포 전용 서브도메인으로 수신, nginx 경유하지 않음 | brainstorm 확인 2 | 포함 | — |
| REQ-5 | 중복 실행 거부 | brainstorm 권장 접근 | 포함 | — |
| REQ-6 | 배포 중에도 상태 조회 유지, 완료 후 결과·로그 조회 | 태형님 요청 | 포함 | — |
| REQ-7 | 기존 배포 워크플로를 빌드 검증만 남기도록 정리 | brainstorm 확인 4 | 포함 | — |
| REQ-8 | 배포 스크립트 내용 무수정 재사용 | brainstorm 결정 | 포함 | — |
| REQ-9 | 앱 내부 배포 엔드포인트 / 관리자 화면 버튼 | brainstorm 제외 범위 | 제외 | 앱이 재기동 대상이라 불가 |
| REQ-10 | GitHub self-hosted runner | brainstorm 제외 범위 | 제외 | public 저장소에서 fork PR 임의 코드 실행 위험 |
| REQ-11 | 롤백 API · 배포 이력 저장 · 알림 연동 | brainstorm 제외 범위 | 제외 | 별건 |
| REQ-12 | 포트포워딩·방화벽 변경, 배포 절차 변경 | brainstorm 제외 범위 | 제외 | 이번 목표가 트리거 교체로 한정됨 |

## 수정 가능 범위

- `scripts/deploy-hook-listener.py` (신규), `scripts/deploy-hook.service` (신규)
- `cloudflared/config.yml` — ingress 항목 추가
- `docker-compose.yml` — `cloudflared` 서비스에 `extra_hosts`만 추가
- `.github/workflows/deploy.yml` — SSH 단계 제거
- 이 plan 문서와 대응 brainstorm 문서

## 수정 금지 범위

- `src/**` 애플리케이션 코드 일절. 이번 작업은 앱 코드를 건드리지 않는다
- `nginx/default.conf` — 훅이 nginx를 경유하지 않으므로 수정하지 않는다
- `scripts/deploy-on-server.sh`, `scripts/deploy-hubth-server.sh` — 내용 변경 없이 재사용한다. **커밋에도 포함하지 않는다**(태형님 결정: `deploy-hubth-server.sh`에 서버 내부 IP·사용자명이 들어 있어 public 저장소에 올리지 않는다). 서버에는 `scp`로 배치한다
- `.env`, `build.gradle`
- 하네스 스크립트(`scripts/harness-*.sh`, `scripts/start-issue-worktree.sh` 등) — 경로 불일치 문제는 별건이며 이번 범위가 아니다

## 주의사항

- 단위테스트 대상이 없다. Java 코드 변경이 없고 산출물이 인프라 스크립트·설정이라 `test_plan_status: none`으로 둔다.
- Entity·DB 스키마 변경이 없다. DB 마이그레이션은 이번 범위에서 제외한다(`deploy-on-server.sh` 주석대로 스키마 변경이 있는 배포는 수동 선행).
- 토큰 값은 어떤 산출물에도 기록하지 않는다. 서버 `~/.deploy-hook.env`에만 둔다.
- 저장소가 public이므로 커밋·PR 본문에 서버 IP·내부 경로·사용자명·토큰을 넣지 않는다.
- 이 엔드포인트는 사실상 서버 원격 코드 실행이다. 인증 검증은 구현 검증 항목에서 반드시 실측한다.

## 서버 반영 절차 (태형님 직접 실행)

에이전트는 서버에 파일을 배치하거나 systemd 유닛을 등록·시작하지 않는다. 아래 순서로 실행한다.
모든 명령은 서버 사용자 홈 기준이며 사용자명·호스트를 문서에 적지 않는다(`$HOME`, `$USER` 사용).

### 0. 부트스트랩 순서 주의

배포 훅이 아직 없으므로 **이번 반영만은 기존 방식으로 한다.** 로컬에서:

```bash
scripts/deploy-hubth-server.sh
```

이 시점에 ingress와 `extra_hosts` 변경이 서버에 반영된다. 리스너가 아직 없어 배포 훅 도메인은
502를 내지만 기존 서비스에는 영향이 없다.

### 1. 토큰과 환경 파일 (서버에서)

토큰 값은 화면에 출력하지 않는다. 필요할 때 파일에서 읽는다.

```bash
umask 077
printf 'DEPLOY_HOOK_TOKEN=%s\n' "$(python3 -c 'import secrets; print(secrets.token_urlsafe(48))')" > ~/.deploy-hook.env
cat >> ~/.deploy-hook.env <<EOF
DEPLOY_SCRIPT=$HOME/deploy-on-server.sh
DEPLOY_LOG_DIR=$HOME/deploy-logs
BIND_HOST=172.17.0.1
BIND_PORT=9000
LOG_KEEP=20
EOF
chmod 600 ~/.deploy-hook.env
```

호출할 때 토큰이 필요하면 `grep DEPLOY_HOOK_TOKEN ~/.deploy-hook.env`로 확인한다.

### 2. 리스너와 배포 스크립트 복사본 배치

저장소 안 경로를 직접 실행하지 않는다. `git pull`이 실행 중인 스크립트 파일을 교체한다.

`deploy-on-server.sh`는 저장소에 커밋하지 않는다(태형님 결정). 서버 저장소에 없으므로 **로컬에서 `scp`로 보낸다.**

> **완료 (2026-09-28)** — 태형님 지시로 에이전트가 실행했다. 서버 홈에 배치, 실행 권한 확인, 로컬과 SHA-256 일치, `bash -n` 문법 통과. 스크립트 자체는 실행하지 않았다(배포 미트리거).
호스트·사용자는 `scripts/deploy-hubth-server.sh`의 값과 같다.

```bash
# 로컬에서
scp -P <SSH_PORT> -i ~/.ssh/hubth-server-deploy -o IdentitiesOnly=yes \
  scripts/deploy-on-server.sh <SSH_USER>@<SSH_HOST>:~/deploy-on-server.sh
```

리스너는 저장소에 커밋되므로 서버 저장소에서 복사한다.

```bash
# 서버에서
cp "$HOME/hubth-server/scripts/deploy-hook-listener.py" ~/deploy-hook-listener.py
chmod +x ~/deploy-hook-listener.py ~/deploy-on-server.sh
```

### 3. systemd 유닛 설치 (서버에서, sudo)

유닛은 템플릿이므로 사용자·홈 경로를 치환해 설치한다.

```bash
sed -e "s|__DEPLOY_USER__|$USER|g" -e "s|__DEPLOY_HOME__|$HOME|g" \
  "$HOME/hubth-server/scripts/deploy-hook.service" \
  | sudo tee /etc/systemd/system/deploy-hook.service >/dev/null
sudo systemd-analyze verify /etc/systemd/system/deploy-hook.service
sudo systemctl daemon-reload
sudo systemctl enable --now deploy-hook
systemctl status deploy-hook --no-pager
```

`systemd-analyze verify`가 경고 없이 끝나야 한다. 기동 실패 시 `journalctl -u deploy-hook -n 50`으로 확인한다.
토큰 파일이 없거나 배포 스크립트 경로가 틀리면 리스너가 스스로 종료하며 그 이유를 로그에 남긴다.

### 4. Cloudflare Zero Trust — Access 애플리케이션과 서비스 토큰 (대시보드)

1. Zero Trust > Access > Service Auth에서 서비스 토큰을 생성한다. Client ID와 Client Secret은 **생성 직후 한 번만** 표시되므로 그때 보관한다.
2. Zero Trust > Access > Applications > Add an application > Self-hosted를 만든다.
   - Application domain: 배포 훅 서브도메인
   - Policy: Action `Service Auth`, Include에 위에서 만든 Service Token
3. 이 정책이 있어야 edge에서 무인증 요청이 차단된다. 리스너의 Bearer 검증은 그 뒤의 2단이다.

### 5. DNS 레코드 (둘 중 하나)

대시보드: DNS > Records에 CNAME `deploy` → `<터널 UUID>.cfargotunnel.com`, Proxied 켜기.
터널 UUID는 `cloudflared/config.yml`의 credentials 파일명에 있는 UUID다.

또는 서버에서 컨테이너로:

```bash
docker run --rm --user "$(id -u):$(id -g)" \
  -v "$HOME/.cloudflared:/etc/cloudflared:ro" \
  cloudflare/cloudflared:latest \
  tunnel --origincert /etc/cloudflared/cert.pem route dns hubth-server deploy.hubth.com
```

`~/.cloudflared`가 `0700`이라 이미지 기본 사용자(`nonroot`)로는 읽을 수 없다. `--user`로 서버 사용자 권한을 쓰고 인증서 경로는 `--origincert`로 명시한다.
Access 애플리케이션을 만들어도 DNS 레코드는 생기지 않으므로 이 단계는 별도로 필요하다.

### 6. ingress 변경 반영 (서버에서)

0단계에서 이미 반영되었으면 건너뛴다.

```bash
cd "$HOME/hubth-server" && docker compose up -d cloudflared
```

### 7. 바인딩이 막힐 경우 (실측으로 해당 없음 확인)

컨테이너(`172.20.0.x`)에서 호스트 `172.17.0.1`로 가는 경로가 방화벽에 막히면 훅이 502를 낸다.
그때는 `~/.deploy-hook.env`의 `BIND_HOST`를 `0.0.0.0`으로 바꾸고 `sudo systemctl restart deploy-hook`한다.
이 경우 LAN에서도 접근 가능해지므로 Bearer 토큰이 유일한 방어선이 된다는 점을 감안한다.
