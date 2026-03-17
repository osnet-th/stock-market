# 포트폴리오 종목별 개별 카드 UI 설계

## 작업 리스트
- [x] 종목 항목 컨테이너를 개별 카드로 변경 (`index.html`:686~948)

## 배경
현재 자산 유형별로 하나의 카드 안에 모든 종목이 행(row)으로 나열됨. 종목별 개별 카드로 분리하여 시각적 구분을 명확히 함.

## 핵심 결정
- 카테고리 헤더(접이식) + 소계 행은 기존 그대로 유지
- 헤더 아래 종목들을 개별 `rounded-lg border` 카드로 분리
- 카드 간 `space-y-2` 간격, 카드 영역에 `p-3` 패딩
- 재무 상세 패널, 뉴스 섹션은 해당 카드 내부에 그대로 유지

## 구현

### 변경 파일
`src/main/resources/static/index.html` (686~948 라인 영역)

### 변경 내용

**현재 구조**:
```
<div class="border-t border-gray-100">          ← 섹션 내용 컨테이너
  <template x-for="item in ...">
    <div>                                        ← 래퍼
      <div class="px-5 py-3 border-b ...">      ← 종목 행 (구분선으로 분리)
        ...
      </div>
      <!-- 재무 상세, 뉴스 등 -->
    </div>
  </template>
</div>
```

**변경 후 구조**:
```
<div class="p-3 space-y-2">                     ← 패딩 + 카드 간격
  <template x-for="item in ...">
    <div class="bg-white rounded-lg border border-gray-200 overflow-hidden">  ← 개별 카드
      <div class="px-4 py-3 ...">               ← 종목 내용 (border-b 제거)
        ...
      </div>
      <!-- 재무 상세, 뉴스 등 (카드 내부) -->
    </div>
  </template>
</div>
```

[예시 코드](./examples/card-layout-example.md)