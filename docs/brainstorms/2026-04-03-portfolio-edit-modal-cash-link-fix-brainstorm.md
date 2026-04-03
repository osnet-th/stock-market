# 포트폴리오 수정 모달 - 계좌 연결 표시 버그 재발 해결

**날짜**: 2026-04-03  
**상태**: 브레인스톰 완료  
**관련 이슈**: GitHub #22

---

## 무엇을 해결하는가

포트폴리오 주식 수정 모달에서 계좌 연결 후 저장 → 다시 수정 버튼 클릭 시 "연결 안 함"으로 표시되는 버그.

- 이전 수정(d41ff09, 36a5491)에도 불구하고 **100% 재발**
- 백엔드 API 응답의 `linkedCashItemId`는 정상 반환 확인됨
- **프론트엔드 Alpine.js 렌더링 문제**로 확인

---

## 근본 원인

### 중첩 x-if 구조에서의 x-model 양방향 바인딩 충돌

```
x-show="showEditModal"
  └─ x-if="editingItem"              ← 1단계
      └─ x-if="assetType === 'STOCK'" ← 2단계
          └─ <select x-model="cashItemId">  ← x-model 초기화
              └─ x-for="cash in getCashItems()"  ← 옵션 렌더링
```

**문제 흐름**:
1. `openEditModal(item)` → `form.cashItemId = "26"` 설정
2. Alpine이 중첩 `x-if` DOM을 생성하며 `<select>` 생성
3. `<select>`의 `x-model` 초기화 시점에 `x-for` 옵션이 아직 미렌더링
4. 브라우저가 select.value를 "" (첫 번째 option)으로 초기화
5. x-model 양방향 바인딩이 ""를 모델에 역동기화 → `cashItemId = ""`
6. `setTimeout(0)` 재설정도 Alpine 반응형 사이클이 미완료 시 재덮어쓰기

---

## 선택된 접근법: x-effect 디렉티브

### 왜 이 접근법인가

- `x-effect`는 Alpine의 반응형 시스템 내에서 실행되므로 DOM 업데이트 완료 후 동기화 보장
- 변경 범위 최소 (index.html의 select 요소 1줄 추가)
- `setTimeout(0)` 해킹 제거 가능
- Alpine 공식 API를 사용한 정석적 해결

### 변경 범위

| 파일 | 변경 내용 |
|------|-----------|
| `index.html` (~1456행) | select에 `x-effect` 디렉티브 추가 |
| `portfolio.js` (~938-942행) | `setTimeout` 블록 제거 |

### 구현 방향

**index.html** - select 요소에 x-effect 추가:
```html
<select x-model="portfolio.editForm.cashItemId"
  x-effect="if(portfolio.editForm.cashItemId) { $el.value = portfolio.editForm.cashItemId }"
  class="...">
```

**portfolio.js** - setTimeout 블록 제거:
```js
// 제거 대상 (938-942행)
// setTimeout(() => {
//     if (item.linkedCashItemId != null) {
//         this.portfolio.editForm.cashItemId = String(item.linkedCashItemId);
//     }
// }, 0);
```

---

## 검증 시나리오

1. 계좌 없음 → 계좌 연결 → 저장 → 수정 재진입 → 연결된 계좌 표시 확인
2. 계좌 연결됨 → 연결 해제 → 저장 → 수정 재진입 → "연결 안 함" 표시 확인
3. 계좌 연결 유지 → 다른 필드만 수정 → 저장 → 수정 재진입 → 동일 계좌 표시 확인
4. 모달 외부 클릭으로 닫기 → 수정 재진입 → 데이터 정상 확인

---

## 기각된 대안

| 접근법 | 기각 사유 |
|--------|-----------|
| x-show로 구조 변경 | 변경 범위가 크고 다른 자산 타입 폼에도 영향 |
| x-init + $watch | x-effect보다 복잡하고 getCashItems() 감시 오버헤드 |
| setTimeout 딜레이 증가 | 근본 해결이 아닌 임시방편, 환경에 따라 실패 가능 |