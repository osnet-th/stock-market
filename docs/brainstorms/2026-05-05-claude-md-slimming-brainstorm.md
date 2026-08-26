# CLAUDE.md 슬림화 브레인스토밍

## 배경
- 현재 `CLAUDE.md`는 workflow, context, artifact, scenario 규칙이 여러 섹션에 반복되어 있다.
- 우선순위 체계와 plan 정의가 분산돼 있어 실제 작업 시 해석 비용이 높다.
- 실행 계약 문서 안에 프로젝트 개요와 빌드 정보까지 섞여 있어 하네스 신호가 약해진다.

## 목표
- `CLAUDE.md`를 실행 계약 중심으로 재구성한다.
- 우선순위와 plan 정의를 한 곳에서만 선언한다.
- documented workflow와 lightweight workflow의 차이를 짧고 명확하게 유지한다.
- 저신호 섹션과 반복 규칙을 제거한다.

## 결정
- `CLAUDE.md`는 workflow, approval, context priority, implementation, review, response 중심으로 축소한다.
- 프로젝트 개요와 빌드 정보는 `CLAUDE.md`에서 제거한다.
- policy 문서는 `Context Sources`에만 연결하고, 세부 규칙은 별도 policy 문서에 유지한다.
- `AGENTS.md`, `compound-engineering.local.md`는 `CLAUDE.md`와 충돌하지 않도록 최소 범위만 정리한다.
