#!/usr/bin/env python3
#
# 배포 훅 리스너.
#
# 내부 서버에 인바운드 포트를 열지 않고 배포를 트리거하기 위한 최소 HTTP 리스너다.
# 기존 Cloudflare Tunnel(cloudflared 컨테이너)이 이 프로세스로 요청을 넘긴다.
#
# 엔드포인트:
#   POST /deploy  -> 202 {"runId": ...}   배포를 백그라운드로 시작. 이미 실행 중이면 409
#   GET  /status  -> 200                  실행 여부 + 마지막 실행 결과 + 로그 tail
#   그 외          -> 404
#
# 설계 제약(어기면 배포가 자기 자신을 끊는다):
#   1. 이 프로세스는 docker compose 밖에서(systemd) 돌아야 한다. compose 안에 두면
#      `docker compose up -d --build`가 리스너 컨테이너를 재생성하며 실행을 끊는다.
#   2. 배포는 세션을 분리해 띄운다. 요청 연결이 끊겨도 배포는 계속되어야 한다.
#   3. 응답은 배포를 기다리지 않고 즉시 202로 끊는다. 빌드가 수 분 걸려 edge 타임아웃을 넘는다.
#   4. 중복 실행은 flock으로 막는다. 빌드와 컨테이너 재생성이 겹치면 컨테이너 상태가 깨진다.
#
# 환경변수:
#   DEPLOY_HOOK_TOKEN  (필수) Bearer 토큰. 비어 있으면 기동하지 않는다.
#   DEPLOY_SCRIPT      배포 스크립트 경로. 기본 ~/deploy-on-server.sh
#                      저장소 안 경로를 쓰지 않는다. git pull이 실행 중인 스크립트 파일을 교체한다.
#   DEPLOY_LOG_DIR     로그 디렉토리. 기본 ~/deploy-logs
#   BIND_HOST          기본 0.0.0.0 (cloudflared 컨테이너에서 호스트로 들어온다)
#   BIND_PORT          기본 9000
#   LOG_KEEP           보존할 실행 로그 수. 기본 20
#
# 인증은 2단이다. 이 리스너의 Bearer 검증은 2단이며, 1단은 Cloudflare Access가
# 터널 앞단에서 처리한다. 토큰 값은 로그에 남기지 않는다.
import hmac
import json
import os
import shlex
import subprocess
import sys
import time
from datetime import datetime, timezone
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

HOME = os.path.expanduser("~")
TOKEN = os.environ.get("DEPLOY_HOOK_TOKEN", "")
DEPLOY_SCRIPT = os.environ.get("DEPLOY_SCRIPT", os.path.join(HOME, "deploy-on-server.sh"))
LOG_DIR = os.environ.get("DEPLOY_LOG_DIR", os.path.join(HOME, "deploy-logs"))
BIND_HOST = os.environ.get("BIND_HOST", "0.0.0.0")
BIND_PORT = int(os.environ.get("BIND_PORT", "9000"))
LOG_KEEP = int(os.environ.get("LOG_KEEP", "20"))

LOCK_PATH = os.path.join(LOG_DIR, ".deploy.lock")
LAST_RUN_PATH = os.path.join(LOG_DIR, "last-run")
TAIL_LINES = 40


def _now():
    return datetime.now(timezone.utc).astimezone().isoformat(timespec="seconds")


def _log_path(run_id):
    return os.path.join(LOG_DIR, run_id + ".log")


def _rc_path(run_id):
    """종료 코드 파일. 존재하면 그 실행이 끝났다는 뜻이다."""
    return os.path.join(LOG_DIR, run_id + ".rc")


def is_running():
    """배포 락이 잡혀 있으면 실행 중이다.

    잡아보고 곧바로 풀어 확인한다. 이 사이에 다른 요청이 끼어들 수 있지만,
    실제 배포를 띄우는 자식도 flock -n 을 쓰므로 두 번째 배포는 스스로 취소된다.
    """
    import fcntl

    try:
        fd = os.open(LOCK_PATH, os.O_RDWR | os.O_CREAT, 0o600)
    except OSError:
        return False
    try:
        fcntl.flock(fd, fcntl.LOCK_EX | fcntl.LOCK_NB)
        fcntl.flock(fd, fcntl.LOCK_UN)
        return False
    except OSError:
        return True
    finally:
        os.close(fd)


def prune_logs():
    """오래된 실행 로그를 지운다. 최근 LOG_KEEP개만 남긴다."""
    try:
        runs = sorted(f[:-4] for f in os.listdir(LOG_DIR) if f.endswith(".log"))
    except OSError:
        return
    for run_id in runs[:-LOG_KEEP] if len(runs) > LOG_KEEP else []:
        for path in (_log_path(run_id), _rc_path(run_id)):
            try:
                os.remove(path)
            except OSError:
                pass


def read_tail(path, lines=TAIL_LINES):
    try:
        with open(path, "r", encoding="utf-8", errors="replace") as f:
            return [line.rstrip("\n") for line in f.readlines()[-lines:]]
    except OSError:
        return []


