# 포트폴리오 목표 자산 배분 비율 Review 기록 (#102)

gate: docs/gates/2026-07-30-asset-allocation-target-gates.md
work: docs/works/2026-07-30-asset-allocation-target-work.md
방식: 셀프 리뷰 (태형님 "진행해", 2026-07-30)

## Findings (심각도순)

| ID | 심각도 | 내용 | 근거 | 처리 |
|----|--------|------|------|------|
| M1 | 중간 | 금시세 API가 날짜 파라미터(beginBasDt/endBasDt)를 무시하면 과거 데이터 페이지가 반환되어 오래된 종가로 평가할 위험. 최신성 가드 부재 | `KrxGoldPriceAdapter.fetchLatestPrice` | **반영** — 수신 basDt가 lookback(14일) 범위 밖이면 실패 처리(원금 fallback) |
| L1 | 낮음 | 안전 비율 소수 3자리 입력 시 투자 비율 반올림으로 합≠100 → 서버 400인데 클라이언트는 일반 실패 알림만 표시 | `portfolio.js submitAllocationTarget` | **반영** — safeRatio·자산군 비율을 소수 2자리로 반올림 후 전송 |
| L2 | 낮음 | 자산군 합계 표시에 부동소수 노이즈 가능 (예: 99.99999999%) | `portfolio.js getAllocationAssetSum` | **반영** — 합계 2자리 반올림 |
| N1 | nit | `formatAllocAmount` 데드 코드 (마크업은 Format.number 사용) | `portfolio.js` | **반영** — 제거 |
| N2 | nit | 도넛/자산 비중 막대(클라이언트 계산, 주식만 실시간)와 배분 섹션(서버 평가, 금 포함)의 총액 불일치 가능 | `portfolio.js getEvalAmount` | **미반영 수용** — work 문서 기록, 후속 개선 후보 (클라이언트에 금 시세 전달 경로 필요) |

버그·회귀 위험 재점검 결과:
- `AllocationTargetRepositoryImpl.save`의 삭제→재삽입: 동일 트랜잭션 내 처리, `allocation_target_asset`에 unique 제약 없어 Hibernate flush 순서(INSERT 선행)와 무관하게 안전
- `IllegalArgumentException` → 전역 핸들러 400 매핑 확인 (`GlobalExceptionHandler.java:52`)
- 목표 미설정 204 → `api.js request`가 null 반환 확인 (`api.js:45`) — 모달 기본값으로 동작
- Entity 연관관계 금지·@Transactional 위치·포트-어댑터 방향 등 ARCHITECTURE.md 규칙 준수 확인
- code-convention 위반 사항 없음 (메서드 길이·중첩 기준 내)

## Open Questions / Assumptions

1. **매도 기여율·마감 알림 총자산에 금 평가 포함됨** — `computeTotalAsset`이 평가액 기준이라 금 중량 입력 시 매도 기여율(contributionRate)·알림 메일 총액에도 금 시세가 반영된다. 평가액 기준 통일이 의도라고 가정 (기존 주식과 동일 원리)
2. **KRX 금 종목 필터** — 종목명 "금 99.99" prefix 기준(미니금 제외). KRX 금시장 정식 종목명 체계 가정
3. **DATAGOKR_SERVICE_KEY의 일반상품시세정보 활용신청 여부** — validation 단계 확인 항목

## Change Summary

목표 배분 설정(Entity 2종+GET/PUT)·금 중량 기반 KRX 시세 평가(포트-어댑터+캐시)·배분 현황 API(CRYPTO 제외, 편차·밴드)·포트폴리오 배분 섹션+설정 모달+GOLD 중량 입력·대시보드 카드 업그레이드. 42파일 +1,658줄 (리뷰 반영 4건 별도 커밋). 검증: compileJava·node --check PASS.
