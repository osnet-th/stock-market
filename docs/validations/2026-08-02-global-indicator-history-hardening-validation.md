# 글로벌 경제지표 히스토리 적재 보강 Validation 기록

**Date:** 2026-08-02
**Issue:** #51
gate: docs/gates/2026-08-02-global-indicator-history-hardening-gates.md

## 실행한 검증

| 검증 | 명령/방법 | 결과 |
|------|-----------|------|
| Java 컴파일 | `./gradlew compileJava -q` | 통과 (exit 0) |
| JS 문법 | `node --check global.js` | 통과 |
| 셀프 리뷰 | 트랜잭션 경계·latestMap 정합·정렬 근거·catch-up 조건 라인 검토 | 명시적 findings 없음 |

## 미검증 항목 (운영 배포 후 확인)

1. **`last_collected_at` 컬럼 자동 생성** — 기동 로그에 DDL 에러 없는지 (ddl-auto=update)
2. **첫 기동 catch-up** — `docker logs hubth-app | grep "catch-up"` 에 "catch-up 시작"(첫 기동, NULL 부트스트랩) 후 "완료: N건", 이후 재시작에서는 "catch-up 불필요"
3. **배치 요약 로그** — 16:30(KST) 이후 "글로벌 경제지표 저장 완료: 히스토리 N건 (지표 성공 a / 실패 b / 전체 41)" — #25 추가 10개 지표의 자동 시딩 포함 여부
4. **그래프 확인** — 글로벌 탭 그래프 보기: 1포인트 국가는 점 표시, 데이터 누적 후 x축 시간순 (사전순 뒤섞임 해소)
5. 지표 단위 트랜잭션의 부분 성공은 실운영 실패 상황에서만 관측 가능 — 요약 로그의 실패 카운트와 저장 건수 병존으로 간접 확인

## 판단

이 환경에서 가능한 검증 전부 통과. 미검증 항목은 배포 후 확인 성격 — commit/push 진행.
