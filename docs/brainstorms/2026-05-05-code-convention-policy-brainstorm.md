# 코드 컨벤션 정책 브레인스토밍

## 배경
- 코드 스타일 규칙을 `CLAUDE.md` 본문에 직접 넣으면 운영 계약과 코드 컨벤션이 함께 흔들린다.
- 메서드 길이, 중첩 제한, SOLID 원칙, 예외 정책, 리뷰 체크리스트는 별도 정책 문서로 분리하는 편이 유지보수와 재사용에 유리하다.

## 목표
- 코드 컨벤션을 별도 문서로 분리한다.
- `CLAUDE.md`, `AGENTS.md`, `compound-engineering.local.md`가 해당 정책을 공식 참조하도록 연결한다.
- 단순 슬로건이 아니라 판단 가능한 규칙과 예외 조건, 리뷰 체크리스트를 포함한다.

## 고려 사항
- 메서드 5줄 규칙은 절대 금지보다 기본 목표와 예외 사유 체계로 쓰는 편이 실용적이다.
- 중첩 제한은 guard clause, helper 추출, 역할 분리 유도용으로 정의해야 한다.
- SOLID는 원칙 이름만 적지 말고 실제 판단 기준으로 풀어써야 한다.

## 결정
- 정책 문서 경로는 `docs/policies/code-convention.md`를 사용한다.
- `CLAUDE.md`는 Context Manifest와 Implementation Contract에서 이 문서를 참조한다.
- `AGENTS.md`와 `compound-engineering.local.md`도 같은 문서를 공식 기준으로 참조한다.
