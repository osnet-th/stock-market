# 기업 리포트 S-RIM 적정주가 구현 계획

gate: docs/gates/2026-09-19-company-report-srim-gates.md
issue: https://github.com/osnet-th/stock-market/issues/122
brainstorm: docs/brainstorms/2026-09-19-company-report-srim-brainstorm.md
status: implemented — review 진입 승인 대기

## 목표와 범위
기업 리포트의 작성/수정 5단계 `기업가치` 및 상세 조회에 선택적 S-RIM 섹션을 추가한다. 공통 조건과 연도별 예상 ROE로 영구 유지/매년 10% 감소/매년 20% 감소 주당 가치를 계산하고 입력 근거와 결과를 리포트에 보존한다. 기존 7단계, 청산가치, DCF, 등급 제안의 의미를 변경하지 않는다.

종목 평가/포트폴리오 연결, 컨센서스 API, 신규 외부 데이터 연동, 과거 리포트 일괄 재계산은 제외한다.

## 조사 근거와 결정
- `company-report.js` wizardSteps: 5단계 기업가치. draftStep은 2~7이므로 새 단계를 추가하지 않는다.
- `CompanyReportWriteService`: create/update/refresh가 분리됨. refresh는 재무 snapshot만 갱신한다.
- `CompanyReportReadService`: 기존 DCF/청산가치를 조회 시 파생 계산한다. S-RIM은 저장 결과를 반환한다.
- `ValuationParams.discountRate`: 기존 DCF용. S-RIM 요구수익률과 독립시킨다.
- `SnapshotFinancialExtractor`: 자본총계 및 distributedStockCount(보통주 우선, 합계 fallback)를 사용한다. 지배주주지분/총 발행주식수와 혼용하지 않는다.
- `UsSnapshotFinancialExtractor`: 자본/주식수 또한 다른 데이터 의미·시점이 있을 수 있다.
- 첫 구현은 지배주주지분·총 주식수도 기준일과 함께 직접 입력한다. 자동 수집 정확성 확보를 본 기능에 끼워 넣지 않는다. 국내/미국 리포트의 통화 표시를 유지하며 입력은 리포트 통화 기준임을 명시한다.
- 기존 주가지표에서 얻는 비교 가격은 출처·기준일과 함께 명시적으로 가져올 수 있다. 불러올 값이 없으면 가격 비교를 생략하며 S-RIM 계산 자체는 허용한다.

## 화면 및 사용자 흐름
1. 5단계에 `S-RIM 적정주가 (선택)` 영역을 추가한다. 미사용 리포트는 그대로 저장 가능하다.
2. 공통 조건: 지배주주지분·자본 기준일, 총 주식수·주식수 기준일, 요구수익률(%). 기준일은 직접 입력한다.
3. 금액은 원/달러와 단위 선택을 명확히 한다. 기존 금액 파싱 도구를 검토하여 재사용하되 저장 값은 기본 통화 단위 decimal로 정규화한다. 단위 변경 시 실질 금액이 바뀌지 않게 한다.
4. `+ 연도 추가`, 행 삭제, 연도 수정. 행별 방식은 `직접 입력` 또는 `ROE 계산`이다.
5. 표 열: 기준 연도 / ROE 입력 방식 / 예상 ROE / 영구 유지 / 매년 10% 감소 / 매년 20% 감소.
6. ROE 계산 행 아래에 전기말 지배주주지분, 당기말 예상 지배주주지분, 당기 예상 지배주주순이익 입력을 펼친다. 전기/당기 연도를 라벨에 표시한다.
7. `계산하기` 또는 `ROE 적용 후 적정주가 계산`은 서버의 동일 계산 로직을 호출한다. ROE용 평균 자본은 S-RIM 공통 자본에 덮어쓰지 않는다.
8. 각 연도는 독립 가정이며 해당 연도의 미래 목표주가나 다기간 이익 누적 계산이 아님을 짧게 안내한다.
9. 입력 변경 시 이전 결과는 현재 입력에 대응하는 결과로 표시하지 않는다. `재계산 필요` 또는 미완료 행 `—`를 표시한다. 늦게 도착한 계산 응답은 입력 세대/요청 식별자로 버린다.
10. 비교 가격은 `비교 기준 주가 (YYYY-MM-DD)`로 표기한다. 저장 결과에는 저장한 가격만 사용하고 최신 현재가로 오인할 문구를 쓰지 않는다. 기준 가격 > 0일 때 `(적정주가 / 기준가격 - 1) × 100`을 표시한다.
11. 상세 조회는 저장 근거·결과·계산 시점을 표시한다. 일반 `데이터 새로고침`은 S-RIM을 변경하지 않는다. 조건 변경/새 가격 반영은 수정 화면에서 명시적으로 반영 후 재계산·저장한다.
12. 좁은 화면에서 표는 가로 스크롤하며 입력 영역은 세로로 배치한다. Alpine.js/Tailwind만 사용한다.

