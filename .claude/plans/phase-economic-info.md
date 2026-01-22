# Economic Information API Integration Plan

작성일: 2026-01-22

## 작업 목표

주식 시장 분석을 위한 거시경제 지표 데이터 수집 및 제공 기능 구현

## 제공 대상 경제 지표

### 한국 경제 지표
- GDP (분기/연간, 명목/실질)
- 경제성장률
- 산업생산지수
- 경기선행지수 / 경기동행지수
- 소비자물가지수(CPI)
- 근원물가(Core CPI)
- 생산자물가지수(PPI)
- 기준금리
- 국채금리(1Y, 3Y, 5Y, 10Y 등)
- 통화량(M1, M2)
- 환율(원/달러, 원/엔 등)
- 기업경기실사지수(BSI)
- 소비자심리지수(CCI)
- 실업률
- 고용자 수 / 취업자 수
- 비경제활동인구
- 임금 및 소득 통계
- 가계소득/지출
- 주택가격지수
- 주택 거래량
- 주택 착공/인허가/분양 실적
- 부동산 실거래가
- 건설 수주 실적

### 미국/글로벌 경제 지표
- 미국 GDP
- 미국 CPI / Core CPI
- 미국 PPI
- 미국 실업률
- 비농업 고용자수(NFP)
- 평균 임금 상승률
- 연방기금금리(Fed Funds Rate)
- 미국 국채금리(2Y, 10Y 등)
- 장단기 금리차(10Y-2Y)
- 달러 인덱스
- 환율(FOREX)
- 주가지수(S&P500, NASDAQ 등)
- 원자재 가격(유가 등)
- PMI 지수(제조업/서비스업)

## API 데이터 소스 및 무료/유료 정책

### [KOREA] 한국은행 ECOS Open API
**URL**: https://ecos.bok.or.kr/api/

**무료/유료**: 무료 조회 가능
- API 키 발급 필요 (무료 회원 가입)
- 호출 제한 있음 (상세 제한은 공식 문서 확인 필요)

**제공 데이터**:
- GDP (분기/연간, 명목/실질) - 무료 조회 가능
- 경제성장률 - 무료 조회 가능
- 산업생산지수 - 무료 조회 가능
- 경기선행지수 / 경기동행지수 - 무료 조회 가능
- 소비자물가지수(CPI) - 무료 조회 가능
- 근원물가(Core CPI) - 무료 조회 가능
- 생산자물가지수(PPI) - 무료 조회 가능
- 기준금리 - 무료 조회 가능
- 국채금리(1Y, 3Y, 5Y, 10Y 등) - 무료 조회 가능
- 통화량(M1, M2) - 무료 조회 가능
- 환율(원/달러, 원/엔 등) - 무료 조회 가능
- 기업경기실사지수(BSI) - 무료 조회 가능
- 소비자심리지수(CCI) - 무료 조회 가능

**주기**: 월간 / 분기 / 연간 (지표별 상이)

---

### [KOREA] 통계청 KOSIS Open API
**URL**: https://kosis.kr/openapi

**무료/유료**: 무료 조회 가능
- API 무제한 호출 가능 (단, 1회 최대 40,000건)
- 상업적 이용 가능 (출처 표기 필수)
- 국제통계/북한통계는 비상업적 목적만 허용

**제공 데이터**:
- 실업률 - 무료 조회 가능
- 고용자 수 / 취업자 수 - 무료 조회 가능
- 비경제활동인구 - 무료 조회 가능
- 임금 및 소득 통계 - 무료 조회 가능
- 가계소득/지출 - 무료 조회 가능
- 주택가격지수 - 무료 조회 가능
- 주택 거래량 - 무료 조회 가능
- 인구 통계 - 무료 조회 가능
- 소비 관련 통계 - 무료 조회 가능
- 지역별 통계 - 무료 조회 가능

**주기**: 월간 / 분기 / 연간

---

### [KOREA] 공공데이터 포털
**URL**: https://www.data.go.kr

**무료/유료**: 무료 조회 가능
- 공공데이터로 무료 제공
- API 키 발급 필요

**제공 데이터**:
- 주택 착공 실적 - 무료 조회 가능
- 주택 인허가 실적 - 무료 조회 가능
- 주택 분양 물량 - 무료 조회 가능
- 부동산 실거래가 - 무료 조회 가능
- 건설 수주 실적 - 무료 조회 가능
- 토지 거래 현황 - 무료 조회 가능

**주기**: 월간 / 분기

---

### [USA / GLOBAL] FRED (Federal Reserve Economic Data)
**URL**: https://fred.stlouisfed.org/docs/api/fred/

**무료/유료**: 무료 조회 가능
- API 키 발급 필요 (무료)
- Rate Limit: 120 requests/minute
- 별도 유료 플랜 없음

