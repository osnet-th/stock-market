# DART 재무/공시 엔드포인트 설계

## 작업 리스트

- [ ] 도메인 모델 구현 (7개 record)
- [ ] DartFinancialPort 구현
- [ ] DartFinancialMapper 구현
- [ ] DartFinancialAdapter 구현
- [ ] 응답 DTO 구현 (7개)
- [ ] DartFinancialService 구현
- [ ] DartFinancialController 구현

## 배경

DartApiClient(인프라 클라이언트)가 구현됨. 이를 활용하여 외부에서 DART 재무/공시 데이터를 조회할 수 있는 REST API 엔드포인트를 추가한다. 기존 stock 도메인의 Port/Adapter 패턴을 따른다.

## 핵심 결정

- **Controller 분리**: `DartFinancialController` 신규 생성 (`/api/dart` base path) — 기존 StockController와 분리
- **도메인 모델 통합**: 단일/다중 재무계정 → 1개 record, 단일/다중 재무지표 → 1개 record, 사모/공모 자금 → 1개 record
- **총 도메인 모델 7개**: FinancialAccount, FinancialIndex, FullFinancialStatement, StockTotalQuantity, DividendInfo, LawsuitInfo, FundUsage
- **단일 Port/Adapter**: `DartFinancialPort` 하나에 10개 메서드, `DartFinancialAdapter`가 구현

## API 엔드포인트

| 메서드 | 경로 | 설명 | 파라미터 |
|--------|------|------|----------|
| GET | `/api/dart/accounts/single` | 단일회사 재무계정 | corpCode, bsnsYear, reprtCode |
| GET | `/api/dart/accounts/multi` | 다중회사 재무계정 | corpCodes, bsnsYear, reprtCode |
| GET | `/api/dart/indices/single` | 단일회사 재무지표 | corpCode, bsnsYear, reprtCode, idxClCode |
| GET | `/api/dart/indices/multi` | 다중회사 재무지표 | corpCodes, bsnsYear, reprtCode, idxClCode |
| GET | `/api/dart/statements` | 전체 재무제표 | corpCode, bsnsYear, reprtCode, fsDiv |
| GET | `/api/dart/stock-total` | 주식 총수 현황 | corpCode, bsnsYear, reprtCode |
| GET | `/api/dart/dividends` | 배당 | corpCode, bsnsYear, reprtCode |
| GET | `/api/dart/lawsuits` | 소송 | corpCode, bgnDe, endDe |
| GET | `/api/dart/funds/private` | 사모자금 사용내역 | corpCode, bsnsYear, reprtCode |
| GET | `/api/dart/funds/public` | 공모자금 사용내역 | corpCode, bsnsYear, reprtCode |

## 구현

### 패키지 구조

```
stock/
├── domain/
│   ├── model/
│   │   ├── FinancialAccount.java          # 재무계정 (단일/다중 공용)
│   │   ├── FinancialIndex.java            # 재무지표 (단일/다중 공용)
│   │   ├── FullFinancialStatement.java    # 전체 재무제표
│   │   ├── StockTotalQuantity.java        # 주식 총수 현황
│   │   ├── DividendInfo.java              # 배당
│   │   ├── LawsuitInfo.java               # 소송
│   │   └── FundUsage.java                 # 자금사용내역 (사모/공모 공용)
│   └── service/
│       └── DartFinancialPort.java         # 포트 인터페이스 (10 메서드)
├── application/
│   ├── DartFinancialService.java          # 서비스
│   └── dto/
│       ├── FinancialAccountResponse.java
│       ├── FinancialIndexResponse.java
│       ├── FullFinancialStatementResponse.java
│       ├── StockTotalQuantityResponse.java
│       ├── DividendInfoResponse.java
│       ├── LawsuitInfoResponse.java
│       └── FundUsageResponse.java
├── infrastructure/stock/dart/
│   ├── DartFinancialAdapter.java          # 어댑터 (포트 구현)
│   └── DartFinancialMapper.java           # 매퍼
└── presentation/
    └── DartFinancialController.java       # 컨트롤러
```

### 도메인 모델 + 포트

위치: `stock/domain/model/`, `stock/domain/service/DartFinancialPort.java`

[예시 코드](./examples/domain-port-example.md)

### DartFinancialMapper / DartFinancialAdapter

위치: `stock/infrastructure/stock/dart/`

[예시 코드](./examples/infrastructure-adapter-example.md)

### DartFinancialService / 응답 DTO

위치: `stock/application/`

[예시 코드](./examples/application-service-example.md)

### DartFinancialController

위치: `stock/presentation/DartFinancialController.java`

[예시 코드](./examples/presentation-controller-example.md)

## 주의사항

- 도메인 모델은 record 사용 (기존 Stock, StockPrice 패턴)
- 응답 DTO는 `@Getter` + `@RequiredArgsConstructor` + `static from()` 패턴 (기존 StockPriceResponse 패턴)
- DART API 에러는 DartApiClient에서 이미 DartApiException으로 변환됨 → 추가 에러 처리 불필요
- 단일/다중 재무계정은 FinancialAccount 하나로 통합 (다중 전용 필드는 단일 조회 시 null)
- 사모/공모 자금은 FundUsage 하나로 통합 (구/신 버전 필드명을 도메인 수준에서 정규화)