def last_run():
    """마지막 실행의 runId·상태·종료 코드. 기록이 없으면 None."""
    try:
        with open(LAST_RUN_PATH, encoding="utf-8") as f:
            run_id = f.read().strip()
    except OSError:
        return None
    if not run_id:
        return None

    info = {"runId": run_id, "startedAt": None, "finishedAt": None, "exitCode": None}
    try:
        info["startedAt"] = _now_from_mtime(_log_path(run_id))
    except OSError:
        pass
    try:
        with open(_rc_path(run_id), encoding="utf-8") as f:
            info["exitCode"] = int(f.read().strip())
        info["finishedAt"] = _now_from_mtime(_rc_path(run_id))
        info["status"] = "succeeded" if info["exitCode"] == 0 else "failed"
    except (OSError, ValueError):
        info["status"] = "running" if is_running() else "unknown"
    return info


def _now_from_mtime(path):
    ts = os.path.getmtime(path)
    return datetime.fromtimestamp(ts, timezone.utc).astimezone().isoformat(timespec="seconds")


def build_runner(run_id):
    """배포를 실제로 돌리는 셸 스크립트.

    flock으로 중복을 막고, 종료 코드를 .rc 파일에 남겨 /status가 완료를 판정할 수 있게 한다.
    """
    q = shlex.quote
    log, rc = q(_log_path(run_id)), q(_rc_path(run_id))
    return "\n".join([
        # flock이 없으면 중복 실행을 막을 수 없다. 락 경합과 구분되도록 따로 보고한다.
        "if ! command -v flock >/dev/null 2>&1; then",
        '  echo "[hook] flock 명령을 찾을 수 없어 중복 실행을 막을 수 없습니다. 배포를 중단합니다." >> ' + log,
        "  echo 127 > " + rc,
        "  exit 127",
        "fi",
        "exec 9>" + q(LOCK_PATH),
        "if ! flock -n 9; then",
        '  echo "[hook] 다른 배포가 실행 중이어서 이 실행은 취소되었습니다." >> ' + log,
        "  echo 9 > " + rc,
        "  exit 9",
        "fi",
        'echo "[hook] 배포 시작 ' + run_id + ' ($(date -Iseconds))" >> ' + log,
        q(DEPLOY_SCRIPT) + " >> " + log + " 2>&1",
        "rc=$?",
        'echo "[hook] 배포 종료 exit=$rc ($(date -Iseconds))" >> ' + log,
        "echo $rc > " + rc,
        "exit $rc",
    ])


def start_deploy():
    """배포를 세션 분리해 띄우고 runId를 돌려준다.

    start_new_session=True가 setsid(2)를 호출하므로 요청 연결이 끊겨도 배포는 계속된다.
    별도 setsid 명령에 의존하지 않는다.
    """
    run_id = time.strftime("%Y%m%d-%H%M%S")
    prune_logs()
    subprocess.Popen(
        ["bash", "-c", build_runner(run_id)],
        stdin=subprocess.DEVNULL,
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL,
        start_new_session=True,
        close_fds=True,
    )
    # 기동에 성공한 실행만 마지막 실행으로 기록한다.
    with open(LAST_RUN_PATH, "w", encoding="utf-8") as f:
        f.write(run_id)
    return run_id


class Handler(BaseHTTPRequestHandler):
    server_version = "deploy-hook/1.0"

    def _send(self, code, payload):
        body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def _authorized(self):
        header = self.headers.get("Authorization", "")
        prefix = "Bearer "
        if not header.startswith(prefix):
            return False
        return hmac.compare_digest(header[len(prefix):].encode("utf-8"), TOKEN.encode("utf-8"))

    def do_POST(self):
        if self.path.rstrip("/") != "/deploy":
            self._send(404, {"error": "not found"})
            return
        if not self._authorized():
            self._send(401, {"error": "unauthorized"})
            return
        if is_running():
            self._send(409, {"error": "deploy already running", "lastRun": last_run()})
            return
        try:
            run_id = start_deploy()
        except OSError as exc:
            sys.stderr.write("[%s] 배포 기동 실패: %s\n" % (_now(), exc))
            self._send(500, {"error": "failed to start deploy"})
            return
        self._send(202, {"runId": run_id, "acceptedAt": _now()})

    def do_GET(self):
        if self.path.rstrip("/") != "/status":
            self._send(404, {"error": "not found"})
            return
        if not self._authorized():
            self._send(401, {"error": "unauthorized"})
            return
        info = last_run()
        self._send(200, {
            "running": is_running(),
            "lastRun": info,
            "logTail": read_tail(_log_path(info["runId"])) if info else [],
        })

    def log_message(self, fmt, *args):
        """토큰이 섞일 수 있는 헤더는 남기지 않는다. 요청 라인과 상태만 남긴다."""
        sys.stderr.write("[%s] %s\n" % (_now(), fmt % args))


def main():
    if not TOKEN:
        sys.stderr.write("DEPLOY_HOOK_TOKEN이 비어 있습니다. 기동을 중단합니다.\n")
        return 1
    if not os.path.isfile(DEPLOY_SCRIPT):
        sys.stderr.write("배포 스크립트를 찾을 수 없습니다: %s\n" % DEPLOY_SCRIPT)
        return 1
    os.makedirs(LOG_DIR, mode=0o700, exist_ok=True)
    server = ThreadingHTTPServer((BIND_HOST, BIND_PORT), Handler)
    sys.stderr.write("[%s] 배포 훅 리스너 시작 %s:%d (script=%s)\n"
                     % (_now(), BIND_HOST, BIND_PORT, DEPLOY_SCRIPT))
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()
    return 0


if __name__ == "__main__":
    sys.exit(main())
