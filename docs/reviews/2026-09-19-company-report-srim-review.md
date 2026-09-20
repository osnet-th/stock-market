# 기업 리포트 S-RIM 리뷰

gate: docs/gates/2026-09-19-company-report-srim-gates.md
plan: docs/plans/2026-09-19-001-feat-company-report-srim-plan.md
issue: https://github.com/osnet-th/stock-market/issues/122

## Findings
명시적 findings 없음.

현재 변경 코드의 정적 검토와 아래 제한된 재현 범위에서 수정이 필요한 버그·회귀·설계 위반을 확인하지 못했다. 이는 실제 PostgreSQL/인증 API 통합 검증 완료를 의미하지 않는다.

## 검토 범위와 근거
- domain/model/SrimCalculator.java: 동일한 초과이익을 w=1/0.9/0.8에 적용. 중간 ROE는 DECIMAL128 정밀도를 유지하고 최종 가격만 6자리로 반올림한다. ROE<r 및 음수 결과를 임의로 0 처리하지 않는다.
- domain/model/SrimValidator.java: 0 이하 요구수익률/주식수/공통 자기자본, 평균 자본 0 이하, 중복 연도, 날짜, 숫자 자릿수 제한. 미완성 draft와 최종 저장을 구분한다.
- application/SrimService.java: 저장 때 서버 계산을 수행하고 client result를 입력으로 받지 않는다. 기존 요청 생략/명시 삭제/동일 입력을 구분한다.
- application/dto/SrimInputData.java, SrimDecimalSerializer.java, SrimYearDeserializer.java: decimal plain 문자열로 브라우저 정밀도 유지, 소수 연도 자동 절삭 거부, 30개 행 제한. domain 객체 직접 API 직렬화를 추가하지 않는다.
- infrastructure/persistence/mapper/SrimJsonConverter.java 및 CompanyAnalysisReportMapper.java: 구버전 null 호환, 입력과 결과 재구성, JSON 저장 경계 확인.
- application/CompanyReportWriteService.java: update/refresh/delete는 userId로 소유권 확인. Srim 계산 Controller는 기존 인증 helper를 사용한다.
- infrastructure/persistence/CompanyAnalysisReportJpaRepository.java: refresh 전용 update는 snapshotJson/snapshotAt/updatedAt/stockName만 갱신하고 srim은 갱신하지 않는다. application의 짧은 트랜잭션에서 호출한다.
- static/js/components/company-report.js: 입력의 %/기본 통화 변환, 단위 전환 원값 보존, null과 0 구분, 입력 세대별 늦은 계산 응답 무시, 저장 후 재입력 복원 확인.
- static/partials/company-report.html: 연도별 표, 두 ROE 입력 방식, 비교 기준 주가/통화/일자 명시, 미완료 결과 표시, S-RIM 섹션의 snapshot 독립성을 확인.
- 기존 청산가치/DCF 입력 및 계산기의 의미는 변경하지 않는다.

## Code Convention
- domain은 Java 표준 라이브러리만 사용하며 application→infrastructure 직접 참조를 새로 추가하지 않는다.
- 신규 Entity 연관관계 및 수동 getter/setter 없음.
- 계산·검증은 작은 메서드로 나누고 중첩을 제한했다.
- UI async 요청 상태 전환, 단위 전환의 실패 복구, DTO/Entity 매핑의 길이 예외는 work 문서의 사유를 확인했다. 해당 흐름에서 수정이 필요한 컨벤션 위반은 식별하지 못했다.
- 테스트 파일 미추가는 저장소의 명시 요청 시 작성 정책과 일치한다. 회귀 확인 자체는 validation에서 별도로 수행해야 한다.

## Open Questions / Assumptions
- 지배주주지분·총 주식수는 합의된 1차 수동 입력이며 자동 출처 연동은 범위 밖이다.
- 연도별 결과는 동일 공통 조건에 각 연도의 ROE를 독립 적용한 결과다. 다기간 잔여이익 모델이 아니다.
- 일반적인 동시 리포트 편집의 충돌 감지/버전 관리는 기존 저장 정책을 따른다. 이번 변경의 refresh 쿼리가 srim을 갱신하지 않는 것은 정적으로 확인했으나 DB 동시 실행까지 확인한 것은 아니다.
- 실제 PostgreSQL JSONB bind/저장/부분 갱신 쿼리, 인증/소유권 HTTP 응답, 전체 위저드 임시저장→재개→완료, 기존 Gradle 테스트 실행은 validation 단계에 남아 있다.
- work 브라우저 확인은 실제 UI 코드 + 고정 응답이므로 전체 서버 연결 검증을 대체하지 않는다.