## 계산 및 검증 규칙
- 저장 및 서버 계산의 비율은 소수(12%=0.12), UI 입력은 %이다.
- 평균 지배주주지분 = (전기말 + 당기말 예상 지배주주지분) / 2.
- 예상 ROE = 예상 지배주주순이익 / 평균 지배주주지분.
- 초과이익 = 공통 지배주주지분 × (예상 ROE - 요구수익률).
- 가치(w) = 공통 지배주주지분 + 초과이익 × w / (1 + 요구수익률 - w).
- 적정주가(w) = 가치(w) / 총 주식수. w=1, 0.9, 0.8 고정 순서.
- 순수 Java 계산 컴포넌트를 domain에 배치한다. application은 입력 변환, 저장 및 호출을 조합한다. 프론트에 별도의 금융 계산식을 중복 구현하지 않는다.
- BigDecimal DECIMAL128 등 명시적 정밀도로 계산하고 중간 ROE를 표시용 2자리로 반올림해서 재사용하지 않는다. 최종 저장 주당 가격은 소수 6자리 HALF_UP, 표시만 KRW 0자리/USD 2자리, ROE·비교율은 2자리로 한다. 계산 버전을 보존한다.
- 요구수익률 > 0, 총 주식수는 양의 정수, 공통 자기자본 > 0. ROE 계산의 평균 자본 > 0. 음수 순이익/ROE 및 음수 초과이익은 허용한다.
- 음수 계산 결과는 임의로 0으로 보정하지 않고 산출값과 해석 주의를 함께 표시한다. 시나리오를 가격순으로 재정렬하지 않는다.
- 연도는 1900~2200 정수, 중복 금지, 최대 30개 행. 숫자 입력은 유한 decimal만 허용하고 길이/정밀도 상한을 둔다. 금액/가격은 정수부 24자리·소수부 8자리, 비율은 정수부 6자리·소수부 12자리 이내, 주식수 24자리 이내로 제한한다.
- 빈 값은 null이며 0으로 대체하지 않는다. 0% ROE와 순이익 0은 유효하다. 날짜는 ISO 일자 형식으로 검증한다.
- 직접 입력/계산 방식 전환 시 양쪽 근거값은 보존하되 선택된 방식만 계산에 사용한다.

## 저장 모델과 API
### 독립 저장
- `company_analysis_report`에 nullable JSONB `srim` 컬럼 추가. 기존 리포트는 null이며 기존 valuation_params/snapshot 스키마는 그대로 둔다.
- 저장 payload는 schemaVersion, calculationVersion, input, result, calculatedAt으로 구성한다. input에 공통값·각 기준일·통화·비교가격/일자·연도별 방식/직접 ROE/근거 세 값을 보존한다.
- result에는 연도별 실제 적용 ROE, 평균 자본(계산 방식), 세 시나리오 주당 가격 및 비교율을 보존한다. 클라이언트 결과는 신뢰하거나 저장하지 않는다.
- 날짜/시각 타입 직렬화 모듈을 명시한 전용 JSON converter를 둔다. 저장 DTO와 도메인 객체를 매핑하여 domain 객체의 직접 API/JSON 노출을 추가하지 않는다.
- Entity 및 mapper, domain 리포트의 재구성/생성/수정 경로에 S-RIM을 전달한다. Entity 관계는 추가하지 않는다.
- 멱등 `ADD COLUMN IF NOT EXISTS` 수동 적용 SQL과 롤백 주의(삭제 시 데이터 손실)를 기록한다. 운영 DB 변경은 이 단계에서 실행하지 않는다.

### 요청/응답
- 기존 POST/PUT `/api/company-reports` 요청에 선택적 `srim` 입력과 명시적 `clearSrim` 플래그를 추가한다. 기존 클라이언트가 둘 다 생략한 update는 저장된 S-RIM을 유지한다. clear와 입력을 함께 보내면 오류다.
- Detail에 선택적 S-RIM 결과 DTO 추가. 목록 응답/페이지네이션은 변경하지 않는다.
- 신규 POST `/api/company-reports/srim/calculate`: 로그인 필요, 입력 전용 요청 → 계산 결과 DTO. DB 쓰기와 외부 호출이 없는 미리보기 API. 저장 시에도 같은 서버 계산기를 사용한다.
- 새 입력/변경 입력 저장 시 서버에서 검증·계산한다. 미리보기 결과를 그대로 반송해 저장하지 않는다.
- 기존 입력과 동일한 저장(다른 메모만 수정)은 기존 결과·계산 시점을 유지한다. snapshot refresh 또한 S-RIM을 유지한다.
- draft 저장은 빠진 필드를 허용한다. 누락된 행 결과는 null, 완성된 행은 계산한다. 형식이 틀린 숫자/음수 요구수익률 등은 draft에서도 거부한다.
- 최종 저장은 S-RIM 미사용을 허용하되 활성화된 경우 공통 조건과 최소 1개 연도, 각 행의 선택 방식 필수값이 완성돼야 한다. 빈 연도 행을 삭제할 수 있게 한다.
- create 시 재무 API 실패로 snapshot이 null이어도 사용자 입력 S-RIM을 저장·조회할 수 있도록 UI를 snapshot 유무 조건 밖에 둔다.
- 조회/수정/삭제의 기존 userId 소유권 검증을 유지한다. 계산 API에서도 인증을 수행한다.

