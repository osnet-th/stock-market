# 포트폴리오 화면 대시보드형 재설계 Validation (#110)

gate: docs/gates/2026-08-10-portfolio-dashboard-redesign-gates.md
review: docs/reviews/2026-08-10-portfolio-dashboard-redesign-review.md

## 실행 환경

- 빌드: `./gradlew bootJar -x test`
- 기동: `set -a; source .env; set +a` → `DATABASE_URL` 의 `host.docker.internal` → `localhost` 치환 → `SPRING_PROFILES_ACTIVE=dev SERVER_PORT=8082 java -jar build/libs/stock-market-0.0.1-SNAPSHOT.jar`
- DB: 로컬 도커 Postgres 16 (`local-postgres`, `stocks`)
- 기동 결과: `Started StockMarketApplication in 4.166 seconds`, `/` 200

## 실행한 명령과 결과

| 명령 | 결과 |
|---|---|
| `./gradlew compileJava` | PASS |
| `./gradlew compileTestJava` | PASS |
| `./gradlew test --tests "*Portfolio*"` | PASS (59 tests, 0 failures, 0 errors) |
| `./gradlew bootJar -x test` | PASS (112MB) |
| 앱 기동 (dev, :8082) | PASS |

### 스키마 자동 생성 (`ddl-auto: update`)

기동 로그와 실제 DB 양쪽에서 확인.

| 대상 | 결과 |
|---|---|
| `pension_detail` 테이블 | 생성됨 (PK `id`, FK → `portfolio_item(id)`) |
| `portfolio_snapshot` 테이블 | 생성됨 (PK, `uk_portfolio_snapshot_user_date` UNIQUE, `idx_portfolio_snapshot_user_id`) |
| `stock_purchase_history.fx_rate` | `numeric(12,4)` 컬럼 추가됨 |

주의: FK 이름은 SQL 에 적은 `fk_pension_detail_item` 이 아니라 Hibernate 자동 생성명(`fk25vbbjmyho6bjtxa28r1surve`)으로 만들어졌다. 이름 관리가 필요하면 SQL 선적용이 필요하다.

### 신규 API 실호출

| 요청 | 결과 |
|---|---|
| `GET /api/portfolio/summary?userId=1` | 200 — `totalEvaluated 59,977,635 / totalInvested 58,190,000 / profit 1,787,635 / profitRate 3.07 / holdingDays 154 / cagr 7.44 / fxProfit 0 / fxUnknownCount 2` + `income{monthAmount 82,972.47, yearEstimate 995,669.58, dividendYield 1.12, basis ESTIMATED_MONTHLY_AVERAGE, excludedCount 2}` |
| `GET /api/portfolio/snapshots?months=12` | 200 — 최초 `[]` |
| `POST /api/portfolio/snapshots` | 200 — `2026-08-11` 1건 생성 |
| 같은 날 `POST` 재호출 | 200 — 건수 여전히 1건 (덮어쓰기 동작 확인) |
| `POST /api/portfolio/items/pension` | **최초 409 → 원인 조치 후 200** (아래 V1) |
| `PUT /api/portfolio/items/pension/{id}` | 200 — subType·provider·evaluatedAmount·월납입액·납입일 모두 갱신 |
| `POST /api/portfolio/items/{id}/deposits` | 200 — 연금이 납입 대상으로 정상 동작 |

### 도메인 동작 확인

- **연금 평가액 분리**: 원금 1,000만 / 평가 1,150만 등록 → 총평가 `59,977,635 → 71,477,635`(+11.5M), 총원금 `58,190,000 → 68,190,000`(+10M), 손익 `+1,787,635 → +3,287,635`(+1.5M). 평가액이 원금이 아닌 별도 값으로 반영됨
- **배분 편입**: `/allocation/status` 에서 안전자산 56.51% / 투자자산 43.49% — 연금이 안전자산 버킷에 집계됨

### 프론트 실서버 확인 (`http://localhost:8082/#portfolio`)

- 자산군 그룹 11종 렌더 (`STOCK_KR`·`STOCK_OVERSEAS`·`BOND`·`REAL_ESTATE`·`FUND`·`OTHER`·`CRYPTO`·`GOLD`·`COMMODITY`·`PENSION`·`CASH`)
- KPI: `총 자산 7,198만원` / `총 평가손익 +339만원 · 투자원금 6,859만원 · 환차손익 +0원 · 환율 미기록 2건 제외` / `누적 수익률 +4.94% · CAGR +12.10% · 보유 154일` / `이달 배당·이자 +8만원 · 연 예상 100만원 · 시가배당 1.12% · 배당률·금리 미입력 2건 제외 · 월 평균 환산 기준`
- 자산 추이: 스냅샷 1건 상태에서 `스냅샷이 1건뿐이라 추이를 그릴 수 없습니다` 안내 정상 노출
- 탭 배지: 보유 17 / 목표 배분 4 / 분석 3