## Review 재현
- bundled Node VM에서 저장된 SrimData.input → _crPopulateForm → _crSrimInput을 비교: JSON 표현까지 일치.
- 같은 상태의 _crBuildBody가 enabled일 때 clearSrim=false, 비활성화하면 srim=null/clearSrim=true를 생성함을 확인.
- git diff --check 성공.
- 기존 work 근거의 27개 Java 검산 및 격리 브라우저 결과를 검토했다. 이번 리뷰에서 전체 테스트를 새로 실행했다고 간주하지 않는다.

## Change Summary
기업 리포트 5단계에 S-RIM 연도별 평가를 추가하고 입력 근거·결과를 독립 JSONB로 보존한다. 리뷰 중 구현 코드는 변경하지 않았다. 다음 단계는 승인 후 validation이다.

---

# 2차 리뷰 (2026-09-20)

태형님 재검토 지시로 1차 결론("명시적 findings 없음")을 전제하지 않고 변경 코드를 다시 읽었다.
1차에서 식별하지 못한 findings 6건을 확인했다. 모두 정적 검토와 코드 추적 근거이며, DB/서버 실행 검증은 여전히 validation 단계에 남아 있다.

## Findings (심각도 순)

### F1. [중간-높음] S-RIM JSON 변환기가 미지 필드를 거부해 schemaVersion 진화 시 목록/상세 조회 전체가 깨진다
- 근거: `SrimJsonConverter.java:11` — `new ObjectMapper()` (Jackson 기본값 `FAIL_ON_UNKNOWN_PROPERTIES = true`).
- 대비: `ReportManualJsonConverter.java:16-17`은 같은 jsonb 페이로드에 대해 명시적으로 `false`를 설정하고 "schemaVersion 진화 시 구버전 행도 읽기 위함"이라고 사유를 남긴다. `ValuationParamsJsonConverter.java:16-17`은 반대로 `true`를 명시한다. 즉 저장소는 두 정책을 모두 *명시*하는데 S-RIM만 암묵 기본값이다.
- 모순: `SrimData`는 진화를 전제로 `schemaVersion`을 보존한다(plan 55행). 그런데 `fromJson`은 `schemaVersion`을 읽지도 분기하지도 않아 버전 필드가 무력하다.
- 파급: `fromJson`은 `CompanyAnalysisReportMapper.java:41`의 `toDomain`에서 호출되고, `toDomain`은 상세뿐 아니라 **목록 조회에도 사용**된다. 스키마 v2 행이 하나라도 있으면 구버전 앱(롤백/카나리)에서 `IllegalStateException`이 나며 해당 리포트가 아니라 **사용자의 리포트 목록 전체**가 실패한다.
- 실패 시나리오: v2에서 `input.taxRate` 추가 → 사용자 A가 v2로 저장 → v1로 롤백 → A의 리포트 목록 500.
- 조치 제안: `FAIL_ON_UNKNOWN_PROPERTIES=false` 명시 + 선택 사유 javadoc, 또는 `schemaVersion` 분기를 실제로 구현.

### F2. [중간] `CompanyAnalysisReport.refreshSnapshot()`이 死코드가 되고 도메인 규칙이 JPQL로 이전됐다
- 근거: `CompanyAnalysisReport.java:93`의 `refreshSnapshot(...)` 호출자가 0이다. `CompanyReportWriteService.java:62`는 동명의 자기 메서드이고 실제로는 `snapshotPersistenceService.replace(...)`를 부른다.
- 규칙 이전: 기존 도메인의 "빈 stockName이면 기존 값 유지"(`CompanyAnalysisReport.java:95-97`)가 이제 두 곳으로 쪼개졌다 — `CompanyReportSnapshotPersistenceService.java:18`의 blank→null 변환 + `CompanyAnalysisReportJpaRepository.java:29`의 `coalesce(:stockName, r.stockName)`.
- 위반: ARCHITECTURE.md의 도메인 규칙 소유 원칙, code-convention.md의 DIP("application, domain 계층이 infrastructure 구체 구현 세부를 직접 알지 않도록 한다"). 한 규칙이 도메인/애플리케이션/인프라 세 곳에 흩어졌고 도메인 사본은 아무도 실행하지 않아 썩는다.
- 조치 제안: 死코드 제거 또는 도메인 메서드를 다시 경유하도록 정리. 어느 쪽이든 규칙의 단일 소유자를 정할 것.

