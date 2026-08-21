# 용어 사전 마스터-디테일 리디자인 Validation 기록

gate: docs/gates/2026-08-21-glossary-redesign-gates.md

Status: Done (2026-08-21)
환경: 원격 세션 컨테이너 (Linux), JDK/Gradle 9.2.1, PostgreSQL 16(로컬 기동), Node 22, Chromium(Playwright)

## 실행한 명령과 결과

### 1. 백엔드 컴파일/전체 테스트

| 명령 | 결과 |
|---|---|
| `./gradlew compileJava` | PASS |
| `./gradlew test` (1차, PG 없음) | 117개 중 1 실패 — `StockMarketApplicationTests.contextLoads` (localhost:5432 connection refused, **환경 요인**) |
| 로컬 PG16 기동 (`initdb` + role `root`/`root` + db `stocks`) 후 `./gradlew test` | **117 tests, failures 0, errors 0** |
| 최종 재실행 (`FULL SUITE`) | **tests=117 failures=0 errors=0** |

### 2. 관계 수명주기 스모크 (임시 테스트 — 정책상 미커밋, 실행 후 삭제)

`GlossaryRelationSmokeTest`(@SpringBootTest, 로컬 PG) 를 임시 작성해 실행 후 파일 삭제
(CLAUDE.md '테스트는 명시적 요청 시에만 작성' 준수 — 저장소에 테스트 파일을 남기지 않음):

- 생성 시 중복/비실존 related id 조용히 제거 — PASS
- 수정 시 자기 참조 제거 — PASS
- 타 사용자 용어 참조 조용히 제거 (IDOR/존재 여부 비노출) — PASS
- outbound 관계 보유 용어 삭제 — PASS (**F1 FK 수정 전에는 불가능했던 경로**)
- inbound 정리 + @OrderColumn position 구멍 (c→[a,d] 에서 a 삭제 후 c 조회 = [d]) —
  **F2 수정 전 red(NullPointerException) → 수정 후 green** 재현 검증
- 결과: tests=1 failures=0 errors=0 (사전 revert 실행에서는 failures=1, NPE)

### 3. 스키마 실측 (Hibernate ddl-auto: update 생성분, psql)

- `glossary_term` — abbreviation VARCHAR(200) / one_line VARCHAR(300) / scale_note·example·takeaway TEXT 추가 확인
- `glossary_term_related` — (term_id, position) PK + `idx_glossary_term_related_target`(related_term_id),
  **FK 부재 확인** (NO_CONSTRAINT 반영 후 재생성)
- 백업 SQL 검증: scratch DB 에 `glossary_tables_2026_05_18.sql` → `glossary_term_detail_fields_2026_08_21.sql`
  순차 적용 `ON_ERROR_STOP=1` — PASS

### 4. 프론트

| 검증 | 결과 |
|---|---|
| `node --check js/components/glossary.js` | PASS |
| Playwright 목 하네스 (API 스텁 + 실제 partial/컴포넌트/custom.css + Alpine, Chromium) | **57항목 PASS / 0 FAIL — 3회 연속** |
| pageerror / Alpine Expression 경고 캡처 | 0건 |
| 스크린샷 (보기/편집, 1560px) | 목업 레이아웃/토큰 대조 — 일치 |

하네스 시나리오: A 부트(전량 로드 3페이지 루프·초기 선택·카운트·배지·초성 그룹) /
B 검색(텍스트·초성·클리어·무결과→쿼리 등록·초안 생성/삭제) / C 디테일(점프 칩·섹션 카드·
서브불릿·nudge·채우기·관련 용어 이동) / D 수정·신규 저장(PUT/POST body·related 보존·
초안 write-through·이어쓰기·pending 카테고리·저장 후 선택) / E 카테고리(필터 건수·미분류·
관리 팝오버 생성/이름 변경/삭제 미리보기) / F 정렬(월별 그룹) / G 좁은 화면(단일 pane 전환) /
H 용어 삭제(빈 상태) / I 콘솔 오류 0건

### 5. 워크플로 하네스

- `scripts/check-documented-workflow.sh --through push` — PASS (커밋 직전 실행, push 문서 포함)

## 미검증 항목 (잔여)

1. **실서버 전체 스택 통합** — 컨테이너에 Elasticsearch 미기동 (glossary 는 ES 무관, 컨텍스트 로드는
   ES 없이도 성공 — 재시도 워커 경고만). 실배포 환경 1회 확인 권장.
2. **실브라우저(Tailwind CDN) 렌더** — 하네스는 Tailwind 를 유틸리티 셔밍으로 대체
   (기능·바인딩·custom.css 는 실물). 사용 클래스가 flex 계열 소수라 리스크 낮음.
3. **운영 DB 반영** — ddl-auto: update 환경은 자동, 수동 관리 DB 는 백업 SQL 적용 필요.
4. 모바일 실기기 터치 스크롤 체감.

미검증 항목은 위 범위로 한정되며, 다음 진행(commit/push)은 게이트 로그의 포괄 승인 해석에 따름 —
최종 수용은 태형님이 푸시된 브랜치에서 확인.
