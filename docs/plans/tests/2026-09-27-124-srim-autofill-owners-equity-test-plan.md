# S-RIM 자동 채움 · 지배주주지분 · 주식총수 반기 전환 단위 테스트 계획 (#124)

2026-09-28 태형님 승인.

## 테스트 대상
- `SnapshotFinancialExtractor`: 지배주주 계정 추출, ROE 기준 교체·폴백, S-RIM 근거
- 주식총수 기준 보고서 선택 로직 (신규 순수 클래스, `stock/application`)
- `KrReportSnapshotAssembler`: 반기 주식수 반영, 폴백, schemaVersion
- `ValuationMetricService`: 반기 주식수 전환, 폴백
- `UsSnapshotFinancialExtractor`: 미국 ROE 기준 표기
- `DartFinancialAdapter`: 결산기준일 전달

## 테스트 범위
- 포함: 위 클래스의 상태 기반 단위 테스트
- 제외: 프론트엔드 로직 전체(자동 채움, 단위 환산, 수동 입력 보존, v2 버튼 비활성, 산식 분기, 종목 평가 화면 — `bootRun` 실검증으로 대체), Controller, 챗봇, 통합·E2E
- 메서드 호출 여부 검증은 작성하지 않는다.

## Mock 대상
- 외부 API: DART·SEC 클라이언트
- 기타: 기업코드 캐시, `StockFinancialPort`, 주가 조회 포트, 조립기가 의존하는 application 서비스

## 테스트 케이스

### A. SnapshotFinancialExtractor (Mock 없음, 타임라인 fixture)
| Case | Given | When | Then |
|---|---|---|---|
| A1 정상 | BS 지배주주지분·비지배지분, IS 지배주주순이익이 있는 3개 연도 | 요약 행 생성 | 세 행이 연도별 값으로 생성 |
| A2 정상 | BS·IS에 동일 이름 "지배기업 소유주지분" 계정 | 추출 | 자본 행=BS 값, 순이익 행=IS 값 |
| A3 정상 | 지배주주순이익이 CIS에만 있음 | 추출 | CIS 값으로 추출 |
| A4 정상 | 지배주주 계정 모두 있음 | ROE 계산 | 지배주주순이익 ÷ 지배주주지분, 기준 값=지배주주 |
| A5 예외 | 지배주주 계정 없음(OFS) | ROE 계산 | 당기순이익 ÷ 자본총계, 기준 값=전체 |
| A6 예외 | 지배주주지분만 있고 순이익 없음 | ROE 계산 | 기준 혼합 없이 전체 기준 폴백 |
| A7 정상 | 최신 분기 컬럼에 지배주주지분 있음 | S-RIM 근거 | 최신 컬럼 값·결산일 |
| A8 예외 | 최신 분기 컬럼 값 없음 | S-RIM 근거 | 기준연도 연간 값·12월 말 기준일 |
| A9 정상 | 주식총수 보통주·우선주·합계 행 | S-RIM 근거 / 주가지표 | S-RIM=합계 유통주식수, 시가총액=보통주(기존 유지) |
| A10 예외 | 합계 행 없음 | S-RIM 근거 | 주식수 비움(보통주 대체 없음) |

### B. 주식총수 기준 보고서 선택 (Mock 없음)
| Case | Given | When | Then |
|---|---|---|---|
| B1 정상 | 2026 반기, 2025 사업 | 선택 | (2026, 반기) |
| B2 정상 | 2026 1분기, 2025 사업 | 선택 | (2025, 사업), 분기 제외 |
| B3 정상 | 2025 3분기·반기, 2024 사업 | 선택 | (2025, 반기), (연도, 종료월) 쌍 비교 |
| B4 예외 | 목록 비어 있음 | 선택 | (기준연도, 사업) 폴백 |

### C. KrReportSnapshotAssembler (서비스 Mock)
| Case | Given | When | Then |
|---|---|---|---|
| C1 정상 | 공시 목록에 2026 반기 | 조립 | 반기 주식수·결산기준일 반영, schemaVersion=3 |
| C2 예외 | 공시 목록 조회 예외 | 조립 | 기준연도 사업보고서 주식수 폴백, 스냅샷 정상 생성 |

### D. ValuationMetricService (포트 Mock)
| Case | Given | When | Then |
|---|---|---|---|
| D1 정상 | 반기보고서 존재 | 계산 | EPS·BPS가 반기 유통주식수 기준 |
| D2 예외 | 공시 조회 실패 | 계산 | 사업보고서 주식수로 기존과 같은 결과 |

### E. UsSnapshotFinancialExtractor (fixture)
| Case | Given | When | Then |
|---|---|---|---|
| E1 정상 | 순이익·자본 모두 지배주주 태그 | ROE 계산 | 기준 값=지배주주 |
| E2 예외 | 자본만 비지배 포함 폴백 태그 | ROE 계산 | 기준 혼합 없이 전체 기준 표기 |

### F. DartFinancialAdapter (클라이언트·캐시 Mock)
| Case | Given | When | Then |
|---|---|---|---|
| F1 정상 | 주식총수 응답 결산기준일 2026-06-30 | 조회 | 도메인 모델에 결산기준일 전달 |

정상 17건, 예외 6건.

## 검증 명령
- `./gradlew test --tests "*SnapshotFinancialExtractorTest" --tests "*ShareReportSelectorTest" --tests "*KrReportSnapshotAssemblerTest" --tests "*ValuationMetricServiceTest" --tests "*UsSnapshotFinancialExtractorTest" --tests "*DartFinancialAdapterTest"`