### F3. [중간 · DB 검증 필수 · PLAUSIBLE] `coalesce(:stockName, r.stockName)`에 null 바인딩 시 PostgreSQL 파라미터 타입 추론 실패 가능
- 근거: `CompanyAnalysisReportJpaRepository.java:25-31`. `CompanyReportSnapshotPersistenceService.java:18`이 blank를 **의도적으로 null로 바꿔 전달**하므로 null 바인딩 경로는 실제로 도달한다.
- 위험: JPQL `coalesce`의 null 파라미터는 Hibernate/PostgreSQL에서 `could not determine data type of parameter $N`로 실패하는 알려진 지점이다. Hibernate 6는 보통 반대편 인자에서 타입을 추론하지만 이 쿼리는 **DB에 대해 한 번도 실행된 적이 없다**(validation 미수행).
- 검증 방법: `snapshot.stockName()`이 null/blank인 종목으로 `POST /api/company-reports/{id}/refresh`를 실제 PostgreSQL에서 1회 실행.
- 확정 아님. 정적으로는 판정 불가이며 validation에서 반드시 확인해야 한다.

### F4. [중간] `@Modifying` 쿼리에 `clearAutomatically`/`flushAutomatically`가 없다
- 근거: `CompanyAnalysisReportJpaRepository.java:25`.
- 현재는 잠복: `CompanyReportWriteService.refreshSnapshot`에 `@Transactional`이 없어 조회와 갱신이 서로 다른 영속성 컨텍스트에서 실행된다. 컨트롤러도 갱신 후 `readService.findDetail`로 DB를 다시 읽으므로(`CompanyReportController.java:92-93`) 응답은 정상이다.
- 잠복 위험: `replace()`는 `@Transactional(REQUIRED)`이다. 누군가 `refreshSnapshot`에 `@Transactional`을 붙이거나 상위 트랜잭션에서 감싸는 순간, bulk update가 영속성 컨텍스트를 우회해 이미 로드된 엔티티가 stale로 남고 이후 flush가 **옛 snapshot을 되덮을 수 있다**. S-RIM 보존을 위해 도입한 쿼리가 정반대 사고를 내는 경로다.

### F5. [중간 · 계획 위반] srim 생략 update가 "유지"가 아니라 "재검증 후 거부"가 될 수 있다
- 근거: `SrimService.java:22, 26-29` — `data == null`이면 `validateRetained(previous, draft)`가 **저장된 입력을 현재 draft 플래그로 다시 검증**한다.
- plan 62행: "기존 클라이언트가 둘 다 생략한 update는 저장된 S-RIM을 유지한다."
- 실제: 저장된 S-RIM이 미완성(예: 요구수익률 없음)인 리포트를 `draft=false`로, srim/clearSrim 둘 다 생략하고 저장하면 호출자가 S-RIM을 건드리지 않았는데도 `IllegalArgumentException` → 400.
- 현재 UI에서는 도달 불가다(`company-report.js:875-876`의 `_crBuildBody`가 srim 또는 clearSrim 중 하나를 항상 보낸다). 따라서 사용자 영향은 없고 **API 계약과 plan의 불일치**다.

### F6. [낮음] 시장/통화 판별 규칙이 세 곳에 중복되고, 사본마다 가드가 다르다
- `KrReportSnapshotAssembler.java:84`: `stockCode != null && stockCode.matches("\\d{6}")` (null 가드 있음)
- `SrimService.java:32`: `stockCode.matches("\\d{6}")` (null 가드 없음 → 잠재 NPE. 현재는 `@NotBlank`/`@Pattern`이 막고 있어 잠복)
- `company-report.js:140-143` `crSrimCurrency()`: 종목 코드가 빈 문자열이면 조용히 `'USD'`를 반환한다. 이 값이 서버로 가면 "리포트 통화와 S-RIM 통화가 다릅니다"라는 엉뚱한 메시지로 거부된다.
- code-convention.md OCP/역할 분리 기준상 `isDomestic(stockCode)` 단일 판별기로 모으는 것이 맞다.

