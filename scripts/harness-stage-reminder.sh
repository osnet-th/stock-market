#!/usr/bin/env python3
#
# UserPromptSubmit hook: 진행 중 issue worktree의 현재 단계를 판별해 그 단계의
# 하네스 의무사항을 매 턴 additionalContext로 주입한다. 긴 세션에서 컨텍스트가
# 압축되어도 단계 절차(체크포인트 가드, 리뷰 순서, 검증 질문 등)를 잊지 않게 한다.
#
# 단계 판별:
#   1순위: {worktree}/.claude/issues/{이슈번호}/stage 마커 파일
#           (값: brainstorm | plan | plan-approval | implement | review | explain | verify | pr | merge)
#   2순위: 추론 — plan 없음+브레인스톰 없음→brainstorm, plan 없음→plan,
#           plan draft→plan-approval, plan active→implement, plan done→merge
#
# 노이즈 제어: 상세 블록은 최대 3개(프롬프트에 언급된 이슈 우선, 이후 최근 활동순).
# plan이 draft/active도 아니고 stage 마커도 없는 worktree는 한 줄 요약만.
#
# 관련 정책: docs/ai/agent-harness.md, docs/ai/briefing-harness.md 3-1절
import glob
import json
import os
import re
import subprocess
import sys
import time

ROOT = "/Users/thlee/Documents/personal/stock-market"
WORKTREE_PARENT = "/Users/thlee/Documents/personal"
PLANS_DIR = os.path.join(ROOT, "docs", "plans")
BRAINSTORM_DIR = os.path.join(ROOT, "docs", "brainstorms")
VALID_STAGES = {"brainstorm", "plan", "plan-approval", "implement", "review", "explain", "verify", "pr", "merge"}
MAX_DETAIL = 3


def git(wt, *args, timeout=4):
    try:
        r = subprocess.run(["git", "-C", wt, *args], capture_output=True, text=True, timeout=timeout)
        return r.stdout if r.returncode == 0 else ""
    except Exception:
        return ""


def find_plan(wt):
    """worktree 경로가 일치하는 plan 중 active > draft > 기타 순으로 선택."""
    hits = []
    for p in glob.glob(os.path.join(PLANS_DIR, "*-plan.md")):
        try:
            with open(p, encoding="utf-8") as f:
                head = f.read(4000)
        except Exception:
            continue
        if re.search(rf"^worktree:\s*{re.escape(wt)}\s*$", head, re.M):
            m = re.search(r"^status:\s*(\S+)", head, re.M)
            hits.append((p, m.group(1) if m else ""))
    for want in ("active", "draft"):
        sel = sorted([h for h in hits if h[1] == want], reverse=True)
        if sel:
            return sel[0]
    return sorted(hits, reverse=True)[0] if hits else (None, None)


def read_fm_list(plan_file, key):
    try:
        text = open(plan_file, encoding="utf-8").read()
    except Exception:
        return []
    fm = text.split("---", 2)[1] if text.startswith("---") else text
    m = re.search(rf"^{key}:\s*$(.*?)(?=^\S|\Z)", fm, re.M | re.S)
    if not m:
        return []
    return [x.strip().lstrip("- ").strip() for x in m.group(1).splitlines() if x.strip().startswith("-")]


def path_match(path, entry):
    if entry.endswith("/**"):
        return path.startswith(entry[:-3] + "/") or path == entry[:-3]
    return path == entry or path.startswith(entry.rstrip("/") + "/")


