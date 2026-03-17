# 포트폴리오 주식 국내/해외 분리 표시 설계

## 작업 리스트
- [x] `getItemsByType` 대신 STOCK 타입일 때 국내/해외 서브그룹으로 분리 렌더링 (`index.html`:685~948)
- [x] 국내/해외 주식 필터 헬퍼 함수 추가 (`app.js`)

## 배경
STOCK 카테고리 내에서 국내(KR)와 해외 주식이 구분 없이 나열됨. 서브 헤더로 시각적 분리 필요.

## 핵심 결정
- `stockDetail.country === 'KR'`로 국내/해외 구분
- STOCK 타입에서만 서브 헤더 적용, 다른 자산 유형은 기존 그대로
- 서브 헤더 스타일: 작은 텍스트 라벨 (`🇰🇷 국내 주식`, `🌐 해외 주식`)
- 해당 그룹에 항목이 없으면 서브 헤더도 숨김

## 구현

### app.js — 헬퍼 함수 추가
위치: `src/main/resources/static/js/app.js` (`getItemsByType` 근처)

[예시 코드](./examples/helper-functions-example.md)

### index.html — STOCK 섹션 분기 렌더링
위치: `src/main/resources/static/index.html` (685~948 라인)

현재 `getItemsByType(alloc.assetType)`로 단일 루프 → STOCK일 때 국내/해외 두 그룹으로 분리

[예시 코드](./examples/template-layout-example.md)