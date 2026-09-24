# 기업 리포트 S-RIM 구현 결과

gate: docs/gates/2026-09-19-company-report-srim-gates.md
plan: docs/plans/2026-09-19-001-feat-company-report-srim-plan.md
issue: https://github.com/osnet-th/stock-market/issues/122

## 구현
- 기업 리포트 5단계와 상세 화면에 선택적 S-RIM 표 추가.
- 공통 지배주주지분/총 주식수/요구수익률 및 기준일 직접 입력, 비교 기준 주가 선택 입력.
- 연도 추가/삭제, ROE 직접 입력/근거 세 값 계산, 영구/10%/20% 시나리오.
- 원·억·조(또는 달러 배수) 입력 단위 전환. 가격과 주식수는 별도 기본 단위 유지.
- 서버 BigDecimal 계산기와 인증된 POST /api/company-reports/srim/calculate.
- nullable JSONB srim에 입력/근거/결과/버전/시각 저장. 전용 serializer로 큰 decimal 정밀도와 plain 표기 보존.
- 기존 리포트 null 호환, 생략 update 유지, 명시 삭제, 동일 입력 결과 유지.
- 임시저장 부분 입력 허용, 완료 저장 필수값 검증, 음수 순이익/ROE 허용.
- 입력 변경 시 결과 무효화 및 늦은 응답 무시. 실제 데이터와 무관한 계산 결과를 표시하지 않는다.
- 재무 새로고침은 재무 컬럼만 갱신하여 S-RIM을 보존한다. SQL은 작성만 했으며 운영 DB에 실행하지 않았다.

## 변경 파일 묶음
- domain: SrimInput/Valuation/Calculator/Validator, CompanyAnalysisReport 및 repository 포트.
- application: SrimService, SrimData/InputData/DecimalSerializer/YearDeserializer, 기존 쓰기/조회 DTO·서비스, CompanyReportSnapshotPersistenceService.
- persistence/presentation: Entity·Mapper·JSON converter·재무 갱신 쿼리, 기존 요청 DTO 및 Controller.
- frontend: company-report.html, company-report.js, api.js.
- SQL: src/main/resources/db/migration/company_report_srim_2026_09_19.sql.
- 문서: ARCHITECTURE.md 및 본 작업 workflow 문서.

## work 단계 확인
- ./gradlew compileJava --console=plain: 성공.
- bundled Node --check company-report.js 및 api.js: 성공.
- 임시 /tmp/SrimSmoke.java: 27개 확인 성공. 수식, 첨부 ROE 예시, ROE=r/ROE<r, 0 분모 거부, JSON/Entity 왕복, null/생략/동일 입력/삭제 보존, 통화 검증, 부분 입력, plain 작은 비율, 소수 연도 거부.
- /tmp/srim-browser.cjs: 실제 변경 HTML/JS와 고정 응답을 사용한 격리 Chrome 확인 성공. 단위 전환, 계산 방식, 근거값 변환, 오래된 응답 무시, 모바일 가로 스크롤 및 상세 결과. JS 오류 없음.
- 데스크톱·모바일 스크린샷 육안 확인. 정식 서버/DB를 띄운 end-to-end 검증은 아니다.
- git diff --check: 성공.
- 새 테스트 파일은 저장소에 추가하지 않았다. 임시 검산은 계획된 work 확인 범위에서 수행했다.

## 구현 선택/컨벤션 예외
- UI의 async 계산 메서드는 로딩/오류/응답 세대 처리를 한 흐름으로 유지하여 10줄을 초과한다. 단위 전환은 실패 시 원래 상태 보존을 위한 try/catch를 한 메서드에 유지했다.
- 기존 생성/수정 orchestration 및 DTO/Entity 생성자 매핑은 기존 패턴을 유지하고 관련 필드만 확장했다. 새로운 domain 계산·검증은 작은 메서드로 분리했다.
- 저장 날짜/시각은 ISO 문자열로 사용한다. JSON 자체에 시간 객체를 저장하지 않아 시간 모듈 추가는 불필요하다.

## 다음 단계 및 미검증
- review 진입 승인 대기. 정식 review/validation/commit/push는 진행하지 않았다.
- validation에서 기존 Gradle 테스트, 실제 DB JSONB 저장/재조회·부분 갱신 JPQL 실행, 인증/사용자 소유권 API, 전체 위저드 임시저장→재개→완료 흐름을 확인해야 한다.
- 브라우저 work 확인은 S-RIM 섹션과 고정 응답에 한정된다. 전체 앱에서 외부 재무 API를 사용하는 통합 흐름은 미검증이다.

