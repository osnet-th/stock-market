---
module: salary
tags: [dashboard, mobile, responsive, i18n, ui]
problem_type: bug
issue: "#37"
related_analysis: ".claude/analyzes/salary/dashboard-mobile-i18n/dashboard-mobile-i18n.md"
---

# 대시보드 월급지출내역 - 모바일/한글화 해결 설계

> 분석 문서: `.claude/analyzes/salary/dashboard-mobile-i18n/dashboard-mobile-i18n.md`
> 관련 이슈: [osnet-th/stock-market#37](https://github.com/osnet-th/stock-market/issues/37)

## 1. 목표

1. 메인 대시보드 "월급 사용 비율" 카드의 도넛 차트 범례를 한글로 표시
2. 모바일에서 입력 폼/월 전환 바/차트가 짤리지 않고 자연스럽게 표시되도록 반응형 보강

## 2. 핵심 결정

### 결정 1. 카테고리 한글 라벨은 **백엔드에서 내려준다**
- **이유**:
  - 프론트는 이미 `salary.js`에서 `SALARY_CATEGORIES` 상수로 자체 매핑하고 있으나, 대시보드 카드(`dashboardSummary.js`)는 별도 컴포넌트라 동일 매핑을 중복으로 갖게 됨
  - DTO에 한 번에 라벨을 함께 내리면 프론트에서 분기/매핑 코드 제거, fallback `s.categoryLabel || s.category`도 단순화
  - 다국어 확장 시 enum 한 곳만 수정하면 됨 (현재 다국어 요구는 없으나 DRY 측면)
- **대안 비교**:
  - (A) 프론트에 `SALARY_CATEGORIES` 한 번 더 두기 → 매핑 중복, 추가 변경 시 두 곳 수정 필요. 채택 X
  - (B) 백엔드 DTO에 `categoryLabel` 추가 → 채택
- **범위**: `SpendingCategory` enum에 한글 표시명을 추가하고, `SpendingLineResponse`에서 `categoryLabel` 필드로 노출

### 결정 2. 모바일 레이아웃은 **Tailwind 반응형 수정자만으로 해결**
- 별도 미디어쿼리 CSS 파일을 만들지 않고 기존 inline 클래스에 `sm:`, `md:` 수정자 추가
- 입력 행은 모바일에서 `flex-col`, `sm:` 이상에서 `flex-row`

### 결정 3. 변경 범위는 이슈 #37에 한정
- 도메인 로직, 트랜잭션, JPA 매핑은 일체 건드리지 않음
- 새 API 엔드포인트 추가하지 않음 (기존 응답 DTO에 한 필드 추가만)

## 3. 변경 사항

### 3-1. 백엔드 — 카테고리 한글 라벨 노출

#### (a) `SpendingCategory` enum에 한글 표시명 추가
- 파일: `src/main/java/com/thlee/stock/market/stockmarket/salary/domain/model/enums/SpendingCategory.java`
- 각 값에 `displayName` 필드를 추가하고 `getDisplayName()` 제공
- Lombok `@Getter`(필드 final) 또는 enum 표준 패턴 사용

```java
@Getter
public enum SpendingCategory {
    FOOD("식비"),
    HOUSING("주거"),
    TRANSPORT("교통"),
    EVENTS("경조사"),
    COMMUNICATION("통신"),
    LEISURE("여가"),
    SAVINGS_INVESTMENT("저축·투자"),
    ETC("기타");

    private final String displayName;

    SpendingCategory(String displayName) {
        this.displayName = displayName;
    }
}
```

#### (b) `SpendingLineResponse`에 `categoryLabel` 필드 추가
- 파일: `src/main/java/com/thlee/stock/market/stockmarket/salary/application/dto/SpendingLineResponse.java`
- 필드 추가: `private final String categoryLabel;`
- 생성자/팩토리에서 `category.getDisplayName()`을 채워 넣음
- 기존 `category` 필드는 유지 (프론트가 key로 식별)

### 3-2. 프론트 — 대시보드 카드

- 파일: `src/main/resources/static/js/components/dashboardSummary.js`
- 라인 159: 이미 `s.categoryLabel || s.category || '기타'` fallback 구조라 백엔드만 수정되어도 자동으로 한글 노출됨 (코드 변경 불필요)
- 검증 차원에서 fallback 유지

- 파일: `src/main/resources/static/index.html:295`
- sr-only 테이블도 동일 fallback 적용 중 → 자동 한글 노출

### 3-3. 프론트 — 입력 폼 모바일 반응형 (index.html)

#### 월급 입력 행 (라인 3063-3077)
- 변경 전: `<div class="flex items-center gap-2">`, 라벨 `w-24`, 입력 `flex-1 min-w-0`
- 변경 후: 모바일은 세로, sm부터 가로
  - 컨테이너: `flex flex-col sm:flex-row sm:items-center gap-2`
  - 라벨: `w-full sm:w-24` (모바일에서는 라벨이 위에 한 줄, 데스크톱은 24)

#### 카테고리 입력 행 (라인 3081-3101)
- 변경 전: 라벨 `w-24` + 금액 `w-28` + 메모 `flex-1 min-w-0` 한 줄 flex
- 변경 후: 모바일에서는 2-row 레이아웃
  - 컨테이너: `flex flex-col sm:flex-row sm:items-center gap-2`
  - 라벨 행(모바일): 색상 점 + 라벨 — `w-full sm:w-24`
  - 금액+메모 행: 모바일에서 한 줄에 두 입력 (`flex gap-2`), sm 이상에서는 sm:기존 너비
    - 금액: `w-28` 유지(모바일에서도 충분)
    - 메모: `flex-1 min-w-0`

#### 월 선택 바 (라인 2973-3009)
- 드롭다운 `min-w-[140px]` → `min-w-[120px] sm:min-w-[140px]`
- "새 월 기록" 버튼: 모바일에서는 텍스트 축약/아이콘만 노출하지 않고, 컨테이너에 `flex-wrap` 추가하여 줄바꿈 허용

#### 차트 캔버스 부모 높이 (라인 3110, 3118, 3131)
- `h-64` → `h-48 sm:h-64`
- 라인 차트 `h-80` → `h-56 sm:h-80`

### 3-4. 변경하지 않는 것
- `SalaryService`, Repository, Mapper, JPA Entity
- `salary.js`의 `SALARY_CATEGORIES` (탭 페이지는 여전히 자체 매핑을 사용 — 별도 통일 작업은 본 이슈 범위 외)
- API 엔드포인트, 요청 DTO

## 4. 작업 리스트

### Phase 1. 백엔드
- [ ] `SpendingCategory` enum에 `displayName` 필드 + 한글 값 추가
- [ ] `SpendingLineResponse`에 `categoryLabel` 필드 추가, 팩토리/생성자 반영

### Phase 2. 프론트 (index.html)
- [ ] 월급 입력 행을 `sm:` 반응형으로 수정 (라인 3063-3077)
- [ ] 카테고리 입력 행을 `sm:` 반응형으로 수정 (라인 3081-3101)
- [ ] 월 선택 바 `flex-wrap`, 드롭다운 `min-w` 반응형 적용 (라인 2973-3009)
- [ ] 차트 캔버스 부모 높이 반응형 적용 (라인 3110, 3118, 3131)

### Phase 3. 검증
- [ ] dev 서버 기동 후 메인 대시보드 카드의 도넛 범례가 한글로 보이는지 확인
- [ ] 모바일 뷰포트(예: 375px)에서 입력 폼이 짤리지 않는지 확인
- [ ] 월 전환/저장/삭제 기존 동작 회귀 없는지 확인

## 5. 테스트 계획 (작성 시 별도 요청 시에만)
- 본 이슈는 UI 위주 변경 — 단위 테스트 별도 작성 요청 없으면 생략
- 백엔드 변경은 enum 표시명/DTO 필드 추가에 한정 (로직 변경 없음)

## 6. 리스크 / 주의사항

- `SpendingCategory.getDisplayName()` 추가는 enum이라 BinaryCompatibility 영향 없음
- `SpendingLineResponse`에 필드 추가만 하므로 기존 프론트 호환 유지
- `salary.js`의 자체 카테고리 매핑은 그대로 두므로 탭 페이지 동작에 영향 없음
- Tailwind 클래스 변경은 빌드 산출물 없이 즉시 반영 (정적 리소스)
