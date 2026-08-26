# 포트폴리오 그래프 가독성 개선 Work 기록 (#107)

gate: docs/gates/2026-08-02-portfolio-graph-readability-gates.md
plan: docs/plans/2026-08-02-002-feat-portfolio-graph-readability-plan.md

## 구현 요약

plan의 구현 단계 전체 완료. 프론트엔드 2개 파일만 변경 — 서버 API·Entity·비즈니스 로직 무변경.

## 변경 파일

### `static/js/components/portfolio.js`
- `renderDonutChart` — `legend.display: false` (generateLabels 포함 범례 블록 제거). 범례는 우측 "자산 비중" 카드가 대체.
- 편차 표시 헬퍼 신규:
  - `hasDeviation` / `isDeviationZero` (|편차| < 0.05%p → 목표 일치)
  - `getDeviationScale` — `max(밴드×2, ceil(최대|편차%p|×1.1))` 동적 스케일
  - `getDeviationBarStyle` — 중앙 0 기준 다이버징 (부족=왼쪽, 초과=오른쪽)
  - `getDeviationBandStyle` — 허용밴드 중앙 음영
  - `getDeviationBarClass` / `getDeviationTextClass` — 초과=red, 부족=blue, 밴드 내=gray
  - `formatKrwCompact` — 만원/억 축약 (만원 미만은 원 단위)
  - `formatDeviationLabel` — `−17.4%p · 996만원 부족` 행동 언어, 목표 미설정/목표 일치 분기
  - `formatAllocRatioPair` — `47.6% / 65.0%` 보조 표기
  - `getRebalanceSummary` — 밴드 초과 시 "투자자산에서 안전자산으로 약 N 이동 시 목표 도달", 밴드 내면 "안전·투자 비율이 허용밴드 안에 있습니다"
- 교체로 미사용이 된 `formatAllocDeviation`·`getAllocationBucketBarStyle`·`getAllocationTargetMarkerStyle` 제거.

### `static/partials/portfolio.html`
- 도넛 중앙 오버레이 — 고정 `bottom: 48px` 제거, `inset-0` 정중앙 정렬 (범례 제거로 캔버스 중심=원 중심).
- 자산 비중 미니 막대 — 트랙·채움 `rounded-full` → `rounded` (소수% 뭉개짐 해소).
- 목표 자산 배분 카드 재구성:
  - 상단 리밸런싱 요약 배너 (파란 박스 1줄)
  - 버킷 2행·자산군 6행을 동일 다이버징 레이아웃으로 통일 (이름 | 현재/목표% | 편차 막대 | 편차 라벨)
  - "밴드 초과" 배지·현재원/목표원 캡션 제거 (편차 라벨·색으로 대체)
  - 하단 범례 (많음→줄이기 / 적음→채우기 / 허용밴드 음영) + 암호화폐 제외 문구 통합

## 구현 중 결정

- node 미설치 환경이라 `node --check` 대신 브라우저 콘솔 무에러로 문법·런타임 검증 대체 (실제 런타임 기준).
- 8081 검증 시 프론트 로그인 게이트는 localStorage 목 값(`accessToken='dev-harness'`)으로 통과 — dev 프로필 포트폴리오 API는 인증 미강제 확인. 실제 토큰·자격증명 미사용.

## work 단계 검증 (worktree bootRun :8081, 자산 9종 시드)

- 도넛: 차트영역 280×280(기존 158px), 구멍 지름 182px(기존 103px), 중앙 텍스트 113px 수용, 오버레이 중심 오차 X=0px·Y=0px (기존 37px 이탈)
- 편차 막대 8행 실측: 밴드 음영 [43.1%, 56.9%] 중앙 대칭, 모든 막대 50% 기준 정방향, 중앙선 50.1%
- 라벨: `−14.4%p · 773만원 부족` / `+31.5%p · 836만원 많음` / `허용밴드 ±5.0%p` / `배분 제외: 암호화폐 1건` 정상
- 엣지 케이스 (컴포넌트 함수 직접 호출): 편차 0 → "목표 일치"·막대 숨김 / 목표 미설정 → "현재 12.3% · 목표 미설정"·막대 숨김 / 억 단위 → "1억 2,346만원 많음" / 만원 미만 → "4,500원" / 밴드 내 요약 → "안전·투자 비율이 허용밴드 안에 있습니다" / 최소 스케일 = 밴드×2
- 콘솔: portfolio.js 관련 에러 0건 (프로필/관심지표 500은 목 토큰에 의한 기존 인증 API 동작, 본 변경 무관)

## 남은 항목

- validation 단계: 검증 기록 문서화, 미검증 항목(모바일 반응형 실기기, 실제 로그인 세션에서의 확인) 정리

## 범위 확장 — 매도 이력 전체 합계 카드 (2026-08-02, 태형님 요청)

- `portfolio.js` — `getSalesSummary()` 신규: 실입금 합계(Σ saleNetProceeds)·실현손익 합계(Σ saleNetProfit)·실현 수익률(손익합÷원가합, 원가=실입금−손익) 계산. 기존 월별 그룹 헬퍼 재사용.
- `portfolio.html` — 매도 이력 탭 월별 리스트 상단에 합계 카드 1개 (총 매도 건수 / 실입금 합계 / 실현손익 합계(+빨강·−파랑) / 실현 수익률, 2×2→sm 4열 그리드).
- 검증: 시드 2건 기준 "총 매도 2건 · 실입금 2,680,000원 · 실현손익 +360,000원 · 수익률 +15.52%" — 수계산 일치, 콘솔 에러 0건. 원가 0 이하(전액 손실 등) 시 수익률 "—" 표시.