**제공 데이터**:
- 미국 GDP - 무료 조회 가능
- CPI / Core CPI - 무료 조회 가능
- PPI - 무료 조회 가능
- 실업률 - 무료 조회 가능
- 비농업 고용자수(NFP) - 무료 조회 가능
- 평균 임금 상승률 - 무료 조회 가능
- 연방기금금리(Fed Funds Rate) - 무료 조회 가능
- 국채금리(2Y, 10Y 등) - 무료 조회 가능
- 장단기 금리차(10Y-2Y) - 무료 조회 가능
- 달러 인덱스 - 무료 조회 가능
- 글로벌 주요국 경제지표 - 무료 조회 가능

**주기**: 월간 / 분기 / 주간

---

### [GLOBAL] Alpha Vantage API
**URL**: https://www.alphavantage.co/documentation/

**무료/유료**: 무료 + 유료 플랜 제공
- 무료 티어: 25 requests/day, 5 requests/minute
- 유료 플랜: $49.99/month부터 (75~1,200 requests/minute, 일일 제한 없음)

**제공 데이터**:
- 환율(FOREX) - 무료 조회 가능 (제한적)
- 주가지수(S&P500, NASDAQ 등) - 무료 조회 가능 (제한적)
- 일부 거시경제 지표(CPI, GDP 등) - 무료 조회 가능 (제한적)
- 원자재 가격(유가 등) - 무료 조회 가능 (제한적)
- 실시간 미국 시장 데이터 - 유료 조회 가능

**제한**: 무료 API는 일일 호출 횟수 제한 있음

---

### [GLOBAL] NASDAQ Data Link (Quandl)
**URL**: https://docs.data.nasdaq.com/

**무료/유료**: 무료 + 유료 데이터 혼합
- API 사용 자체는 무료
- 무료 데이터: 40개 데이터셋 무료 제공
- 유료 데이터: 210개 이상 데이터셋은 유료 구독 필요
- 무료 Rate Limit: 300 calls/10s, 2,000 calls/10min, 50,000 calls/day
- 유료 Rate Limit: 5,000 calls/10min, 720,000 calls/day

**제공 데이터**:
- 글로벌 경제지표 - 무료/유료 조회 가능 (데이터셋별 상이)
- 국가별 GDP / 물가 - 무료/유료 조회 가능
- 원자재 가격 - 무료/유료 조회 가능
- 금융시장 데이터 - 무료/유료 조회 가능

---

### [NOTE] PMI 지수 (제조업/서비스업)
**URL**: https://kr.investing.com/economic-calendar/ism-manufacturing-pmi-173

**무료/유료**: HTTP GET 요청으로 무료 조회 가능
- PMI 지수 확인 가능

## 구현 범위

### Phase 1: 기본 구조 설계
- [ ] Economic Data 도메인 모델 설계
- [ ] External API 연동 Infrastructure 계층 설계
- [ ] DTO 및 응답 구조 정의

### Phase 2: 한국 경제 지표 연동
- [ ] 한국은행 ECOS API 연동
- [ ] 통계청 KOSIS API 연동
- [ ] 공공데이터 포털 API 연동
- [ ] 한국 경제 지표 조회 API 구현

### Phase 3: 미국/글로벌 경제 지표 연동
- [ ] FRED API 연동
- [ ] Alpha Vantage API 연동 (무료 티어)
- [ ] NASDAQ Data Link API 연동 (무료 데이터셋)
- [ ] PMI 지수 수집
- [ ] 글로벌 경제 지표 조회 API 구현

### Phase 4: 데이터 캐싱 및 최적화
- [ ] 경제 지표 데이터 캐싱 전략 수립
- [ ] Rate Limit 관리 로직 구현
- [ ] 배치 수집 스케줄러 구현

### Phase 5: 테스트 및 문서화
- [ ] 단위 테스트 작성 (TDD)
- [ ] API 문서화
- [ ] 에러 핸들링 및 Fallback 전략 구현

## 제약 사항

### API 호출 제한
- Alpha Vantage 무료: 25 requests/day, 5 requests/minute
- FRED: 120 requests/minute
- NASDAQ Data Link 무료: 50,000 calls/day
- ECOS: 구체적 제한 확인 필요
- KOSIS: 무제한 (1회 40,000건 제한)

### 데이터 신선도
- 경제 지표는 발표 주기가 월간/분기/연간으로 상이
- 실시간 데이터가 아닌 발표 시점 기준 데이터

### 유료 데이터 제외
- Alpha Vantage 실시간 미국 시장 데이터
- NASDAQ Data Link 유료 데이터셋 (210개 이상)
- 초기 구현은 무료 API만 사용

## 참고 자료

- [한국은행 ECOS API 문서](https://ecos.bok.or.kr/api/)
- [통계청 KOSIS API 가이드](https://kosis.kr/serviceInfo/openAPIGuide.do)
- [FRED API 문서](https://fred.stlouisfed.org/docs/api/fred/)
- [Alpha Vantage API 문서](https://www.alphavantage.co/documentation/)
- [NASDAQ Data Link 문서](https://docs.data.nasdaq.com/)