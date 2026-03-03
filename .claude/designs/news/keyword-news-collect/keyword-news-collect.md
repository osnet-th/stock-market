# 키워드 뉴스 즉시 수집 설계

## 작업 리스트

- [ ] KeywordNewsBatchService 인터페이스에 단건 수집 메서드 추가
- [ ] KeywordNewsBatchServiceImpl에 단건 수집 구현
- [ ] NewsController에 즉시 수집 API 추가
- [ ] 즉시 수집 응답 DTO 생성
- [ ] api.js에 즉시 수집 API 함수 추가
- [ ] app.js에 수집 메서드 + 상태 추가
- [ ] index.html 키워드 목록에 수집 버튼 추가

## 배경

키워드 뉴스는 스케줄러(`KeywordNewsBatchScheduler`)로 배치 수집됨. 새로 등록된 키워드는 다음 배치까지 뉴스가 없어 사용자 경험이 좋지 않음. 키워드별 "뉴스 수집" 버튼으로 즉시 수집을 트리거.

## 핵심 결정

- **기존 로직 재사용**: `KeywordNewsBatchServiceImpl`의 검색 → 필터링 → 저장 흐름을 단건 키워드용으로 분리
- **동기 처리**: 외부 API 호출이 포함되지만, 단건 키워드 수집은 수 초 내 완료 가능하므로 동기 처리
- **응답**: 수집 결과(성공/중복/실패 건수) 반환 → 프론트에서 토스트 메시지 표시
- **API**: `POST /api/news/collect` (body: `{ keyword, userId, region }`)

## 구현

### KeywordNewsBatchService (인터페이스)
위치: `news/application/KeywordNewsBatchService.java`
- `NewsBatchSaveResult collectByKeyword(String keyword, Long userId, Region region)` 추가

### KeywordNewsBatchServiceImpl (구현)
위치: `news/application/KeywordNewsBatchServiceImpl.java`
- `collectByKeyword`: 단건 키워드에 대해 검색 → 중복 필터 → 저장
- 기존 `executeKeywordNewsBatch`의 내부 로직을 재사용

[예시 코드](./examples/application-example.md)

### NewsCollectResponse (presentation DTO)
위치: `news/presentation/dto/NewsCollectResponse.java`

### NewsController
위치: `news/presentation/NewsController.java`
- `POST /api/news/collect` 엔드포인트 추가

[예시 코드](./examples/presentation-example.md)

### 프론트엔드
- `api.js`: `collectNewsByKeyword(keyword, userId, region)` 추가
- `app.js`: `collectNews(kw)` 메서드 + `news.collecting` 상태 추가
- `index.html`: 키워드 항목에 "수집" 버튼 추가 (활성 키워드만 표시)

[예시 코드](./examples/frontend-example.md)

## 주의사항

- 수집 중 버튼 비활성화 (중복 클릭 방지)
- 수집 완료 후 선택된 키워드와 동일하면 뉴스 목록 자동 갱신
- 외부 API 타임아웃(연결 3초, 읽기 5초)은 기존 설정 그대로 적용