# fix: 매도 이력 수정 진입점 보완

## 배경

매도 이력 수정 기능은 이미 상세 모달 안에 존재하지만, 매도 이력 목록에는 수정 버튼이 보이지 않는다. 사용자가 기존 매도 이력을 정정하려 할 때 진입점을 찾기 어렵다.

## 범위

- 매도 이력 목록 UI에 `수정`/`상세` 액션을 추가한다.
- `수정` 액션은 기존 수정 모달 상태를 바로 열도록 연결한다.
- 기존 계산식, API, 엔티티, DB 구조는 변경하지 않는다.

## 작업 목록

- [x] 매도 이력 목록 row에 `수정`/`상세` 버튼 추가
- [x] `수정` 버튼 클릭 시 row 클릭 이벤트와 분리
- [x] 기존 edit form 생성 로직을 재사용하는 `openSaleEditModal(history)` 추가
- [x] JS 문법 검사 실행
- [x] diff whitespace 검사 실행
- [x] 전체 테스트 실행
- [x] 브라우저에서 매도 이력 row의 `수정`/`상세` 버튼 렌더링 확인

## 검증

- `node --check src/main/resources/static/js/components/portfolio.js`
- `git diff --check`
- `./gradlew test`
- 브라우저 확인: `http://localhost:8080/#portfolio` 매도 이력 탭에서 row별 `수정`/`상세` 버튼 표시

## 리스크

- 버튼이 row 내부에 있으므로 이벤트 버블링이 발생하면 상세 모달만 열릴 수 있다. `@click.stop`으로 행 클릭 이벤트와 분리한다.
- 편집 상태 초기화가 기존 상세 모달과 달라지면 중복 로직이 생길 수 있다. 기존 `createSaleEditForm(history)`를 재사용해 입력값 구성 방식을 유지한다.