## 파일 변경 예상
- domain/model: S-RIM 입력/평가 모델, CompanyAnalysisReport 확장.
- domain의 순수 S-RIM 계산 및 검증 컴포넌트(기존 의존성 방향 유지).
- application: S-RIM 계산 유스케이스/DTO 매핑, CompanyReportWriteService, CompanyReportReadService, CompanyReportCommands/Results.
- presentation: S-RIM 계산 요청 DTO/응답 매핑, 기존 Create/Update 요청 확장 및 CompanyReportController.
- infrastructure/persistence: Entity, mapper, 전용 S-RIM JSON converter.
- resources/db/migration: S-RIM nullable JSONB 추가 SQL.
- static: company-report.html, company-report.js, api.js.
- ARCHITECTURE.md: companyreport의 실제 변경 범위를 필요한 수준만 기록.
- docs/works, reviews, validations, commits, pushes 및 gate: 각 승인된 단계에서 작성.

## 구현 체크리스트
- [x] 순수 계산·검증 모델 구현, 중간 정밀도 유지.
- [x] 선택적 JSON 저장와 기존 리포트/생략 요청 호환성.
- [x] 인증된 계산 API 및 생성/수정/상세 연결.
- [x] 5단계 공통 조건·연도 행·ROE 근거 입력·결과 표.
- [x] 임시저장 재개·완료 저장·수정·데이터 새로고침의 보존 동작.
- [x] 변경된 입력/늦은 계산 응답 처리 및 단위/통화 표시.
- [x] SQL·아키텍처·work 기록 갱신.

## 검증 계획
테스트 파일은 명시 요청이 없으므로 추가하지 않는다. 기존 테스트와 임시 실행 검산으로 검증한다.
- `./gradlew compileJava`, 수정 JS `node --check`(사용 가능 런타임 확인), `git diff --check`.
- 기존 `./gradlew test` 실행. DB/외부 서비스 의존으로 실행 불가하면 원인·미검증 범위를 기록한다.
- 임시 하네스/REPL: 자본 1,000,000,000, 주식수 100,000, ROE 0.12, 요구수익률 0.08에서 w=1/0.9/0.8은 15,000 / 12,000 / 11,142.857143.
- 첨부 예시: 2,098억·2,636억 평균 2,367억, 576억 순이익으로 ROE 약 24.3346%, 표시 24.33%. 계산에는 원정밀도 사용.
- ROE=r이면 모든 가격=자본/주식수, ROE<r이면 감소 시나리오 가격 순서 역전 확인. 0 ROE, 음수 순이익, 결측, 0 분모, 중복 연도·형식 오류 검증.
- JSON 왕복, 구버전 null 리포트, 생략 update의 기존 값 유지, 명시 삭제, 위조 결과 미반영, 수정된 입력 결과 갱신 확인.
- 가능하면 로컬 격리 환경에서 사용자별 접근, DB 저장·재조회, 임시저장 후 재개, snapshot refresh 후 S-RIM 불변, 재무조회 실패 시 수동 계산을 확인한다. 운영 서비스는 검증용으로 변경하지 않는다.
- 브라우저에서 연도 추가/삭제·방식 전환·단위 변경·결과 무효화·늦은 응답·모바일 표 확인. 환경 제한은 미검증으로 기록한다.
- 단계에 맞춰 `scripts/check-worktree.sh --mode documented`와 `scripts/check-documented-workflow.sh --base main --through <stage>` 실행한다.

## work 진입 시 승인 대상
1. 신규 S-RIM 계산 비즈니스 로직 및 기존 저장 동작 확장.
2. 계산 API 추가와 기존 생성/수정/상세 API의 선택적 필드 확장.
3. Entity에 nullable JSONB 컬럼 추가 및 매핑 확장.
4. 자기자본·주식수의 1차 수동 입력, 5단계 배치, 저장 기준 주가 비교 방식.
5. 위 범위의 구현만 승인 대상이며 review/validation/commit/push는 각각 별도 게이트를 따른다.


## 구현 중 구체화 (2026-09-20)
- 재무 새로고침과 리포트 수정이 겹쳐도 S-RIM 보존 요구를 지키기 위해 repository에 재무 컬럼 전용 updateSnapshot을 추가했다. application의 별도 짧은 트랜잭션에서 실행하며 외부 호출은 트랜잭션 밖에 둔다. 새로운 평가 기능이나 화면 확장은 아니다.
- ISO 날짜/시각은 문자열 DTO로 저장한다. Java 시간 객체를 JSON으로 직접 직렬화하지 않으므로 별도 JavaTime 모듈은 필요 없다. 입력 날짜는 LocalDate로 검증하고 계산 시각은 Instant의 ISO 문자열이다.
- API/저장 decimal은 지수 표기 없는 문자열로 직렬화하여 브라우저 큰 금액/작은 비율의 정밀도를 보존한다. 도메인 내부는 BigDecimal이다.
- 미리보기는 임시저장과 같은 부분 입력을 허용하고, 완료 저장에서 필수값을 검증한다.
