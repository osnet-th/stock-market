# workflow mode unification

## 작업 리스트
- [x] 모든 작업이 `brainstorm -> plan -> work -> review`를 따르도록 정책 정리
- [x] documented workflow와 lightweight workflow의 차이를 문서 보존 수준으로 정의
- [x] `CLAUDE.md`, `AGENTS.md`, `compound-engineering.local.md`의 충돌 문구 정리
- [x] lightweight 작업 중 documented workflow로 승격하는 조건 명시
- [x] 변경 후 용어와 우선순위 일관성 점검

## 배경
현재 문서는 compound-engineering workflow를 기준으로 정리되어 있지만,
간단한 작업도 같은 흐름으로 처리하되 문서 작성 강도만 다르게 가져가려는 운영 기준이 추가되었습니다.

## 핵심 결정
- workflow는 하나로 유지하고, 산출물 보존 정책만 `documented`와 `lightweight`로 구분합니다.
- documented workflow는 `docs/brainstorms/**`, `docs/plans/**`를 사용합니다.
- lightweight workflow는 같은 순서를 따르되 문서를 파일로 남기지 않을 수 있습니다.
