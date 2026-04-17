# 프론트 UI 버튼 예시

체크박스/카탈로그는 없다. **종목 선택 → 분석 버튼 4개 → 클릭만**.

## 1. 상태 확장 (`chat.js`)

기존 상태에서 신규 필드는 없음. 분석 버튼 트리거 함수만 추가된다.

## 2. 버튼 트리거 함수

```javascript
canRequestAnalysis() {
  return this.chat.chatMode === 'FINANCIAL'
      && !!this.chat.stockCode
      && !this.chat.isLoading;
},

requestAnalysis(task) {
  if (!this.canRequestAnalysis()) return;
  this.sendChatMessage({
    message: '',
    analysisTask: task,
  });
},
```

`sendChatMessage`는 옵션 객체를 받아 `analysisTask`를 요청 바디에 실어 보내도록 확장한다.

## 3. `api.js` `streamChat` 호출

```javascript
// api.js
body: JSON.stringify({
  message,
  chatMode,
  stockCode,
  indicatorCategory,
  analysisTask,      // 신규
  messages
})
```

## 4. HTML 마크업 (`index.html` FINANCIAL 섹션)

```html
<!-- 종목 선택 배지 바로 아래 -->
<div x-show="chat.chatMode === 'FINANCIAL' && chat.stockCode"
     class="chat-analysis-buttons">
  <button :disabled="!canRequestAnalysis()"
          @click="requestAnalysis('UNDERVALUATION')">저평가/고평가 판단</button>
  <button :disabled="!canRequestAnalysis()"
          @click="requestAnalysis('TREND_SUMMARY')">실적 추세 요약</button>
  <button :disabled="!canRequestAnalysis()"
          @click="requestAnalysis('RISK_DIAGNOSIS')">리스크 요인 진단</button>
  <button :disabled="!canRequestAnalysis()"
          @click="requestAnalysis('INVESTMENT_OPINION')">투자 적정성 의견</button>
</div>
```

CSS 제안: 2×2 그리드 (4개 버튼).

## 5. FINANCIAL 모드 입력창 숨김

```html
<!-- 기존 입력창에 조건 추가 -->
<div class="chat-input-area" x-show="chat.chatMode !== 'FINANCIAL'">
  <textarea ...></textarea>
  <button @click="sendChatMessage()">전송</button>
</div>
```

## 6. 모드 라벨 변경

```html
<button @click="setChatMode('FINANCIAL')">종목 분석</button>  <!-- 기존 '종목 재무' -->
```

## 7. 주의

- 분석 버튼 클릭 시 사용자 메시지는 빈 문자열. 서버는 `analysisTask` 기반으로 프롬프트 조립.
- 분석 응답이 끝난 뒤에도 같은 종목에 다른 버튼을 이어서 클릭 가능.
- 종목 변경 시 어차피 버튼은 동일 — 별도 초기화 로직 없음.
- 카테고리 체크박스·세부 항목 체크박스·카탈로그 조회 API 모두 제공하지 않음.