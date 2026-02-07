뉴스 조회 작업 계획

뉴스 모델
- 뉴스 ID(Long)
- 뉴스 원본 url
- userid
- 제목
- 내용
- 발행일
- 저장일(데이터 생성일)
- 저장 목적(키워드 기반, 주식 기반 아래에 명시한 enum 사용)
- 검색으로 사용한 키워드(단일 키워드)

주식과 연관을 가질 필요 없음

뉴스 Entity 설계 (모델 기준)
- id: 식별자
- originalUrl: 뉴스 원본 URL (중복 기준), UNIQUE, 길이 500 VARCHAR 
- title: 제목 , 길이 500 VARCHAR
- userid (Long)
- content: 내용 (TEXT)
- publishedAt: 발행일
- createdAt: 저장일(데이터 생성일)
- purpose: 저장 목적 (KEYWORD, STOCK) VARCHAR 길이 100
- searchKeyword: 검색에 사용한 단일 키워드 (not null)

뉴스 조회
- Layered + DDD 기준으로 port(NewsSearchPort) 인터페이스는 domain 계층에 두고, 실제 구현은 infrastructure 계층에서 담당
- API 별로 응답 객체가 다를 수 있으나, 조회 전용 모델(예: NewsSearchResult)은 domain 계층에 두고 해당 모델로 통일한다
- port 구현은 요청하는 뉴스API에 따라서 추가 구현될 수 있으므로 확장 가능한 구조를 가져야 함
  - 실제 구현 API 는 추후에 구현 예정
- port 요청은 검색어와 시간을 입력받도록 한다
- 저장된 뉴스를 조회하는 기능도 필요 -> 저장 목적(KEYWORD, STOCK) 기준으로 분류 조회한다

저장된 뉴스 조회 Repository 구현 방향
- domain 계층에 조회용 repository 메서드 정의
- infrastructure 계층에서 JPA 구현
- 저장 목적(KEYWORD, STOCK)을 조건으로 조회하는 메서드 제공

뉴스 조회 유스케이스
- List<Port인터페이스> 로 모든 구현체에 인스턴스를 가져와서 처음 구현체부터 요청 진행
- API 무료 요청이 만료되는 등 에러가 발생하거나, 키워드 또는 주식 이름으로 조회했을 때 응답값이 없는 경우, 다음 구현체로 동일하게 요청
- 요청 응답값이 있으면 해당 값들을 NewsResultDto로 변환하여 List로 반환
- 조회 요청은 검색어를 입력받으며, LocalDateTime 기준(서버 타임존) 금일 00시 이후 발행된 뉴스만 가져오도록 해야함
- 해당 서비스는 조회만 담당하며 저장하는 기능은 일체 하지 않는다.

뉴스 저장 유스케이스(JPA 사용)
- 해당 유스케이스는 저장만하며 조회 로직은 일체 추가하지 않는다.
- 뉴스 저장 요청은 키워드 기반 또는 주식 기반인지 enum 값(KEYWORD, STOCK)을 파라미터로 받도록한다.
- 저장한 사용자의 id 값과 뉴스 정보들을 요청 받는다
    - 해당 값들은 DTO 로 입력 받는다.
- 저장은 단건 저장, 배치 저장이 있다
- 트랜잭션 정책
    - 단건 저장: 실패 시 전체 롤백
    - 배치 저장: 부분 성공 허용, 사용자 또는 청크 단위로 @Transactional 경계를 적용
- 배치 저장 결과는 성공/무시/실패 건수를 반환한다

뉴스 저장 구현
- 중복 저장 처리
    - 중복 기준: 원본 URL
    - DB 중복 무시는 JPA Repository에 Native Query로 `INSERT ... ON CONFLICT DO NOTHING` 전략 사용
    - 배치 저장에서 중복은 실패로 처리하지 않고 무시한다. 그 외 오류만 실패로 처리한다.
    - 배치 단위는 JPA 설정 값 중 batch-size 값 사용, 현재 1000
뉴스 조회 전용 모델 (NewsSearchResult)
- title
- url
- content
- publishedAt

뉴스 조회 응답 DTO (NewsResultDto)
- NewsSearchResult를 기반으로 조회 결과를 반환
- 다수의 뉴스 조회이므로 List로 반환
- 필드: title, url, content, publishedAt

저장된 뉴스 응답 DTO (NewsDto)
- 뉴스 모델 정보를 그대로 DTO로 구성하여 반환
- 필드: id, originalUrl, title, content, publishedAt, createdAt, purpose, searchKeyword


API 