def stage_messages(issue, wt, stage_file):
    """단계별 의무사항. 각 메시지 끝에 다음 단계 전환용 마커 갱신 지시를 포함한다."""
    return {
        "brainstorm": (
            f"Harness Brainstorm 문서(docs/ai/brainstorm-harness.md)를 docs/brainstorms/에 먼저 작성. "
            f"브레인스톰 없이 /ce:plan·plan 초안·구현 진입 금지. 작성 완료·확인 요청 시 Notion 작업 페이지+Brainstorm 페이지 동기화(notion-guide.md). "
            f"작성 완료 시: echo plan > {stage_file}"
        ),
        "plan": (
            f"브레인스톰을 입력으로 /ce:plan 필수(생략 불가) → 구현 계약만 뽑아 root docs/plans plan 초안(status: draft) 작성 → "
            f"scripts/validate-plan.sh 1차 실행. test_plan_status는 태형님과 확정(확정 전 상세 시나리오 선기입 금지). "
            f"승인 전 구현 금지. plan 초안 완료 시: echo plan-approval > {stage_file}"
        ),
        "plan-approval": (
            f"plan 승인 대기 중: 실행 브리핑+plan-to-code 매핑(briefing-harness 1·2절) 제시 여부 확인. "
            f"validate-plan 통과 후 승인 요청 시 Notion Plan 페이지 동기화, active 전환 시 최종본 갱신(notion-guide.md). "
            f"승인 후 active 전환 → check-plan-conflicts.sh → /ce:work로만 구현 시작. 구현 시작 시: echo implement > {stage_file}"
        ),
        "implement": (
            f"/ce:work task 기준, allowed_paths 안에서만, 한 번에 하나의 task. "
            f"큰 작업 단위 완료 시 체크포인트 가드(briefing-harness 3-1): scripts/checkpoint-guard.sh {wt} 실행(스냅샷+경로검사) → "
            f"범위·방향 판정 read-only 서브에이전트 백그라운드 스폰 → 체크포인트 브리핑 후 태형님 확인. "
            f"SCOPE_CREEP·경로 위반은 수신 즉시 중단·보고. 리뷰 진입 시: echo review > {stage_file}"
        ),
        "review": (
            f"리뷰 순서: /ce:review 수행 → actionable issue 표 제시 → 태형님 선택분만 plan 범위 안에서 수정(비선택은 보류 기록). "
            f"리뷰·선택 수정 종료 시 Notion Review 페이지 동기화(notion-guide.md). "
            f"완료 시: echo explain > {stage_file}"
        ),
        "explain": (
            f"구현 이해 확인 Gate: check-implementation-explainer 구조로 구현 흐름·계층별 역할·도메인 정책·유지보수 포인트 + "
            f"요구사항 원장 기준 커버리지(REQ별 판정·위치·근거) 제시. 미충족 1건이라도 있으면 즉시 중단, "
            f"범위 제외·부분 충족은 건별 GAP 결정. 진입 시 Notion Implementation 페이지 생성·동기화(notion-guide.md). "
            f"Gate 완료 시: echo verify > {stage_file}"
        ),
        "verify": (
            f"PR 묻기 전에 '검증은 어떤 걸로 실행할까요?' 필수(정적/문서, ./gradlew test, bootRun+curl 실검증, 미실행 사유 기록) — "
            f"변경 규모 기준 권장안 제시. 결과·미실행 사유는 검증 증거 산출물에 기록. 검증 종료 시: echo pr > {stage_file}"
        ),
        "pr": (
            f"git-pr-gate Implementation PR Gate: plan 체크리스트 갱신 → check-plan-conflicts.sh → "
            f"커밋(Co-Authored-By 등 AI 작성자 정보 금지) → push → gh pr create(본문: 작업 요약+Closes #{issue}, 내부 md 경로 금지). "
            f"완료 시: echo merge > {stage_file}"
        ),
        "merge": (
            f"태형님 병합 승인 확인 전 gh pr merge 금지. Gate 이후 코드 변경 있으면 Notion Implementation 페이지 갱신 → "
            f"plan status done → gh pr merge(기본 squash) → 이슈 #{issue} 자동 close 확인 → "
            f"Final Cleanup(worktree 정리+main 최신화+로컬 브랜치 삭제)."
        ),
    }


