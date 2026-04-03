# 계좌 연결 수정 후 화면 미반영 버그 분석

GitHub Issue: #22

## 현재 상태

주식 수정 시 계좌 연결(연결 안함 → 계좌 선택) 후 저장 → 다시 수정 화면 진입 시 "연결 안 함"으로 표시됨.
실제 추가매수 시에는 연결된 계좌에서 잔액이 차감되므로 **DB 저장은 정상, 화면만 미반영**.

## 1차 수정 후 검증 결과

- **백엔드 응답 확인**: `GET /api/portfolio/items` 응답에 `linkedCashItemId: 26` 정상 포함
- **String 변환 + `$nextTick` 적용 결과**: **여전히 "연결 안 함" 표시** → `$nextTick`으로는 해결 불가 확인

## 근본 원인 (확정)

**Alpine.js `x-if` 중첩 렌더링에서 `x-model`이 `x-for` 옵션보다 먼저 초기화됨.**

편집 모달의 중첩 구조:
```
x-show="showEditModal"          ← 모달 컨테이너 (DOM 유지)
  └─ x-if="editingItem"        ← 1단계: 조건부 DOM 생성
      └─ x-if="assetType==='STOCK'" ← 2단계: 조건부 DOM 생성  
          └─ select x-model     ← 3단계: 모델 바인딩 (이 시점에 옵션 없음)
              └─ x-for options  ← 4단계: 옵션 렌더링 (이 시점에 이미 모델이 "" 로 리셋됨)
```

Alpine.js의 `x-model`이 `<select>`에 바인딩될 때:
1. `select.value = "26"` 시도 → 매칭 옵션 없음 (x-for 미완료)
2. 브라우저가 `select.value`를 `""` (첫 번째 옵션)으로 폴백
3. Alpine이 `""` 를 읽어 **모델 값을 `""` 로 덮어씀**
4. 이후 `x-for`가 옵션을 렌더링해도 모델은 이미 `""`

`$nextTick`이 실패하는 이유: Alpine의 microtask 내에서 중첩 `x-if`의 자식 `x-for`가 아직 처리되지 않았기 때문.

## 해결 방안

### 방안: `setTimeout(fn, 0)` 으로 macrotask 지연

`$nextTick` (microtask)이 아닌 `setTimeout(fn, 0)` (macrotask)을 사용하면, Alpine의 모든 reactive 처리(중첩 `x-if` + `x-for` 포함)가 완료된 후 실행됨.

```javascript
this.portfolio.editForm = form;
this.portfolio.showEditModal = true;
setTimeout(() => {
    if (item.linkedCashItemId != null) {
        this.portfolio.editForm.cashItemId = String(item.linkedCashItemId);
    }
}, 0);
```

`setTimeout(fn, 0)`이 `$nextTick`보다 나은 이유:
- microtask(Promise, $nextTick): Alpine의 reactive 큐와 같은 레벨에서 실행 → 중첩 템플릿 미완료 가능
- macrotask(setTimeout): 모든 microtask 완료 후 실행 → 모든 DOM 렌더링 보장

## 영향 범위

- `portfolio.js`: `openEditModal()` 의 `$nextTick` → `setTimeout` 변경 (1줄)