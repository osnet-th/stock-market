# 포트폴리오 자산 금융기관 입력과 기관별 합계 단위 테스트 계획

- 승인: 2026-10-04 태형님 승인 (plan 승인 요청과 함께 제시한 D1~D5, M1~M2, S1~S6)

## 테스트 대상
- 대상 클래스/메서드
  - `PortfolioItem.updateInstitution(institution)`
  - `PortfolioItemMapper.toEntity(item)`, `PortfolioItemMapper.toDomain(entity)`
  - `PortfolioService`의 등록 7종(`add*Item`)·수정 7종(`update*Item`)
- 대상 유스케이스
  - 금융기관 값의 앞뒤 공백을 지우고, 빈 값은 미지정으로 두며, 50자를 넘으면 거부한다.
  - 모든 자산군에서 금융기관이 저장·조회 변환을 거쳐도 유지된다.
  - 등록·수정 요청의 금융기관이 응답에 반영되고, 연결 현금 자산의 금융기관을 이어받지 않는다.

## 테스트 범위
- 포함: 공백 정규화(유니코드 공백 포함), 빈 값 처리, 50자 경계, 10개 자산군 매핑 왕복, 기존 행 호환, 등록 7종·수정 7종 반영, 수정 시 지움, 길이 초과 오류, 연결 현금 자산 비상속
- 제외: 컨트롤러(요청 DTO → 서비스 인자 전달), JPA·DDL(컬럼 생성), 전역 예외 처리기의 400 변환, 화면(CP3 브라우저 하네스로 확인)

## Mock 대상
- Repository: `PortfolioItemRepository`(저장은 받은 항목을 돌려준다. id가 없는 새 항목은 저장소처럼 id를 붙인 사본을 돌려준다. 주식-현금 연결 생성에 id가 필요하다), `StockPurchaseHistoryRepository`, `StockSaleHistoryRepository`, `DepositHistoryRepository`, `CashStockLinkRepository`
- 외부 API: `ExchangeRatePort`
- 기타 외부 의존성: `PortfolioEvaluationService`, `KeywordService`, `KeywordRepository`, `UserKeywordRepository`, `DomainEventLogger`
- 도메인·매퍼 테스트는 Mock 없이 실제 객체로 확인한다.
- 검증은 반환값과 객체 상태로만 한다. 호출 여부·횟수는 검증하지 않는다.

## 테스트 케이스
### 도메인 (`PortfolioItemInstitutionTest`)
| Case | Given | When | Then |
|---|---|---|---|
| D1 앞뒤 공백 제거 | 금융기관이 없는 항목 | `"  국민은행  "`으로 갱신 | `"국민은행"` |
| D2 유니코드 공백 제거 | 금융기관이 없는 항목 | 앞에 전각 공백, 뒤에 줄바꿈 없는 공백과 탭이 붙은 `"KB 국민은행"`으로 갱신 | `"KB 국민은행"` (가운데 공백 유지) |
| D3 빈 값은 미지정 | `"국민은행"`이 있는 항목 | null, `""`, `"   "` 각각으로 갱신 | 미지정(null) |
| D4 50자 경계 | 금융기관이 없는 항목 | 정확히 50자로 갱신, 이어서 앞뒤 공백 포함 52자(본문 50자)로 갱신 | 두 번 모두 본문 50자가 저장된다 |
| D5 50자 초과 | `"국민은행"`이 있는 항목 | 51자로 갱신 | `IllegalArgumentException`("금융기관은 50자 이하로 입력해 주세요."), 값은 `"국민은행"` 그대로 |

### 매핑 (`PortfolioItemMapperTest`)
| Case | Given | When | Then |
|---|---|---|---|
| M1 10개 자산군 왕복 | 자산군마다(주식·채권·부동산·펀드·암호화폐·금·원자재·현금성·연금·기타) 금융기관 `"키움증권"`인 항목 | 도메인 → 엔티티 → 도메인 | 금융기관 `"키움증권"`, 자산군 그대로 |
| M2 기존 행 호환 | 금융기관이 null인 엔티티 | 엔티티 → 도메인 | 미지정(null) |

### 서비스 (`PortfolioServiceInstitutionTest`)
| Case | Given | When | Then |
|---|---|---|---|
| S1 등록 7종 반영 | 중복 항목 없음 | 주식·채권·부동산·펀드·연금·현금성·일반(암호화폐) 등록, 금융기관 `"  키움증권 "` | 7건 모두 응답 금융기관 `"키움증권"` |
| S2 등록 시 값 없음 | 중복 항목 없음 | 채권 등록, 금융기관 null | 응답 금융기관 null |
| S3 수정 7종 반영 | 자산군마다 금융기관 `"국민은행"`인 기존 항목 | 수정 7종, 금융기관 `"신한은행"` | 7건 모두 응답 금융기관 `"신한은행"` |
| S4 수정 시 지움 | 금융기관 `"국민은행"`인 현금성 항목 | 수정, 금융기관 null | 응답 금융기관 null |
| S5 등록 시 길이 초과 | 중복 항목 없음 | 현금성 등록, 금융기관 51자 | `IllegalArgumentException`("금융기관은 50자 이하로 입력해 주세요.") |
| S6 연결 현금 자산 비상속 | 금융기관 `"국민은행"`, 잔액 10,000,000인 CMA | 그 CMA를 연결해 주식 10주·매수가 70,000 등록, 금융기관 null | 주식 응답 금융기관 null. CMA는 잔액 9,300,000으로 차감되고 금융기관 `"국민은행"` 유지 |

- 정상 케이스: D1, D2, M1, S1, S3, S6 (6건)
- 예외·경계 케이스: D3, D4, D5, M2, S2, S4, S5 (7건)

## 검증 명령
- `./gradlew test --tests "*PortfolioItemInstitutionTest" --tests "*PortfolioItemMapperTest" --tests "*PortfolioServiceInstitutionTest"`
- 회귀: `./gradlew test --tests "com.thlee.stock.market.stockmarket.portfolio.*"` (생성자 인자를 바꾼 기존 테스트 6종 포함)
