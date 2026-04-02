# SEC EDGAR 원본 확인 링크 추가

## 배경

해외주식 재무제표 데이터의 값 검증을 위해, UI에서 SEC EDGAR 원본 Filing 페이지로 바로 이동할 수 있는 링크를 제공한다.

## 핵심 결정

- **CIK 전달 방식**: 기존 재무제표 API 응답을 변경하지 않고, 별도 경량 엔드포인트(`GET /api/stocks/{ticker}/sec/cik`)를 추가
  - 기존 API 계약 변경 없음
  - 프론트엔드에서 SEC 페이지 진입 시 1회만 호출
- **EDGAR URL**: `https://www.sec.gov/cgi-bin/browse-edgar?action=getcompany&CIK={CIK}&type=10-K&dateb=&owner=include&count=40`

## 작업 리스트

### 백엔드

- [x] `SecFinancialController`에 CIK 조회 엔드포인트 추가
  - `GET /api/stocks/{ticker}/sec/cik` → `{ "cik": 320193 }`
  - `SecCikCache.getCik(ticker)` 활용 (이미 존재)

### 프론트엔드

- [x] `api.js`에 CIK 조회 API 메서드 추가
- [x] `financial.js`에 SEC EDGAR 링크 버튼 추가
  - SEC 재무제표 영역 상단에 외부 링크 아이콘 + "SEC 원본 확인" 버튼 배치
  - 클릭 시 새 탭으로 EDGAR Filing 페이지 오픈