### F7. [사소] 잔여물·일관성
- `CompanyReportController.java:37-38`: 연속 빈 줄 2개.
- `SrimInputData.java:22`: `java.util.Objects::isNull` FQCN 인라인 — 파일의 다른 import 스타일과 불일치.
- `company-report.js:158`: `String(year)` — `year`가 이미 문자열이라 이중 변환.
- `company-report.js:156`: 첫 연도 기본값이 `현재연도 + 1`(오늘 기준 2027)이라 당해 연도 예측을 먼저 입력하려면 매번 수정해야 한다. 2200 도달 시 연도가 `''`가 되는데 UI 안내가 없다.
- `company-report.js:249-253` `crSrimNumber`: 정수 문자열은 BigInt로 정확하지만 소수점이 있으면 `Number()`를 거친다. 가격은 항상 scale 6이라 **항상 손실 경로**를 탄다. 2^53(약 9.0e15) 초과에서만 어긋나므로 실사용 영향은 없으나, `SrimDecimalSerializer`가 plain 문자열로 정밀도를 보존한 취지를 표시 단계에서 되돌린다.
- `docs/plans/.2026-09-19-001-feat-company-report-srim-plan.md.swp`: vim 스왑 파일이 untracked로 남아 있다. commit 전 제거 대상.
- `docs/plans/2026-09-19-001-feat-company-report-srim-plan.md:6`: `status: implemented — review 진입 승인 대기`가 게이트(review 완료)와 불일치.

## 재검증으로 "정상" 확인한 항목 (1차 결론 중 유지되는 부분)
- `SrimCalculator.scenario()` 분모 `1 + r - w`는 `r > 0`, `w ≤ 1`이므로 0이 될 수 없다. 0 분모 사고 없음.
- `difference()`의 0 나눗셈 불가 — `referencePrice`는 non-null이면 `> 0`으로 검증된다(`SrimValidator.java:28`).
- `average` 0 나눗셈 불가 — `validateAverage`가 사용 전에 합 `> 0`을 강제한다(`SrimValidator.java:85-88`).
- "동일 입력이면 기존 결과 유지"(`SrimService.java:35`)가 실제로 동작한다. `BigDecimal.equals`는 scale 민감이지만 `SrimInput.normalized()`(`SrimInput.java:28-34`)가 trailing zero와 음수 scale을 정규화하므로 `100`과 `100.00`이 같게 비교된다. 이 정규화가 없었으면 매 저장마다 조용히 재계산됐을 것이다. 미묘하지만 올바르다.
- 지수 폭탄 방어가 유효하다. `SrimInput.java:32`가 `scale < -24`를 `setScale` **이전에** 거부하므로 `1E+999999999` 같은 입력으로 메모리를 터뜨릴 수 없다.
- 단위 전환 왕복(`_crSrimShift`)을 0/8/12 scale, %↔소수, 음수, trailing zero 정리에 대해 손계산으로 검증했다. `crSrimChangeUnit`은 값 계산을 모두 마친 뒤 대입하므로 실패 시 부분 변환이 남지 않는다.
- `crSrimUseReference`가 실재 필드를 가리킨다 — `ReportSnapshot.PriceMetrics.referencePrice/referencePriceDate`(`ReportSnapshot.java:70-71`). 수정 흐름에서도 `_crEnterWizardFrom`이 `cr.preview`를 `detail.snapshot`으로 채우므로(`company-report.js:674-676`) 버튼이 죽지 않는다.
- `_crBuildBody`의 예외가 `_crSave`의 try 안에서 잡혀 `formError`로 표시된다(`company-report.js:706-710`). 숫자 형식 오류로 저장이 조용히 실패하지 않는다.
- 상세 화면의 `input.years[idx]` ↔ `results[idx]` 인덱스 정렬은 서버가 순서대로 매핑하므로 성립한다.
- 새로고침 응답은 DB 재조회 결과다(`CompanyReportController.java:93`). stale 엔티티가 응답에 실리지 않는다.

## Open Questions / Assumptions
- F3은 실제 PostgreSQL 실행 없이는 확정할 수 없다. validation에서 최우선으로 확인해야 한다.
- F1의 정책(미지 필드 거부/허용)은 설계 판단이다. 저장소에 두 선례가 모두 있으므로 태형님 결정이 필요하다. 다만 어느 쪽이든 *명시*가 필요하다는 점은 컨벤션상 분명하다.
- F5는 plan 문구를 고칠지 코드를 고칠지 선택 문제다. 현재 UI 영향은 없다.
- 1차 리뷰가 이 7건을 놓친 이유는 "정적 검토 + 고정 응답 브라우저 확인" 범위에서 계층 간 규칙 이동(F2)과 미래 스키마 진화(F1) 같은 축을 보지 않았기 때문으로 판단한다.

