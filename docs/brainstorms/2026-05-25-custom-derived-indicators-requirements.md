---
date: 2026-05-25
topic: custom-derived-indicators
issue: 36
branch: feat/issue-36-custom-derived-indicators
---

# 사용자 커스텀 파생지표 기능

## Problem Frame

현재 파생지표(장단기 금리차 등)는 프론트엔드(`ecos.js:207-490`)에 **고정 수식**으로 박혀 있어, 운영 중 사용자가 보고 싶은 파생지표를 추가하거나 바꿀 수 없다. 사용자가 경제지표를 조합해 **자신만의 파생지표를 직접 정의·저장**할 수 있게 하여, 코드 배포 없이 관심 지표를 구성하도록 한다. **1차 출시는 국내경제지표(ECOS)로 한정**하고 글로벌경제지표는 후속 단계로 분리한다.

## Requirements

**수식 정의**
- R1. 사용자는 원시 지표와 상수를 사칙연산(+, -, ×, ÷)으로 조합한 수식으로 파생지표를 정의할 수 있다. **1차는 2~3항 제한 구조형**(`term op term (op term)?`, term = 지표 또는 상수)으로 한정한다. 임의 중첩 수식은 후속 확장 여지로 둔다. (예: `A - B`, `(A - B) / C`)
- R2. 수식에 사용 가능한 원시 지표는 **국내경제지표(`EcosIndicator`)에서 실제 호출되는 지표**로 한정한다(1차). 존재하지 않는 지표는 거부한다. 글로벌경제지표(`GlobalIndicator`)는 후속 단계.
- R3. 하나의 수식 안에서는 **같은 카테고리(`EcosIndicatorCategory`) 내** 지표만 조합한다. 카테고리가 다르면(단위/주기 이질) 조합을 금지한다.
- R4. 파생지표는 각 원시 지표의 **최신값 단일 시점**으로만 계산한다. 히스토리/시계열 차트는 비대상.

**사용자별 저장·관리**
- R5. 파생지표는 로그인 사용자별로 저장되며, 본인 것만 조회/수정/삭제할 수 있다. (`Portfolio`의 `assertUserMatches` + 소유권 재검증 패턴 준수)
- R6. 사용자는 파생지표를 생성(Create), 조회(Read), 수정(Update), 삭제(Delete)할 수 있다.
- R7. 파생지표 생성 폼에 **이름 입력 필드와 단위 입력 필드를 분리 제공**하고, 단위는 해당 전용 필드에만 사용자가 직접 입력한다. 두 필드 모두 길이 상한·문자 화이트리스트·출력 이스케이프(저장형 XSS 방지)를 적용한다.

**표시 / UX**
- R8. 기존 경제지표 대시보드의 고정 spread 표시 영역을 **사용자 파생지표 목록 + 생성/관리 UI로 대체**한다.
- R9. 기존 프론트엔드 고정 파생지표(`ecos.js`의 spread 계산 로직)는 제거한다.
- R10. 빈화면 회귀를 막기 위해 **대표 파생지표 프리셋을 필수로 제공**한다. 사용자가 프리셋을 1클릭으로 자신의 파생지표로 추가/복제할 수 있다. (기존 등급/해석 텍스트는 비대상 — 손실 수용)

## Success Criteria

- 사용자가 코드 배포 없이 새 파생지표를 만들고, 대시보드에서 최신값이 정확히 계산되어 표시된다.
- 존재하지 않는 지표·카테고리 불일치·0 나눗셈 같은 잘못된 수식은 생성 시점에 거부된다.
- 사용자 A가 만든 파생지표가 사용자 B에게 노출되지 않는다.
- 출시 직후 신규 사용자도 필수 프리셋(R10)으로 즉시 의미 있는 파생지표를 본다(빈화면 회귀 없음).

## Scope Boundaries

