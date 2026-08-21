# 월급 사용 비율 화면 목업 기반 재설계 Validation 기록

- gate: docs/gates/2026-08-21-salary-usage-screen-redesign-gates.md
- 환경: Claude Code 원격 컨테이너 (PostgreSQL 16 로컬 기동, Chromium+Playwright)

## 실행한 명령과 결과

### 1. 빌드/테스트
- `./gradlew compileJava` — 성공 (기존 realestate deprecation note만 존재)
- `./gradlew test` — **117 tests, failures 0, errors 0**
  - 최초 실행 시 `StockMarketApplicationTests.contextLoads` 1건이 DB 부재로 실패 →
    로컬 PostgreSQL 16 기동 후 재실행에서 전체 통과
- `node --check` — salary.js / app.js / api.js 문법 통과
- salary.html — 태그 균형·template 구조 검사 통과 (파서 검증 스크립트)

### 2. 로컬 PostgreSQL + 실서버 기동 검증
- PostgreSQL 16 기동, `stocks` DB + root 계정 생성 → `./gradlew bootRun --spring.profiles.active=dev`
- ddl-auto=update로 `spending_item_set`·`spending_item` 테이블, `spending_config.budget`
  (numeric(15,0) nullable) 자동 생성 확인 (`\d spending_config`)

### 3. API E2E 시나리오 (userId=777)
- 2026-07 일괄 저장: 항목 보유 카테고리 amount=항목 합계 파생(식비 320,000=180k+90k+50k),
  total 2,940,000, savingsRatio 0.4574 ✓
- 2026-08 조회(레코드 없음): income/항목/카테고리 모두 2026-07 상속 표기,
  `previous`=7월 요약 ✓
- 2026-08 일괄 저장(식비만 변경): 식비만 8월 신규 config, 나머지 7월 유지(NOOP),
  항목 세트는 8월 스냅샷 신규 ✓
- DB 검증: income 1행(동일값 재저장 NOOP), config 9행(7월 8 + 8월 식비 1),
  item_set 2행(각 9항목) ✓
- 동일 payload 재저장 → 레코드 수 불변 (9/2/1 → 9/2/1) ✓
- trend: points에 categoryTotals 포함, 합계·저축률 정합 ✓ / months 목록 ✓
- 검증 오류: 음수 income 400, 미래 월 400("미래 월은 입력할 수 없습니다"),
  잘못된 enum body 400(MALFORMED_REQUEST_BODY — 핸들러 보강 후) ✓
- 레거시 개별 upsert: income NOOP 응답 유지, spending 신규 레코드 생성 시 상속 budget 보존 ✓

### 4. 브라우저 E2E (Playwright + Chromium 1560px)
- 컨테이너 프록시가 브라우저의 CDN(tailwind/jsdelivr) 접근을 차단 → npm 동일 패키지
  (alpinejs, @alpinejs/collapse, chart.js, marked, sortablejs, @tailwindcss/browser)를
  라우트 인터셉트로 대체 주입해 검증
- 확인 항목: 제목/월 라벨, 저축률 카드(45.7% +0.0%p), 배분됨 2,972,000, 카테고리 8행,
  추이 3모드 SVG 렌더, 모두 펼치기(항목 9행), 항목 추가→이름·금액 입력→dirty 표시
  ("저장 안 된 변경 있음"/"변경 저장"), 저장 후 식비 4개 항목 반영, 예산 열 토글,
  지난달 대비 카드, 인사이트 3장('교통 예산 초과 2,000원' 등), 홈(#home) 렌더 회귀 없음
- 스크린샷: 전체 화면이 목업 레이아웃·색·타이포와 일치함을 확인
- JS 오류: salary 관련 0건 (프로필/관심지표 오류는 존재하지 않는 테스트 사용자 777로 인한
  타 컴포넌트 사정)

### 5. 컴포넌트 로직 시뮬레이션 (Node)
- 편집 버퍼 파생 계산(배분/미배분/저축률/고정·변동/리본/예산 diff/전월 대비/인사이트),
  SVG 3모드 생성, 빈 데이터 방어(전월 없음 '—', 포인트 <2 안내), 입력 파싱 — 전부 통과

## 미검증 항목
- 운영 프로파일(prod) 기동 및 실데이터 마이그레이션 영향 — 기존 데이터는 additive 컬럼/테이블이라
  이관 불필요하지만, 운영 반영 후 첫 조회 확인 권장
- 실사용자 계정으로 홈 우측 패널 스택 바 표시 (스키마 계약은 무파괴 확인, 실계정 검증은 미수행)
- 모바일 소형 뷰포트 실기기 확인 (테이블은 min-width 840px + 가로 스크롤 처리)

---

## 범위 확장 검증 (커스텀 카테고리 + 목표 설정화)

### 실행한 명령과 결과
- `./gradlew compileJava` / `./gradlew test` — **117/117 통과** (로컬 PostgreSQL 기동 상태)
- ddl-auto로 `user_spending_category`·`user_salary_setting` 생성 확인
- **CHECK 제약 발견·해소**: 커스텀 code 저장 시
  `spending_config_category_check` 위반으로 409 → `salary_category_check_drop_2026_08_21.sql`
  적용 후 정상. pg_constraint 조회로 두 테이블 제약 실재 확인.
- API E2E (userId=777, 기존 enum 데이터 보유 상태):
  1. 시드 전 조회 — 기본 메타로 기존 데이터 표시(호환), savingTarget 기본 50
  2. 일괄 저장(커스텀 '구독' 추가 + 목표 55) — code 'U…' 발급·색 부여·항목 합계 53,000 파생,
     user_spending_category 시드 8종+커스텀 1종(savings/system/sort 정확)
  3. 커스텀 이름 변경(구독→구독료) 반영
  4. 저축 카테고리 누락 payload → 400
  5. 커스텀 삭제(payload 제외) → 라인에서 제거(비활성+0 레코드)
  6. 동명 재추가 → 기존 code 재활성(True) — 이력 연결
  7. 추이 `categories` 메타에 커스텀 포함, catTotals 동적 code
  8. 레거시 endpoint 알 수 없는 code → 400
  9. 재조회 시 savingTarget 55 유지
  10. 비활성+과거 월 금액 보유 카테고리 — 해당 월 조회 시 표시(active=false), 기록 없는 월은 미표시
- 브라우저 E2E: 커스텀 카테고리 렌더(이름 입력·색·배지), + 카테고리 → 행 추가·dirty,
  이름 '교육비' 입력·저장 후 유지, 목표 입력 55→60 변경 시 추이 목표선 '목표 60%' 즉시 반영,
  카테고리 × → 저장 후 제거 확인, salary 관련 JS 오류 0건, 전체 스크린샷 목업 일치

### 미검증 항목 (추가)
- 운영 DB에서 `salary_category_check_drop_2026_08_21.sql` 실행 — **배포 시 필수, 미실행 시
  커스텀 카테고리 저장이 409로 실패** (기본 8종만 쓰는 동안은 영향 없음)
