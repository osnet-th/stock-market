아래 절차를 순서대로 수행해줘.

## 1. 변경 사항 분석
- `git status`와 `git diff --staged`, `git diff`를 실행해서 현재 변경된 파일 목록과 내용을 파악해.
- 변경 사항이 없으면 "커밋할 변경 사항이 없습니다"라고 알려주고 종료해.

## 2. 설계 문서 확인
- 변경된 파일의 모듈/기능명을 기반으로 `.claude/designs/` 하위에서 관련 설계 문서를 검색해.
- 설계 문서가 있으면 읽어서 작업 리스트와 설계 제목을 파악해.
- 설계 문서가 없으면 이 단계를 건너뛰고 변경 내용만으로 커밋 메시지를 생성해.

## 3. 커밋 메시지 생성
- 변경 내용을 분석해서 적절한 커밋 메시지를 생성해.
- `$ARGUMENTS`가 있으면 커밋 메시지 힌트로 활용해.
- 최근 커밋 로그(`git log --oneline -10`)를 참고해서 프로젝트의 커밋 메시지 스타일을 따라.
- **설계 문서가 있는 경우** 커밋 메시지 형식:
  ```
  {type}({scope}): {설계 문서 제목 기반 간결한 설명}

  - {완료된 작업 1 (설계 문서 작업 리스트 기반)}
  - {완료된 작업 2}

  설계 문서: {설계 문서 상대 경로}

  Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>
  ```
- **설계 문서가 없는 경우** 커밋 메시지 형식:
  ```
  {type}({scope}): {간결한 설명}

  - {변경 사항 1}
  - {변경 사항 2}

  Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>
  ```
- type: feat, fix, refactor, docs, style, test, chore 중 적절한 것 선택
- scope: 변경된 모듈명 (hub, ground, lens 등) 또는 생략

## 4. 사용자 확인
- 변경된 파일 목록과 생성한 커밋 메시지를 보여주고 AskUserQuestion으로 확인을 받아.
- 사용자가 수정을 요청하면 반영해.

## 5. 스테이징 및 커밋
- 변경된 파일을 개별적으로 `git add` 해 (`.env`, `.env-local`, credentials 등 민감 파일 제외)
- 생성한 커밋 메시지로 커밋해.

## 6. 푸시
- 현재 브랜치를 원격에 푸시해.
- 리모트 트래킹이 없으면 `git push -u origin {브랜치명}`으로 설정해.

## 7. main 브랜치 머지 (main이 아닌 경우)
- 현재 브랜치가 `main`이 아니면 main 브랜치에 머지해.
- `git checkout main && git pull origin main`으로 main 최신화.
- `git merge {작업브랜치명}`으로 머지.
- 머지 충돌이 발생하면 사용자에게 알리고 중단해.
- 머지 성공 시 `git push origin main`으로 푸시.
- 머지 완료 후 결과를 보여줘.

## 주의사항
- 민감 파일(.env, .env-local, credentials, secrets 등)은 절대 커밋하지 마.
- `git push --force`는 절대 사용하지 마.
- main 브랜치에서는 머지 단계를 건너뛰고 푸시만 해.