- **글로벌경제지표 1차 제외** (R2). operand 매트릭스(`countryName × indicatorType`) 설계·신규 point-lookup 쿼리가 필요하므로 국내(ECOS) 검증 후 후속 단계로 분리.
- 파생지표 히스토리/추이 차트는 비대상 (R4). 글로벌지표 히스토리 미해결(이슈 #51)에 의존하지 않기 위함.
- 시계열 함수(이동평균, 증감률 등) 비대상. 사칙연산 조합까지만.
- 카테고리 간 혼합 수식 비대상 (R3).
- 기존 고정 파생지표의 등급/색상/해석 텍스트 이전 비대상 (R10, 손실 수용).
- 파생지표 공유/공개(다른 사용자에게 노출) 비대상. 사용자별 비공개만.

## Key Decisions

- **수식 모델 = 2~3항 제한 구조형**: `term op term (op term)?`. 기존 spread 전부 재현 가능한 최소 표현력으로 시작해 AST 파서/검증 부담을 낮춤. 임의 중첩 수식은 수요 확인 후 후속 확장. (scope-guardian·adversarial P2 반영)
- **기존 고정 파생지표 전면 제거 + 필수 프리셋 보존**: 계산 경로를 단일화(프론트 고정식 제거). 빈화면 회귀는 필수 프리셋(R10)으로 완화. 등급/해석 텍스트 손실은 수용.
- **사용자별 커스텀 유지**: "코드 배포 없는 변경"은 운영자 편집형으로도 가능하나, 사용자별 개인화를 목표로 확정 → 신규 `UserDerivedIndicator` Entity + per-user 인가 채택. (product-lens가 제기한 운영자-config 축소안은 기각)
- **그룹 = 카테고리 단위**: `EcosIndicatorCategory` 내에서만 조합 허용. "국내끼리"로 묶으면 단위 이질 산술이 허용되는 문제(adversarial P1) 해소.
- **글로벌 후속 분리**: 1차는 국내(ECOS)만. 글로벌은 전례 0건 + operand 매트릭스 설계 부담으로 후속.
- **최신값 단일 시점**: 구현 단순화 + 글로벌 히스토리 미해결 이슈와의 결합 회피.
- **기존 대시보드 spread 영역 대체**: 별도 화면 신설 없이 기존 진입점 재사용.
- **수식 평가 안전성 (보안 제약, 확정)**: 범용 표현식 엔진(SpEL/ScriptEngine/eval 류) 사용 금지. 사칙연산·괄호·숫자·화이트리스트 지표 토큰만 허용하는 전용 파서/AST 평가기를 사용한다. 입력 길이·노드 수·상수 절대값 상한과 평가 타임아웃을 적용하고, 오버플로/NaN/Infinity 결과는 거부한다.
- **권한 패턴 (보안 제약, 확정)**: `PortfolioController`의 `assertUserMatches(JWT principal vs 요청 userId)` + 서비스 레이어 소유권 재검증 패턴을 채택한다. `UserKeyword` 패턴은 JWT 주체 대조가 없어 IDOR 위험이 있으므로 모방하지 않는다. 단건 조회/수정/삭제는 `(id, jwtUserId)` 동시 매칭으로만 허용하고 불일치 시 403/404.
- **검증 시점 (확정)**: 0 나눗셈·결측·오버플로 검증은 생성 시점뿐 아니라 매 평가(최신값 계산) 시점에도 적용한다. 분모가 지표 참조인 경우 생성 시점 검증만으로 불충분하므로, 평가 시 안전 처리(오류 플래그 반환, 예외 전파 금지)한다.

## Dependencies / Assumptions

- [가정] 본 기능은 로그인 사용자 전용이다. 비로그인 사용자에게는 파생지표 생성/표시를 제공하지 않는다.
- [확정] 빈화면 회귀 완화용 프리셋은 **필수**다(R10). 기존 대표 spread를 프리셋으로 제공.
- [검증됨] 원시 지표 소스(`EcosIndicator`)와 사용자별 저장 패턴(`Portfolio`)은 코드베이스에 존재.
- [검증됨] 백엔드에 `DerivedIndicator` 레코드 + `EcosDerivedIndicatorService`가 이미 존재 → 재사용/대체 여부는 planning에서 판단.

## Outstanding Questions

### Resolve Before Planning
- (없음)

### Deferred to Planning
- [Affects R1][Technical] 2~3항 구조형 수식의 저장 포맷과 파싱/검증 방식. 0 나눗셈·미지원 연산자·오버플로 검증 포함.
- [Affects R5][Technical] `UserDerivedIndicator` 엔티티 스키마 및 매핑. (Entity 신규 생성 → CLAUDE.md 승인 게이트)
- [Affects R6][Technical] 파생지표 CRUD API 엔드포인트 시그니처. (공개 API 추가 → 승인 게이트)
- [Affects R2][Technical] 원시 지표 식별자 체계. ECOS는 `(className, keystatName)` 복합키(한글명) 기반이라 단일 안정 코드가 없음 → 식별/검증 방식 확정 필요.
- [후속 단계][Technical] **글로벌 지표 operand 식별**(1차 제외): `(countryName × indicatorType)` 매트릭스의 operand 설계 + `GlobalIndicatorLatestRepository` point-lookup 신규 쿼리. 글로벌 단계 착수 시 해결.
- [Affects R2][Needs research] 참조 지표 단종/개명 시 저장된 사용자 수식 처리(비활성화/숨김/마지막 값 동결)와 사용자 통지.
- [Affects R8][Design] 수식 빌더 UX: 자유 텍스트 vs 구조화 빌더, 검증 타이밍(실시간 vs 저장 시점)과 에러 표시 위치/문구, 신규 사용자 빈 상태 화면, 원시 지표 결측·계산 실패 시 목록 항목 표시 상태.
- [Affects R9][Needs research] 기존 백엔드 `DerivedIndicator`(transient record)/`EcosDerivedIndicatorService`(하드코딩 계산기) 재사용 가능 범위. 확인 결과 영속/사용자 스코프 미지원 → 신규 구축이 유력하나 지표값 조회·맵 빌드 로직 재사용 여부 판단 필요.
- [Affects R7][Technical] 이름·단위 입력 필드의 구체 검증 규칙(길이 상한값, 허용 문자셋). 단위는 사용자 자유 입력으로 확정.
- [Affects R3][Technical] 외부 경제지표 API를 신뢰 경계로 처리: 응답값 형식/범위 sanity 검증, 불가/이상값 시 정책(거부 권장).

## Next Steps
→ `/ce:plan` for structured implementation planning
