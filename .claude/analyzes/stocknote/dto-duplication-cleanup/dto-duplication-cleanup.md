# [stocknote] application Result ↔ presentation Response DTO 폭발

> ce-review 2026-04-25 P1 #16 (maintainability + project-standards). plan task: Phase 10 P1.

## 현재 상태

application/dto 9개 + presentation/dto 9개 (Request 3 + Response 6). 6+ 케이스에서 Result ↔ Response 가 거의 1:1 복제.

| application Result | presentation Response | 변환 차이 |
|---|---|---|
| `SimilarPatternResult` | `SimilarPatternResponse` | **없음** (TagPair → TagPayload 동일 필드, MatchDto/AggregateDto 동일 필드 그대로 복제) |
| `DashboardResult` | `DashboardResponse` | **없음** (HitRate/TagComboEntry 그대로) |
| `ChartDataResult` | `ChartDataResponse` | NotePoint.summary ↔ triggerTextSummary (1자), DailyPrice → PricePoint (필드 동일 + 평면화) |
| `StockNoteDetailResult` | `StockNoteDetailResponse` | **유지 필요** — locked 계산, NoteDto 평면화, snapshot enum→String |
| `StockNoteListResult` | `StockNoteListResponse` | **유지 필요** — Item.from 가공 (verified/locked/summarize/snapshot 요약) |

> CustomTagResponse 는 도메인 객체(StockNoteCustomTag) 직접 매핑이라 Result 형 자체 없음.

## 영향

| 위험 | 시나리오 |
|---|---|
| 필드 누락 동기화 | `summary` ↔ `triggerTextSummary` 처럼 양쪽이 어긋나기 시작했고 컴파일러 미감지 |
| 변환 코드 누적 | 30~60 라인의 `from()` 메서드가 단순 위임 (가치 없음) |
| 신규 필드 추가 부담 | 매번 4~5 파일 동기화 (domain → mapper → ResultDTO → ResponseDTO + 변환) |

## 해결 옵션

### 옵션 A — 1:1 복제 케이스 (Similar/Dashboard) 의 Response 제거 → Controller 가 Result 직접 반환 (권장)

명백한 1:1 복제만 정리. 변환이 필요한 케이스 (Detail/List) 는 Response 유지.

| 장점 | 단점 |
|---|---|
| 변경 범위 최소 (2 Response 파일 삭제 + Controller 2 메서드 변경) | application Result 가 외부 contract 로 노출됨 (계층 경계 약화) |
| 변환 코드 제거 (~80 라인) | DashboardResult 가 Caffeine Serializable — 응답 노출도 영향 없음 |
| 프론트 응답 형식 무영향 (Result 가 Response 와 동일 필드) | |

### 옵션 B — ChartDataResponse 도 정리 (Result 에 summary 필드명 변경)

Chart 의 NotePoint.summary 필드명을 application Result 측에서도 통일 (triggerTextSummary → summary). PricePoint 는 Result 측에 평면화 record 추가 — 기존 DailyPrice 도메인 객체 사용 안 함.

| 장점 | 단점 |
|---|---|
| Chart 도 단일 표현 | application Result 가 1자 필드명 변경 |
| | 추가 record 정의 |

### 옵션 C — 전체 Response 제거 + Result 만 사용

Detail/List 의 가공도 Result 안에 흡수 (locked 계산 등을 Result 내부 처리).

| 장점 | 단점 |
|---|---|
| 완전 단일 DTO | application Result 가 응답 가공 책임 | 
| | 변경 범위 광범위 |

## 추천: 옵션 A

근거:
- 명백한 복제 (Similar/Dashboard) 만 정리 → 즉시 효과 + 회귀 위험 최소
- 변환이 의미 있는 케이스 (Detail/List) 는 Response 유지 → 책임 분리 유지
- application Result 가 응답으로 노출되는 것은 stocknote 의 "외부 contract = 본인 프론트" 라 안전
- ChartDataResponse 는 NotePoint 필드명 차이 외에 PricePoint 평면화도 있어 변환 가치 인정 (별건 P3 또는 그대로)

## 코드 위치

| 파일 | 변경 |
|---|---|
| `presentation/dto/SimilarPatternResponse.java` | **삭제** |
| `presentation/dto/DashboardResponse.java` | **삭제** |
| `StockNoteAnalyticsController.dashboard` | 반환 타입 → `DashboardResult` |
| `StockNoteAnalyticsController.similarPatterns` | 반환 타입 → `SimilarPatternResult` |

CustomTagResponse / DetailResponse / ListResponse / ChartDataResponse 는 유지.

## 응답 contract 무영향 확인

- DashboardResponse 필드 = DashboardResult 필드 일치 (이미 verified 한 Task #13)
- SimilarPatternResponse 필드 = SimilarPatternResult 필드 일치 (코드 비교 완료)
- 프론트가 받는 JSON 형식 동일

## 후속 task 와의 관계

| Task | 정합 |
|---|---|
| #13 P1 DashboardResponse contract drift | 이미 완료 — Result 형식이 plan 정합. 본 task 후 Response 자체 삭제 |
| #36 P2 SimilarPattern aggregate 누락 필드 | Result 에 필드 추가 시 자동 응답 노출 (Response 변환 코드 갱신 부담 0) |

## 설계 문서

[dto-duplication-cleanup](../../../designs/stocknote/dto-duplication-cleanup/dto-duplication-cleanup.md)