---

# 2차 리뷰 반영 수정 (2026-09-20)

태형님 "전체 수정해줘" 승인으로 2차 리뷰 findings 7건을 모두 반영했다. 기능 범위는 넓히지 않았고 plan의 목표/제외 범위는 그대로다.

## 변경 파일
- 신규 `domain/model/StockMarketCode.java`: 시장/통화 판별 단일 출처. (F6)
- `domain/model/CompanyAnalysisReport.java`: 死코드 `refreshSnapshot` 제거, `resolveRefreshedStockName` 추가. (F2)
- `application/SrimService.java`: `validateRetained` 제거, 통화 판별 위임. (F5, F6)
- `application/CompanyReportWriteService.java`: 종목명을 도메인 규칙으로 확정 후 전달. (F2)
- `application/CompanyReportSnapshotPersistenceService.java`: blank→null 변환 제거. (F3)
- `application/KrReportSnapshotAssembler.java`: `supports`가 `StockMarketCode.isDomestic`에 위임. (F6)
- `application/dto/SrimInputData.java`: `Objects` import. (F7)
- `infrastructure/persistence/CompanyAnalysisReportJpaRepository.java`: `coalesce` 제거, `clearAutomatically`/`flushAutomatically` 추가. (F3, F4)
- `infrastructure/persistence/mapper/SrimJsonConverter.java`: 미지 필드 허용 명시 + 정책 javadoc. (F1)
- `presentation/CompanyReportController.java`: 연속 빈 줄 정리. (F7)
- `static/js/components/company-report.js`: `crSrimCurrency` 빈 코드 처리 + `crSrimCurrencyLabel`, `_crSrimNextYear`, `crSrimNumber` 문자열 반올림 재작성, `_crSrimFormat`/`_crSrimRoundHalfUp`/`crSrimPercent` 추가, `_crSrimInput` 종목 미선택 가드. (F6, F7)
- `static/partials/company-report.html`: `crSrimPercent`/`crSrimCurrencyLabel` 연결, `Number(x)*100` 4곳 제거. (F7)

## 설계 판단 (태형님 확인 필요 항목 포함)
- F1의 미지 필드 정책은 `ReportManualJsonConverter`(허용) 선례를 택했다. `SrimData`가 `schemaVersion`을 진화 목적으로 보존하고, 이 변환기가 목록 조회 경로에서도 호출되어 한 행의 실패가 목록 전체를 막기 때문이다. `ValuationParamsJsonConverter`(거부) 선례를 원하시면 되돌릴 수 있다.
- F2에서 공개 도메인 메서드 `refreshSnapshot`을 제거하고 `resolveRefreshedStockName`을 추가했다. 공개 시그니처 변경이므로 기록해 둔다. 호출자가 0이었고 규칙이 인프라로 새어 있던 상태를 되돌린 것이다.
- F6의 `StockMarketCode`는 신규 공개 domain 클래스다. 판별 규칙이 Java 2곳 + JS 1곳에 중복돼 사본마다 가드가 달랐던 문제를 해소한다.
- JS의 판별 사본은 언어 경계상 제거할 수 없어 남긴다. 대신 잘못된 통화를 조용히 보내지 않도록 빈 문자열 + 즉시 오류로 바꿨다.

## 검산
- `./gradlew compileJava --console=plain`: 성공.
- Java 하네스 19건, JS 하네스 37건 전부 성공. 상세는 review 문서의 "조치 후 검산" 참조.
- `git diff --check`: 성공.
- 새 테스트 파일은 저장소에 추가하지 않았다(명시 요청 시 작성 정책 유지). 하네스는 scratchpad에만 두었다.

## 보류 항목
- `docs/plans/.2026-09-19-001-feat-company-report-srim-plan.md.swp`: vim 활성 세션(PID 10473)이 플랜 파일을 열고 있어 삭제하지 않았다. 잔여물이 아니다.
- 플랜 6행 `status: implemented — review 진입 승인 대기`: 같은 이유로 수정하지 않았다. vim 세션 종료 후 태형님이 갱신하거나, 닫으신 뒤 지시해 주시면 반영한다.
