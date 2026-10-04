---
title: "feat: 포트폴리오 자산 금융기관 입력과 기관별 합계"
type: feat
issue: 136
issue_url: https://github.com/osnet-th/stock-market/issues/136
status: active
date: 2026-10-03
approved: "2026-10-04 태형님 승인 (기능 plan + DB 스키마 설계 portfolio_item.institution VARCHAR(50) NULL + 단위 테스트 시나리오 D1~D5·M1~M2·S1~S6). '구현 끝까지 진행해'로 단위별 확인 없이 구현을 이어가도록 지시함"
workflow_exception: "클라우드 세션에 compound-engineering(/ce:plan·/ce:work·/ce:review)이 없어 planning·briefing·review 게이트 절차를 수동 적용한다 (#113과 동일)"
branch: claude/inspiring-wright-rljbsn
branch_exception: "클라우드 세션은 지정 브랜치에만 push할 수 있어 issue/136-{slug} 대신 세션 브랜치를 쓴다 (2026-10-03 태형님 확인, brainstorm 확인 7). validate-plan.sh의 branch 형식 검사 1건은 이 예외로 실패한다."
worktree: /home/user/stock-market-issue-136
worktree_note: "/home/user/stock-market을 가리키는 심볼릭 링크. checkpoint-guard.sh가 경로에서 이슈 번호를 읽기 때문에 둔다."
brainstorm: docs/brainstorms/2026-10-03-portfolio-institution-totals-brainstorm.md
test_plan_status: approved
schema_plan_status: approved
allowed_paths:
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/domain/model/PortfolioItem.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/infrastructure/persistence/PortfolioItemEntity.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/infrastructure/persistence/StockItemEntity.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/infrastructure/persistence/BondItemEntity.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/infrastructure/persistence/RealEstateItemEntity.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/infrastructure/persistence/FundItemEntity.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/infrastructure/persistence/CryptoItemEntity.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/infrastructure/persistence/GoldItemEntity.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/infrastructure/persistence/CommodityItemEntity.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/infrastructure/persistence/CashItemEntity.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/infrastructure/persistence/PensionItemEntity.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/infrastructure/persistence/OtherItemEntity.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/infrastructure/persistence/mapper/PortfolioItemMapper.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/application/PortfolioService.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/application/dto/PortfolioItemResponse.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/presentation/PortfolioController.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/presentation/dto/StockItemAddRequest.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/presentation/dto/StockItemUpdateRequest.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/presentation/dto/BondItemAddRequest.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/presentation/dto/BondItemUpdateRequest.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/presentation/dto/RealEstateItemAddRequest.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/presentation/dto/RealEstateItemUpdateRequest.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/presentation/dto/FundItemAddRequest.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/presentation/dto/FundItemUpdateRequest.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/presentation/dto/PensionItemAddRequest.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/presentation/dto/PensionItemUpdateRequest.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/presentation/dto/CashItemAddRequest.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/presentation/dto/CashItemUpdateRequest.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/presentation/dto/GeneralItemAddRequest.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/presentation/dto/GeneralItemUpdateRequest.java
  - src/main/resources/db/migration/portfolio_item_institution_2026_10_03.sql
  - src/main/resources/static/js/components/portfolio.js
  - src/main/resources/static/partials/portfolio-add.html
  - src/main/resources/static/partials/portfolio-edit.html
  - src/main/resources/static/partials/portfolio-holdings.html
  - src/test/java/com/thlee/stock/market/stockmarket/portfolio/domain/model/PortfolioItemInstitutionTest.java
  - src/test/java/com/thlee/stock/market/stockmarket/portfolio/infrastructure/persistence/mapper/PortfolioItemMapperTest.java
  - src/test/java/com/thlee/stock/market/stockmarket/portfolio/application/PortfolioServiceInstitutionTest.java
  - src/test/java/com/thlee/stock/market/stockmarket/portfolio/application/PortfolioEvaluationServicePerItemTest.java
  - src/test/java/com/thlee/stock/market/stockmarket/portfolio/application/PortfolioIncomeServiceTest.java
  - src/test/java/com/thlee/stock/market/stockmarket/portfolio/application/PortfolioServiceAddStockSaleTest.java
  - src/test/java/com/thlee/stock/market/stockmarket/portfolio/application/PortfolioServiceDeleteItemSaleGuardTest.java
  - src/test/java/com/thlee/stock/market/stockmarket/portfolio/application/PortfolioServiceUpdateSaleHistoryTest.java
  - src/test/java/com/thlee/stock/market/stockmarket/portfolio/infrastructure/persistence/PortfolioItemRepositoryImplActiveFilterTest.java
  - docs/plans/tests/2026-10-03-136-portfolio-institution-totals-test-plan.md
  - docs/plans/2026-10-03-002-feat-portfolio-institution-totals-plan.md
  - docs/brainstorms/2026-10-03-portfolio-institution-totals-brainstorm.md
  - .claude/issues/136/**
blocked_paths:
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/domain/model/PensionDetail.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/domain/model/CashStockLink.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/domain/repository/**
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/infrastructure/persistence/PortfolioItemJpaRepository.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/infrastructure/persistence/PortfolioItemRepositoryImpl.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/infrastructure/persistence/PortfolioSnapshotEntity.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/infrastructure/persistence/StockSaleHistoryEntity.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/application/PortfolioSummaryService.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/application/PortfolioEvaluationService.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/application/PortfolioIncomeService.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/application/PortfolioSnapshotBatchService.java
  - src/main/java/com/thlee/stock/market/stockmarket/chatbot/**
  - src/main/java/com/thlee/stock/market/stockmarket/infrastructure/web/GlobalExceptionHandler.java
  - src/main/resources/db/migration/portfolio_item_active_partial_unique.sql
  - src/main/resources/application.yml
  - src/main/resources/application-dev.yml
  - src/main/resources/application-prod.yml
  - src/main/resources/static/js/api.js
  - src/main/resources/static/index.html
  - src/main/resources/static/js/app.js
  - build.gradle
---

# 포트폴리오 자산 금융기관 입력과 기관별 합계 (#136)

## 요약
- 모든 자산군의 등록·수정 화면에 "금융기관" 입력칸을 둔다. 직접 입력하고, 이미 입력한 기관명을 자동 완성으로 제안한다.
- 금융기관은 포트폴리오 항목의 공통 값으로 저장한다(`portfolio_item.institution`, 빈 값 허용). 앞뒤 공백을 지우고, 빈 값은 미지정(null)으로 두며, 50자를 넘으면 거부한다.
- 보유 자산 테이블의 항목명 옆에 금융기관을 작게 보인다.
- 보유 자산 탭의 "자산 추이·자산군 구성" 아래에 "금융기관별 합계" 카드를 둔다. 평가액 기준으로 기관명·항목 수·평가액·비중 막대를 금액 큰 순으로 보이고, "미지정"은 마지막에 둔다.
- 바뀌지 않는 것: 연금 "운용사", 연결 현금 자산, 항목 유일성 규칙(사용자·항목명·자산군), 요약·스냅샷·매도 이력·배당 집계 API

## 작업 리스트
- [x] U1 도메인·영속: 금융기관 값·정규화, 컬럼·매핑, 운영 반영용 SQL (테스트 D·M을 먼저 작성) → 체크포인트 CP1 (2026-10-04, D1~D5·M1~M2 작성 후 컴파일 실패 확인, 구현 후 통과)
- [x] U2 서비스·API: 등록 7종·수정 7종 요청과 항목 응답 (테스트 S를 먼저 작성) → 체크포인트 CP2 (2026-10-04, S1~S6 작성 후 컴파일 실패 확인, 구현 후 통과)
- [x] U3 화면 입력: 등록·수정 모달 입력칸과 자동 완성 (2026-10-04, CP3 하네스로 확인)
- [x] U4 화면 표시·합계: 테이블 금융기관 표시, 금융기관별 합계 카드 → 체크포인트 CP3 (2026-10-04, 브라우저 하네스 26건 통과)
- [x] 단위 테스트 — 승인된 시나리오만, 대상 단위 구현 전에 작성하고 실패를 확인한다 (D 7회·M 11회·S 6건 실행 통과, 포트폴리오 패키지 121건 통과)
- [ ] 검증 (태형님 선택 방식)

## 배경 / 현재 상태
- **공통 값 흐름:** 메모가 금융기관과 같은 성격의 공통 값이다.
  - 도메인 `PortfolioItem.memo`(`PortfolioItem.java:22`)는 재구성 생성자(`:39`, 19개 인자)와 `updateMemo`(`:246`)로 바뀐다.
  - 엔티티는 JOINED 상속이다. 부모 `PortfolioItemEntity`(`portfolio_item` 테이블)가 `memo VARCHAR(500)`을 갖고(`PortfolioItemEntity.java:48-49`), 자식 10종이 생성자에서 부모로 값을 넘긴다.
  - `PortfolioItemMapper`가 양방향 변환을 한다(`PortfolioItemMapper.java:92`, `:118-217`).
- **등록·수정 경로:**
  - `PortfolioController`의 등록 7종(`:97-214`)·수정 7종(`:231-354`)이 요청 DTO 값을 `PortfolioService`의 인자로 풀어 넘긴다.
  - 서비스는 등록 시 `if (memo != null) updateMemo`, 수정 시 항상 `updateMemo(memo)`를 부른다(`PortfolioService.java:89-593`의 등록 7종·수정 7종). 다른 호출처는 없다.
  - 매수·매도·납입·현금 차감 같은 다른 흐름도 항목을 불러와 다시 저장하므로 매퍼를 지난다(예: 주식 등록 시 연결 현금 차감 후 저장 `:130`).
- **응답:** `PortfolioItemResponse`는 `@JsonInclude(NON_NULL)`이라 null 필드는 JSON에서 빠진다(`PortfolioItemResponse.java:11`).
- **화면:**
  - 등록은 `submitAddItem`(`portfolio.js:1302`), 수정은 `openEditModal`(`:1918`)·`submitEditItem`(`:2022`)이 자산군별 본문을 만든다. 각각 호출처가 한 곳뿐이다.
  - 두 모달의 공통 필드 끝에 메모 입력칸이 있다(`portfolio-add.html:383-387`, `portfolio-edit.html:293-297`).
  - 평가액은 `getEvalAmount`(`portfolio.js:360`), 자산군 구성은 `getEvalAllocation`(`:456`)이 화면에서 계산한다.
  - 보유 자산 테이블의 항목명은 두 곳에서 그린다(`portfolio-holdings.html:209` 현금성 서브그룹, `:254` 그 밖).
- **DB:** dev·prod 모두 `ddl-auto: update`라 빈 값을 허용하는 컬럼 추가는 기동 시 자동 반영된다. 운영 수동 적용·롤백 백업용 SQL을 `db/migration`에 남기는 관례가 있다(예: `stock_purchase_fx_rate_2026_08_10.sql`).
- **오류 응답:** 도메인의 `IllegalArgumentException`은 전역 예외 처리기가 400 `BAD_REQUEST`와 메시지로 바꾼다.

## 요구사항 원장
| ID | 요구사항 | 출처 | 이번 범위 | 근거 / 담당 |
|---|---|---|---|---|
| REQ-1 | 자산 등록·수정 시 금융기관을 입력하는 칸을 둔다 | 이슈 작업 내용 1 | 포함 | U1~U3 |
| REQ-2 | 보유 자산 목록에 금융기관을 표시한다 | 이슈 작업 내용 2 | 포함 | U4 |
| REQ-3 | 금융기관별 평가액 합계와 비중을 표시하고, 미입력 자산은 "미지정"으로 묶는다 | 이슈 작업 내용 3 | 포함 | U4, KTD5 |
| REQ-4 | 모든 자산군 등록·수정에서 금융기관을 입력·수정할 수 있다 | 이슈 완료 조건 1 | 포함 | U2·U3(등록 7종·수정 7종) |
| REQ-5 | 금융기관별 합계가 평가액 기준으로 보인다 | 이슈 완료 조건 2 | 포함 | U4, KTD5 |
| REQ-6 | 기존 자산은 금융기관이 비어 있는 상태로 그대로 동작한다 | 이슈 완료 조건 3 | 포함 | U1, DB 스키마 리뷰(빈 값 허용, 채우기 없음) |
| REQ-7 | 금융기관 목록 관리 | 이슈 범위 밖 | 제외 | 이슈 범위 밖. 직접 입력과 자동 완성으로 대신한다(REQ-11) |
| REQ-8 | 은행 API 연동 | 이슈 범위 밖 | 제외 | 이슈 범위 밖 |
| REQ-9 | 계좌번호 저장 | 이슈 범위 밖 | 제외 | 이슈 범위 밖 |
| REQ-10 | 칸 이름은 "금융기관"(예: 국민은행, 키움증권, 업비트)이고 모든 자산군에 둔다 | brainstorm 확인 1 | 포함 | U3 |
| REQ-11 | 직접 입력하고, 이미 입력한 기관명을 자동 완성으로 제안한다 | brainstorm 확인 2 | 포함 | U3, KTD6 |
| REQ-12 | 연금 "운용사"는 그대로 두고 금융기관과 따로 입력한다 | brainstorm 확인 3 | 포함 | KTD4. 운용사 코드는 바꾸지 않는다 |
| REQ-13 | "자산 추이·자산군 구성" 아래 "금융기관별 합계" 카드: 기관명·항목 수·평가액·비중 막대, 금액 큰 순, "미지정" 마지막 | brainstorm 확인 4 | 포함 | U4, KTD5·KTD6 |
| REQ-14 | 보유 자산 테이블의 항목명 옆에 금융기관을 작게 표시한다 | brainstorm 확인 5 | 포함 | U4 |
| REQ-15 | 연결 현금 자산의 금융기관을 이어받지 않는다 | brainstorm 확인 6 | 포함 | KTD4 |
| REQ-16 | 값은 앞뒤 공백을 지우고, 빈 값은 미지정으로 두며, 50자를 넘으면 거부한다 | brainstorm 권장 접근·확인 7 | 포함 | U1, KTD2 |
| REQ-17 | 미지정을 운용사로 묶기, 운용사 값 옮기기 | brainstorm 확인 3 대안 | 제외 | 태형님 결정(운용사 유지) |
| REQ-18 | 매도 이력·스냅샷·알림·배당 집계 변경 | brainstorm 제외 범위 | 제외 | 금융기관은 보유 항목 등록·수정·표시에서만 쓴다 |

## 핵심 기술 결정
**KTD1 — 금융기관은 포트폴리오 항목의 공통 값으로, 메모와 같은 경로로 흐른다.**
- 도메인 `PortfolioItem`에 `institution`을 두고, 재구성 생성자에 메모 바로 뒤 인자로 넣는다.
- 엔티티 부모 `PortfolioItemEntity`에 컬럼을 두고, 부모·자식 10종 생성자에 메모 바로 뒤 인자로 넣는다. `PortfolioItemMapper`가 양방향으로 옮긴다.
- 생성 팩토리(`create`, `createWith*`)는 바꾸지 않는다. 등록 시 서비스가 항목을 만든 뒤 금융기관을 넣는다.
- 생성자 인자가 늘어 기존 테스트 6개 파일의 생성자 호출에 인자 하나(null)를 넣는다. 검증 내용은 바꾸지 않는다.

**KTD2 — 정규화와 길이 검사는 도메인 `updateInstitution` 한 곳에서 한다.**
- 앞뒤 공백을 지운다. 일반 공백·탭·줄바꿈과 유니코드 공백(전각 공백, 줄바꿈 없는 공백 포함)을 모두 공백으로 본다. 그 결과가 비어 있거나 값이 null이면 미지정(null)으로 둔다.
- 지운 뒤 50자를 넘으면 `IllegalArgumentException`("금융기관은 50자 이하로 입력해 주세요.")을 던지고 값을 바꾸지 않는다. API는 400 `BAD_REQUEST`로 응답한다.
- 가운데 공백과 대소문자는 바꾸지 않는다. 재구성 생성자(DB 조회)는 검사하지 않는다.

**KTD3 — API: 요청에 선택 필드를 더하고, 수정은 메모와 같은 의미로 덮어쓴다.**
- 등록 7종·수정 7종 요청 DTO에 `institution`을 추가한다. 서비스 14개 메서드에 메모 바로 뒤 인자로 넘긴다.
- 등록: 항목을 만든 뒤 `updateInstitution`을 부른다.
- 수정: 항상 요청 값으로 바꾼다. 필드가 없거나 빈 값이면 지운다(메모와 같다). 화면은 수정 시 현재 값을 늘 함께 보낸다.
- `PortfolioItemResponse`에 `institution`을 추가한다. 값이 없으면 JSON에서 빠진다.

**KTD4 — 다른 값과 섞지 않는다.**
- 연결 현금 자산의 금융기관을 이어받지 않고, 연금 운용사와도 섞지 않는다. 합계는 각 항목의 금융기관 값만 본다.
- 항목 유일성 규칙(ACTIVE 항목의 사용자·항목명·자산군)은 그대로다. 같은 이름의 예금을 두 은행에 두려면 항목명을 다르게 해야 한다.

**KTD5 — 합계는 화면에서 계산한다(자산군 구성과 같은 방식).**
- `getInstitutionTotals()`: 보유 항목을 금융기관 값으로 묶어 기관명·항목 수·평가액·비중을 만든다.
  - 평가액은 `getEvalAmount(item)`의 합이다. 비중은 기관 평가액 ÷ 총 평가액 × 100이고, 자산군 구성처럼 소수 첫째 자리로 반올림한다. 총 평가액이 0이면 0%다.
  - 평가액 큰 순으로 정렬하고, 같으면 기관명 가나다순이다. 미지정은 금액과 관계없이 마지막이다.
  - 묶는 기준은 서버가 정규화한 문자열 그대로다. 띄어쓰기나 대소문자가 다르면 다른 기관이다.
- 새 API는 없다. 목록 응답의 `institution`만 쓴다.

**KTD6 — 화면 배치**
- 입력칸: 두 모달의 공통 필드에서 메모 바로 위. 라벨 "금융기관", 안내 "예: 국민은행, 키움증권, 업비트 (선택)", 최대 50자.
- 자동 완성: 보유 항목의 금융기관 값(중복 제거, 가나다순)을 브라우저 기본 자동 완성 목록(`datalist`)으로 제안한다. 두 모달은 서로 다른 목록 id를 쓴다.
- 테이블: 항목명 바로 뒤에 작은 회색 표시. 값이 없으면 아무것도 보이지 않는다.
- 합계 카드: 보유 항목이 있을 때만 보인다. 모든 항목이 미지정이면 "자산 수정에서 금융기관을 입력하면 기관별로 묶어 보여 줍니다." 안내를 함께 보인다. 모바일 폭(375px)에서 가로로 넘치지 않게 한다.

## API 계약
| 구분 | 변경 |
|---|---|
| 등록 7종 `POST /api/portfolio/items/{stock,bond,real-estate,fund,pension,cash,general}` | 요청 본문에 `institution`(문자열, 선택) 추가 |
| 수정 7종 `PUT /api/portfolio/items/{type}/{itemId}` | 요청 본문에 `institution` 추가. 없거나 빈 값이면 지운다 |
| 목록 `GET /api/portfolio/items`, 등록·수정 응답 | `institution` 추가. 값이 없으면 필드가 빠진다 |
| 오류 | 앞뒤 공백을 지운 값이 50자 초과 → 400 `BAD_REQUEST`, "금융기관은 50자 이하로 입력해 주세요." |

경로·인증·다른 필드와 요약·스냅샷·매도 이력 API는 그대로다.

## DB 스키마 리뷰 (2026-10-04 승인)
| 항목 | 설계 |
|---|---|
| table | `portfolio_item` (기존, 포트폴리오 항목 공통 부모 테이블). 목적: 자산을 보관하는 금융기관 이름 |
| column | `institution` `VARCHAR(50)`, NULL 허용, default 없음 |
| PK·UK·FK·index·constraint | 추가하지 않는다. 기존 PK `id`, `idx_portfolio_item_user_id`, ACTIVE 부분 유일 인덱스(user_id, item_name, asset_type)는 그대로다. 합계는 사용자 항목 목록을 화면에서 묶으므로 인덱스가 필요 없다 |
| Entity 매핑 | `PortfolioItemEntity`에 `@Column(name = "institution", length = 50)` 문자열 필드. 연관관계 없음 |
| 반영 방식 | `ddl-auto: update`가 기동 시 컬럼을 추가한다. 운영 수동 적용·롤백 백업용으로 `portfolio_item_institution_2026_10_03.sql`(`ADD COLUMN IF NOT EXISTS`)을 남긴다. 기존 행 채우기는 없다 |
| 롤백 | 코드만 되돌려도 동작한다(남은 컬럼은 쓰지 않는다). 컬럼까지 지우려면 SQL 파일의 롤백 문(`DROP COLUMN IF EXISTS`)을 쓴다. 이때 입력한 금융기관 값은 사라진다 |
| 기존 데이터 영향 | 기존 행은 NULL이라 화면에서 "미지정"으로 묶인다. 다른 테이블·스냅샷·매도 이력은 영향이 없다 |

## 구현 단위
### U1 도메인·영속 (KTD1·KTD2)
- `PortfolioItem`: `institution` 값, 재구성 생성자 인자, `updateInstitution`(정규화·길이 검사, `updatedAt` 갱신)
- `PortfolioItemEntity`: 컬럼과 생성자 인자. 자식 10종 생성자는 인자를 받아 부모로 넘기기만 한다.
- `PortfolioItemMapper`: 엔티티 → 도메인, 도메인 → 엔티티(10종) 모두 금융기관을 옮긴다.
- `db/migration/portfolio_item_institution_2026_10_03.sql`: 컬럼 추가와 주석 처리한 롤백 문. 주석에 이슈 번호를 쓰지 않는다.
- 기존 테스트 6개 파일: 생성자 호출에 인자 하나(null)만 넣는다.
- 승인된 테스트 D(도메인)·M(매핑)을 먼저 작성하고 실패를 확인한다.

### U2 서비스·API (선행 U1, KTD3)
- `PortfolioService`: 등록 7종·수정 7종 메서드에 `institution` 인자를 추가하고 KTD3대로 넣는다.
- `PortfolioItemResponse`: `institution` 필드
- 요청 DTO 14종: `institution` 필드
- `PortfolioController`: 14곳에서 `request.getInstitution()`을 넘긴다.
- 승인된 테스트 S(서비스)를 먼저 작성하고 실패를 확인한다.

### U3 화면 입력 (선행 U2, KTD6)
- `portfolio.js`
  - `submitAddItem`·`submitEditItem`의 자산군별 본문 7종씩에 `institution`을 넣는다.
  - `openEditModal`이 현재 값을 폼에 채운다.
  - `getInstitutionNames()`: 자동 완성 목록
- `portfolio-add.html`·`portfolio-edit.html`: 메모 위 입력칸과 자동 완성 목록

### U4 화면 표시·합계 (선행 U3, KTD5·KTD6)
- `portfolio.js`: `getInstitutionTotals()`
- `portfolio-holdings.html`
  - 항목명 옆 금융기관 표시 2곳(`:209`, `:254`)
  - "자산 추이·자산군 구성"(`:61-120`)과 "보유 자산"(`:122`) 사이에 합계 카드

## 시스템 전반 영향
- 기동 시 `portfolio_item`에 컬럼이 하나 추가된다. 쓰기·조회 쿼리는 이 컬럼만 더 다룬다.
- 등록·수정 요청에 필드가 하나 늘고 응답에 필드가 하나 늘어난다. 기존 클라이언트가 필드를 보내지 않으면 등록은 미지정, 수정은 지움으로 처리된다.
- 챗봇 문맥은 항목 응답을 쓰지만 금융기관을 쓰지 않는다. 바꾸지 않는다.

## 위험과 완화
| 위험 | 완화 |
|---|---|
| 같은 기관이 다른 이름으로 갈라진다(예: 국민은행, KB국민은행) | 자동 완성으로 이미 쓴 이름을 제안한다. 이름 합치기는 범위 밖이다 |
| 수정 요청에 `institution`이 빠지면 값이 지워진다 | 화면은 수정 시 현재 값을 늘 보낸다. 메모와 같은 의미이고 API 계약에 적는다 |
| 매퍼나 자식 엔티티 하나라도 값을 옮기지 않으면 그 자산군은 저장되지 않고, 매수·매도·납입·현금 차감처럼 항목을 다시 저장하는 흐름에서 입력한 값이 지워진다 | 매핑 왕복 테스트를 모든 자산군으로 돌린다(테스트 M). 연결 현금 차감 뒤에도 현금 자산의 값이 남는지 서비스 테스트로 본다(테스트 S) |
| 같은 항목명·자산군은 금융기관이 달라도 중복으로 막힌다 | 기존 규칙 유지(KTD4). 필요하면 후속 이슈에서 유일 인덱스를 바꾼다 |
| 자동 완성 목록이 Alpine 반복 렌더링과 함께 동작하지 않을 수 있다 | CP3 브라우저 하네스에서 확인한다. 안 되면 plan을 갱신한다 |
| 합계 카드가 모바일 폭에서 넘친다 | 작은 화면에서는 막대를 줄이거나 숨긴다. CP3에서 375px를 확인한다 |

## 단위 테스트 계획
- 테스트 작성: 작성함 (2026-10-03 태형님 확인, brainstorm 확인 7)
- 테스트 계획 문서: [테스트 계획](./tests/2026-10-03-136-portfolio-institution-totals-test-plan.md)
- 사용자 승인: 테스트 시나리오 승인됨 (2026-10-04)
- 승인된 테스트 시나리오: 정상 6건, 예외·경계 7건 (도메인 D1~D5, 매핑 M1~M2, 서비스 S1~S6)
- 검증 명령: `./gradlew test --tests "*PortfolioItemInstitutionTest" --tests "*PortfolioItemMapperTest" --tests "*PortfolioServiceInstitutionTest"`
- 작성 순서: 도메인·매핑 테스트는 U1 구현 전에, 서비스 테스트는 U2 구현 전에 작성하고 실패를 확인한다.

## 작업 진행 방식 (워크플로우 예외)
이 세션에는 compound-engineering(`/ce:work`·`/ce:review`)이 없어 같은 절차를 수동으로 적용한다(#113과 동일).
- **구현:** 작업 리스트 순서대로 한 단위씩 진행한다.
  - 단위마다 변경 파일이 allowed_paths 안이고 blocked_paths 밖인지 확인한다.
  - 테스트 대상 단위는 테스트를 먼저 작성하고 실패를 확인한다.
  - 각 단위를 마치면 태형님께 다음 진행 여부를 확인한다.
  - 2026-10-04 태형님의 "구현 끝까지 진행해" 지시로 단위별 확인 없이 이어서 진행하고, 체크포인트 결과는 마지막에 함께 보고한다. 경로 위반이나 범위 밖 변경(SCOPE_CREEP)이 나오면 즉시 멈추고 보고한다.
- **체크포인트:** CP마다 `checkpoint-guard.sh`로 경로를 검사하고 read-only 가드 리뷰를 수행한다.
- **리뷰:** 리뷰 게이트 문서 기준으로 finding 표를 만들고, 태형님이 고른 이슈만 수정한다.

## 체크포인트
- CP1 (U1 후): 정규화·길이 검사, 모든 자산군 매핑 왕복, 테스트 D·M 통과, 기존 테스트 컴파일
- CP2 (U2 후): 등록·수정 14개 경로와 응답, 테스트 S 통과
- CP3 (U4 후): 저장소 밖 브라우저 하네스(실제 JS·partial 사용)로 입력칸·자동 완성·테이블 표시·합계 카드(정렬·미지정·비중·모바일 폭)를 확인한다.

### CP1 결과 (2026-10-04)
- 경로 검사 통과(변경 22개 파일 모두 plan 범위 안). 가드 리뷰 판정 CLEAN.
- 가드 참고 처리
  - 앞뒤 공백 정리 정규식(`[\s\p{Z}]+$`)이 가운데에 긴 공백이 있는 입력에서 길이의 제곱만큼 느려진다(60,000자 입력 약 1.6초). 길이 검사보다 먼저 실행되므로 양 끝에서 훑는 선형 탐색으로 바꿨다(16ms, 별도 커밋). 공백으로 보는 문자는 같고, 보이지 않는 제어 문자 U+001C~001F도 공백으로 본다.
  - 기존 테스트의 복사 도우미 4곳에는 null 대신 원본 값(`getInstitution()`)을 넘겼다. 실행 값이 null이라 검증 결과는 같다(가드 판정: 허용).
  - 50자는 문자 수(코드 포인트) 기준으로 센다. PostgreSQL VARCHAR(50)과 같은 기준이다.

### CP2 결과 (2026-10-04)
- 경로 검사 통과(변경 18개 파일 모두 plan 범위 안). 가드 리뷰 판정 CLEAN. 서비스 14개 메서드와 컨트롤러 14곳 모두 memo 바로 뒤에 institution을 넘긴다.
- 가드 참고 처리: 테스트 계획의 저장 Mock 설명을 실제 fixture에 맞게 고쳤다(id가 없는 새 항목은 id를 붙인 사본을 돌려준다). 시나리오와 확인 내용은 같다.

## 검증
검증 방식은 verify 단계에서 태형님이 고른다. 후보와 이 세션의 제약:
- `./gradlew test`: 임시 Postgres로 실행할 수 있다.
- `bootRun` + curl: 임시 Postgres로 기동해 컬럼 추가, 등록·조회·수정·지우기, 50자 초과 400을 확인할 수 있다.
- 운영 반영 후 확인(태형님): 기존 자산이 "미지정"으로 묶여 보이는지, 금융기관을 넣은 자산이 카드와 테이블에 보이는지

## 수정 범위
- **수정 가능:** 위 allowed_paths 범위
  - 포트폴리오 도메인 항목, 엔티티 부모·자식 10종, 매퍼, 운영 반영용 SQL
  - `PortfolioService`, `PortfolioItemResponse`, `PortfolioController`, 요청 DTO 14종
  - `portfolio.js`, 등록·수정·보유 자산 partial
  - 신규 단위 테스트 3종과 테스트 계획 문서, 생성자 인자만 바꾸는 기존 테스트 6종
- **수정 금지:** 위 blocked_paths 범위
  - 연금 상세·현금 연결 도메인, 리포지토리 포트·JPA 리포지토리·구현체
  - 요약·평가·배당·스냅샷 서비스, 챗봇, 전역 예외 처리기
  - 유일 인덱스 SQL, 설정 파일, `api.js`·`index.html`·`app.js`·`build.gradle`

## 완료 정의
- REQ-1~6과 REQ-10~16을 충족하고, REQ-7~9·17·18은 제외로 유지한다.
- 모든 자산군에서 금융기관을 입력·수정·삭제할 수 있고, 테이블과 합계 카드에 평가액 기준으로 보인다.
- 기존 자산은 미지정으로 그대로 동작하고, 승인된 단위 테스트가 통과한다.
