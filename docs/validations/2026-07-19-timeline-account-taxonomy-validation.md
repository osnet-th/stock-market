# 재무 타임라인 계정 taxonomy 불일치 Validation 기록

work: docs/works/2026-07-19-timeline-account-taxonomy-work.md
review: docs/reviews/2026-07-19-timeline-account-taxonomy-review.md
gate: docs/gates/2026-07-19-timeline-account-taxonomy-gates.md
issue: https://github.com/osnet-th/stock-market/issues/85

## 환경 (태형님 승인 "진행해" — wt-85 재기동)
- wt-84 앱 종료 → wt-85 `SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun` 기동. health 200, 기존 `local-postgres` 연동. dev 사용자(id=1) JWT 주입으로 API 검증.
- `./gradlew compileJava` → exit 0.

## 실행/결과 (preview 스냅샷 API)

### 버그 수정 확인 — 현대차 005380
| 항목 | 2017~2026 | 검증 |
|------|-----------|------|
| netIncome | **전부 채워짐**(이전 2018~2022 누락) | 2018=1.645조·2020=1.925조·2022=7.984조 → DART 정확 일치 |
| operatingCf | **전부 채워짐**(이전 2017·2018 누락) | 2017=3.922조·2018=3.764조 → DART 정확 일치 |
| fcf | **전부 채워짐** | 영업CF 복구로 파생 정상 |
| operatingProfit(영업이익) | **전부 채워짐**(이전 2018·2019 누락 — 범위 확장분) | 2018=2.422조·2019=3.606조 → DART 정확 일치 |
| revenue(매출액) | 전부 채워짐(원래 결측 없음, 방어적 보강 무해) | — |

### 회귀 — 삼성전자 005930
- netIncome 2024=34.5조·2025=45.2조 **기존 값 불변**(putIfAbsent 보존 확인).
- operatingCf 2017=62.2조·2018=67.0조 **채워짐**(삼성도 2017·2018 구 `ifrs_` 접두사 → id 정규화로 개선), 2024=73.0조 그대로.
- operatingProfit 2018=58.9조·2019=27.8조 **불변**(삼성은 원래 완전 → 보강 무해).
- revenue·operatingProfit·netIncome 결측(2017~2025) NONE.

## 미해결/범위 밖 (별개 선행 항목)
- `priceMetrics.eps` 경고 "당기순이익 값을 찾지 못했습니다"는 삼성·현대 **둘 다** 표시되며 출처가 `ValuationMetricService.java:101`(타임라인과 별개 KRX 밸류에이션 컴포넌트). EPS 값 자체는 정상 산출(현대 51,031·삼성 7,757). **#85 범위 밖 선행 이슈** — 필요 시 후속 이슈 후보.

## 정리
- preview 엔드포인트 사용(리포트 영속 없음, 정리 불필요).
- wt-85 앱은 기동 상태로 둠(수정본, 태형님 사용 가능). `local-postgres`·Docker Desktop 유지.