def implement_status_lines(wt, issue, plan_file):
    """implement 단계 상세: 스냅샷 최신성 + 빠른 경로 검사."""
    lines = []
    ckpt_dir = os.path.join(wt, ".claude", "issues", issue, "checkpoints")
    snaps = sorted(glob.glob(os.path.join(ckpt_dir, "task-*.diff")), key=os.path.getmtime)
    changed = [l for l in git(wt, "diff", "HEAD", "--name-only", "--", ".", ":(exclude).claude").splitlines() if l.strip()]
    if snaps:
        age_min = int((time.time() - os.path.getmtime(snaps[-1])) / 60)
        lines.append(f"스냅샷: {os.path.basename(snaps[-1])} ({age_min}분 전) / 현재 변경 파일 {len(changed)}개")
    elif changed:
        lines.append(f"스냅샷 없음 / 현재 변경 파일 {len(changed)}개 — 첫 큰 작업 단위 완료 시 checkpoint-guard.sh 실행")
    if plan_file and changed:
        allowed = read_fm_list(plan_file, "allowed_paths")
        blocked = read_fm_list(plan_file, "blocked_paths")
        bad = [p for p in changed if any(path_match(p, e) for e in blocked)]
        bad += [p for p in changed if allowed and not any(path_match(p, e) for e in allowed) and p not in bad]
        if bad:
            shown = ", ".join(bad[:5]) + (f" 외 {len(bad)-5}개" if len(bad) > 5 else "")
            lines.append(f"⚠ plan 경로 밖 변경 감지: {shown} — 즉시 중단하고 태형님에게 보고")
    return lines


def main():
    try:
        prompt = (json.load(sys.stdin) or {}).get("prompt") or ""
    except Exception:
        prompt = ""
    mentioned = set(re.findall(r"#(\d+)", prompt)) | set(re.findall(r"[Ii]ssue[- ](\d+)", prompt))

    worktrees = sorted(d for d in glob.glob(os.path.join(WORKTREE_PARENT, "stock-market-issue-*")) if os.path.isdir(d))
    if not worktrees:
        return 0

    entries = []
    for wt in worktrees:
        m = re.search(r"stock-market-issue-(\d+)$", os.path.basename(wt))
        if not m:
            continue
        issue = m.group(1)
        stage_file = os.path.join(wt, ".claude", "issues", issue, "stage")
        marker = None
        if os.path.isfile(stage_file):
            try:
                v = open(stage_file, encoding="utf-8").read().strip()
                marker = v if v in VALID_STAGES else None
            except Exception:
                pass
        plan_file, plan_status = find_plan(wt)
        if marker:
            stage = marker
        elif plan_status == "active":
            stage = "implement"
        elif plan_status == "draft":
            stage = "plan-approval"
        elif plan_status in ("done", "completed", "blocked"):
            stage = "merge"
        elif glob.glob(os.path.join(BRAINSTORM_DIR, f"*issue-{issue}*brainstorm*.md")):
            stage = "plan"
        else:
            stage = "brainstorm"

        recency = max(
            [os.path.getmtime(p) for p in (stage_file, plan_file) if p and os.path.exists(p)]
            + [os.path.getmtime(s) for s in glob.glob(os.path.join(wt, ".claude", "issues", issue, "checkpoints", "task-*.diff"))]
            + [0.0]
        )
        detail_ok = bool(marker) or plan_status in ("active", "draft") or issue in mentioned
        entries.append({
            "wt": wt, "issue": issue, "stage": stage, "stage_file": stage_file,
            "plan_file": plan_file, "plan_status": plan_status or "없음",
            "recency": recency, "detail_ok": detail_ok, "mentioned": issue in mentioned,
        })

    detail = sorted([e for e in entries if e["detail_ok"]],
                    key=lambda e: (not e["mentioned"], -e["recency"]))[:MAX_DETAIL]
    rest = [e for e in entries if e not in detail]

    blocks = []
    for e in detail:
        msg = stage_messages(e["issue"], e["wt"], e["stage_file"]).get(e["stage"], "")
        lines = [f"◆ #{e['issue']} ({os.path.basename(e['wt'])}) — 단계: {e['stage']} (plan: {e['plan_status']})", msg]
        if e["stage"] == "implement":
            lines += implement_status_lines(e["wt"], e["issue"], e["plan_file"])
        blocks.append("\n".join(l for l in lines if l))

    if rest:
        blocks.append("기타 worktree: " + ", ".join(f"#{e['issue']}({e['stage']})" for e in rest))

    if not blocks:
        return 0

    msg = "[단계 하네스 리마인더] 아래 진행 중 작업의 현재 단계 절차를 지켜라. 단계가 실제와 다르면 stage 마커 파일을 먼저 갱신하라.\n" + "\n\n".join(blocks)
    print(json.dumps({
        "hookSpecificOutput": {
            "hookEventName": "UserPromptSubmit",
            "additionalContext": msg,
        }
    }, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    sys.exit(main())