---

## V1. 실DB 검증에서 드러난 배포 블로커 (조치 완료)

**`portfolio_item.asset_type` 의 CHECK 제약이 `PENSION` 을 막는다.**

```
ERROR: new row for relation "portfolio_item" violates check constraint "portfolio_item_asset_type_check"
```

- `portfolio_item_asset_type_check` 는 허용 `asset_type` 을 열거한 CHECK 제약인데, **`ddl-auto: update` 는 기존 CHECK 제약을 갱신하지 않는다**
- 그 결과 연금 등록이 `23514` → GlobalExceptionHandler 를 거쳐 **409 CONFLICT** 로 실패했다 (에러 메시지가 "동시성 충돌"이라 원인 파악도 어렵다)
- 하네스(목 API) 검증에서는 절대 드러날 수 없는 문제였다

**조치**: `pension_detail_2026_08_10.sql` 맨 앞에 CHECK 제약 재생성 구문을 추가하고 "운영 적용 필수" 로 표시.

```sql
ALTER TABLE portfolio_item DROP CONSTRAINT IF EXISTS portfolio_item_asset_type_check;
ALTER TABLE portfolio_item ADD CONSTRAINT portfolio_item_asset_type_check
    CHECK (asset_type IN ('BOND','CASH','COMMODITY','CRYPTO','FUND',
                          'GOLD','OTHER','PENSION','REAL_ESTATE','STOCK'));
```

로컬 DB 에 적용 후 재시도 → 연금 등록 200, 이후 수정·납입·배분 집계 모두 정상.

---

## 운영 적용 필수 항목

| # | 항목 | 필수 여부 | 사유 |
|---|---|---|---|
| 1 | `pension_detail_2026_08_10.sql` 의 **CHECK 제약 ALTER 2줄** | **필수** | 없으면 연금 등록이 409 로 실패. `ddl-auto` 가 처리하지 못하는 유일한 변경 |
| 2 | `pension_detail` 테이블 생성 | 선택 | `ddl-auto: update` 가 자동 생성 (FK 이름을 관리하려면 SQL 선적용) |
| 3 | `portfolio_snapshot` 테이블 생성 | 선택 | `ddl-auto` 가 테이블·UNIQUE·인덱스까지 자동 생성 확인 |
| 4 | `stock_purchase_history.fx_rate` 컬럼 추가 | 선택 | `ddl-auto` 가 자동 추가 확인 |

즉 **운영에서 손으로 반드시 넣어야 하는 건 1번 하나**이고, 나머지는 앱 기동만으로 반영된다. 순서는 `SQL 적용 → 앱 배포` 를 권한다.

## 미검증 항목

- **운영 DB 반영** — 로컬 도커 Postgres 에서만 확인했다
- **환차손익 실값** — 보유 해외 주식의 매수 이력이 모두 `fx_rate` 미기록(2건)이라 `fxProfit` 이 0 으로만 나왔다. 신규 해외 매수 후 값이 잡히는 경로는 미검증
- **자산 추이 차트 실렌더** — 스냅샷이 1건뿐이라 안내 문구만 확인했다(2건 이상 라인차트는 하네스에서만 확인)
- **매도 이력 탭 / 목표 배분 탭 / 분석 탭 실서버 화면** — 보유 자산 탭만 실서버로 확인
- **모바일 반응형**
- **금 시세 API** — 로컬 `.env` 서비스키 미등록으로 403 (기존 이슈, 이번 변경과 무관)
- **Elasticsearch 로그 적재** — 로컬 미기동으로 connection refused (기존 이슈, 이번 변경과 무관)

## 정리한 테스트 데이터

- 검증용 연금 항목(`IRP 검증용`, id=20) 삭제 (204)
- 검증용 스냅샷 1건 삭제 → `portfolio_snapshot` 0건
- 로컬 DB 의 `portfolio_item_asset_type_check` 는 **PENSION 포함 상태로 유지**(앱 동작에 필요)
- 앱 프로세스 종료