## Change Summary
구현 코드는 이번 리뷰에서도 변경하지 않았다. 1차의 "명시적 findings 없음"은 철회하며, 게이트의 review 결과 문구를 갱신해야 한다.
수정 없이 validation으로 진행할지, F1·F2를 먼저 고칠지는 태형님 결정 대상이다.

---

## 2차 findings 조치 결과 (2026-09-20, 태형님 "전체 수정해줘")

| # | 조치 | 변경 |
|---|---|---|
| F1 | fixed | `SrimJsonConverter`에 `FAIL_ON_UNKNOWN_PROPERTIES=false` 명시 + 정책 javadoc. `ReportManualJsonConverter` 선례를 따른다. |
| F2 | fixed | 死코드 `CompanyAnalysisReport.refreshSnapshot` 제거. 종목명 유지 규칙을 `resolveRefreshedStockName`으로 도메인에 단일 소유. `CompanyReportWriteService`가 이를 경유한다. |
| F3 | fixed | JPQL에서 `coalesce(:stockName, ...)` 제거 → `r.stockName = :stockName`. 도메인이 값을 확정하므로 null 바인딩 경로 자체가 사라졌다. `CompanyReportSnapshotPersistenceService`의 blank→null 변환도 제거. |
| F4 | fixed | `@Modifying(clearAutomatically = true, flushAutomatically = true)`. |
| F5 | fixed | `validateRetained` 제거. srim/clear 생략 요청은 저장된 값을 그대로 반환한다(plan 62행). 입력을 실제로 보낸 완료 저장의 필수값 검증은 유지. |
| F6 | fixed | 신규 `domain/model/StockMarketCode`(`isDomestic`/`currencyOf`)로 판별 규칙 단일화. `SrimService`와 `KrReportSnapshotAssembler.supports`가 위임한다. JS는 언어가 달라 사본이 남지만, 종목 미확정 시 조용한 `'USD'` 대신 `''`를 반환하고 `_crSrimInput`이 "종목을 먼저 선택하세요."로 즉시 막는다. |
| F7 | fixed (일부 보류) | 컨트롤러 빈 줄, `Objects::isNull` import, `String(year)` 이중 변환 정리. 첫 연도 기본값을 당해 연도로 교정(`_crSrimNextYear`). `crSrimNumber`를 문자열 반올림 기반으로 재작성해 `Number()` 정밀도 손실 제거, `crSrimPercent` 추가로 HTML의 `Number(x)*100` 4곳 제거. **보류**: `.swp` 파일은 vim 활성 세션(PID 10473)이 보유 중이라 삭제하지 않았다. 플랜 6행 status도 같은 이유로 태형님께 위임한다. |

### 조치 후 검산
- `./gradlew compileJava --console=plain`: 성공.
- Java 하네스 `SrimFixSmoke` 19건 전부 성공 — 통화 판별(null 포함), 계산 회귀(15,000 / 12,000 / 11,142.857143), 생략 update 유지(동일 인스턴스 반환), 입력 동반 완료 저장의 검증 유지, clear/충돌 거부, 동일 입력 결과 유지, 미지 필드 역직렬화 및 왕복 동일성, 종목명 유지 규칙 3종.
- JS 하네스(JavaScriptCore) 37건 전부 성공 — 단위·퍼센트 자리이동 왕복, HALF_UP 경계, **2^53 초과 금액 보존**(`9007199254740993.4` → `9,007,199,254,740,993`, 구버전은 `...992`로 어긋났다), 음수/널/지수표기 처리, 첫 연도 당해 연도, 통화 미확정 시 throw.
- 이 환경에는 node가 없어 JS는 macOS JavaScriptCore(osascript -l JavaScript)로 실행했다. 구문 검사뿐 아니라 실제 함수 호출 결과를 비교했다.

### 남은 미검증 (validation 단계)
- F3/F4 수정 후의 `updateSnapshot` JPQL을 **실제 PostgreSQL에서 1회 실행**해야 한다. coalesce를 제거해 null 타입 추론 위험은 사라졌지만 쿼리 자체는 여전히 DB 미실행이다.
- 기존 `./gradlew test`, 인증/소유권 HTTP 응답, 전체 위저드 임시저장→재개→완료, 브라우저 실동작은 1차와 동일하게 남아 있다.
