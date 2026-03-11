# 재무정보 API 메서드 예시

## api.js 추가 메서드

```javascript
// ==================== Stock Financial ====================

// 보고서코드/지표분류코드 옵션 조회
getFinancialOptions() {
    return this.request('GET', '/api/stocks/financial/options');
},

// 단일회사 재무계정
getFinancialAccounts(stockCode, year, reportCode) {
    return this.request('GET',
        `/api/stocks/${stockCode}/financial/accounts?year=${year}&reportCode=${reportCode}`);
},

// 단일회사 재무지표
getFinancialIndices(stockCode, year, reportCode, indexClassCode) {
    return this.request('GET',
        `/api/stocks/${stockCode}/financial/indices?year=${year}&reportCode=${reportCode}&indexClassCode=${indexClassCode}`);
},

// 단일회사 배당정보
getFinancialDividends(stockCode, year, reportCode) {
    return this.request('GET',
        `/api/stocks/${stockCode}/financial/dividends?year=${year}&reportCode=${reportCode}`);
},

// 단일회사 주식수량
getFinancialStockQuantities(stockCode, year, reportCode) {
    return this.request('GET',
        `/api/stocks/${stockCode}/financial/stock-quantities?year=${year}&reportCode=${reportCode}`);
},

// 전체재무제표
getFullFinancialStatements(stockCode, year, reportCode, fsDiv) {
    return this.request('GET',
        `/api/stocks/${stockCode}/financial/full-statements?year=${year}&reportCode=${reportCode}&fsDiv=${fsDiv}`);
},

// 소송현황
getLawsuits(stockCode, startDate, endDate) {
    return this.request('GET',
        `/api/stocks/${stockCode}/financial/lawsuits?startDate=${startDate}&endDate=${endDate}`);
},

// 사모자금사용
getPrivateFundUsages(stockCode, year, reportCode) {
    return this.request('GET',
        `/api/stocks/${stockCode}/financial/private-fund-usages?year=${year}&reportCode=${reportCode}`);
},

// 공모자금사용
getPublicFundUsages(stockCode, year, reportCode) {
    return this.request('GET',
        `/api/stocks/${stockCode}/financial/public-fund-usages?year=${year}&reportCode=${reportCode}`);
},
```