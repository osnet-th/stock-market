# 설계 및 분석 문서 작성 가이드

이 파일은 `.claude/designs/` 및 `.claude/analyzes/` 하위의 설계 및 분석 문서 작성 규칙을 정의합니다.

---

## 작성 원칙: 실용적 균형

**목표**: 구현에 필요한 핵심 정보만 간결하게 제공

**필수 포함**:
- **작업 리스트** (최상단, 체크박스 형식)
- **구현 예시 링크** (별도 examples/ 파일로 분리)

**선택 포함** (필요시에만):
- **배경/개요** (왜 이 작업이 필요한지, 1~3문장)
- **핵심 결정사항** (중요한 설계 선택만, 3~5개 이내)
- **주의사항** (구현 시 반드시 알아야 할 것)

**금지**:
- ❌ 과도한 형식 (개요, 요구사항, 설계 결정사항, 검증 계획 등 모두 포함)
- ❌ 불필요한 섹션 (구현 시 참고하지 않는 내용)
- ❌ 중복 내용 (예시 코드에 이미 있는 내용 반복)
- ❌ 본문 500줄 초과 (예시 제외)

---

## 분석 문서 작성 규칙

### 디렉토리 구조

```
.claude/
├── analyzes/
│   └── {모듈명}/
│       └── {기능명}/
│           └── {기능명}.md
```

### 작성 규칙

- **분석 문서**: `.claude/analyzes/{모듈명}/{기능명}/{기능명}.md`
- **예시**:
  - ground 모듈의 connection-manager 분석 → `.claude/analyzes/ground/connection-manager/connection-manager.md`
  - tableau 모듈의 port-usability 분석 → `.claude/analyzes/tableau/port-usability/port-usability.md`

### 파일명 규칙

- **모듈명**: 축약형 사용 (ground, tableau, hub, lens 등)
- **기능명**: kebab-case 사용 (connection-manager, port-usability 등)

### 내용 규칙

- 분석 외 다른 응답 절대 금지
- **현재 상태**, **문제점**, **원인**, **해결 방안**을 명확히 기술
- 코드 위치 명시 (파일:라인)
- 간결하게 작성 (300줄 이내 권장)

### 구조 예시 (참고용, 유연하게 조정 가능)

```markdown
# {기능명} 분석

## 현재 상태
현재 코드/구조 설명

## 문제점
- 문제 1: 설명
- 문제 2: 설명

## 원인 분석
근본 원인 설명

## 해결 방안
### 방안 1 (권장)
설명 + 장단점

### 방안 2
설명 + 장단점

## 영향 범위
변경 대상 파일 목록

## 권장 사항
최종 추천 방안
```

---

## 설계 문서 작성 규칙

### 디렉토리 구조

```
.claude/
└── designs/
    └── {모듈명}/
        └── {기능명}/
            ├── {기능명}.md
            └── examples/
                └── {컴포넌트명}-example.md
```

### 작성 규칙

- **설계 문서**: `.claude/designs/{모듈명}/{기능명}/{기능명}.md`
- **코드 예시**: `.claude/designs/{모듈명}/{기능명}/examples/{컴포넌트명}-example.md`
- **예시**:
  - 설계: `.claude/designs/tableau/port-facade/port-facade.md`
  - 예시: `.claude/designs/tableau/port-facade/examples/tableau-query-facade-example.md`
  - 예시: `.claude/designs/tableau/port-facade/examples/port-config-example.md`

### 파일명 규칙

- **모듈명**: 축약형 사용 (ground, tableau, hub, lens 등)
- **기능명**: kebab-case 사용 (port-facade, connection-management 등)
- **컴포넌트명**: kebab-case 사용 (tableau-query-facade, port-config 등)

### 내용 규칙

- **본문 300~500줄 이내** (예시 코드 제외)
- **코드 예시는 절대 포함 금지**, 대신:
  - 구현 예시: `.claude/designs/{모듈명}/{기능명}/examples/{컴포넌트명}-example.md`
  - 설계 문서에서는 상대 경로로 참조: `[구현 예시](./examples/{컴포넌트명}-example.md)`
- **테스트 예시는 작성하지 않음** (명시적 요청 시에만 작성)
- 설계 외 다른 응답 절대 금지

### 구조 (유연하게 조정 가능)

**고정된 헤더를 강제하지 않음**. 설계 특성에 맞게 자유롭게 작성. 다음은 참고용 구조:

```markdown
# {기능명} 설계

## 작업 리스트 (필수)
- [ ] 작업 1
- [ ] 작업 2

## 배경 (선택)
왜 이 작업이 필요한지 간단히 (1~3문장)

## 핵심 결정 (선택)
- 결정 1: 이유
- 결정 2: 이유

## 구현 (필수)
### 클래스/파일명
위치: `패키지/파일.java`

[예시 코드](./examples/example.md)

## 주의사항 (선택)
- 구현 시 주의할 점 1
- 구현 시 주의할 점 2
```

**원칙**:
- 헤더 제목은 설계 특성에 맞게 자유롭게 작성
- 구현에 필요한 정보 중심
- 간결함 유지

### 작업 리스트 관리 규칙

- **작업 완료 시 체크 표시**: 설계 문서의 작업 리스트에서 작업을 완료하면 `- [ ]`를 `- [x]`로 변경하여 완료 처리
- **진행 상황 추적**: 작업 완료 후 남은 작업 리스트를 사용자에게 제시

### 기타 규칙

- **섹션 헤더에 순서 번호를 표시하지 않음** (예: "1. 개요" 대신 "## 개요" 사용)
- **불필요한 형식 제거**: 구현 시 참고하지 않는 섹션은 작성하지 않음

---

## 구조 예시: 실용적 균형

### 간결한 설계 문서 (권장)

```markdown
# Port Facade 설계

## 작업 리스트
- [ ] TableauQueryFacade 구현
- [ ] Bean 등록
- [ ] 테스트 작성

## 배경
Port 사용이 복잡 (6개 파라미터) → Facade로 단순화

## 핵심 결정
- Facade 위치: `domain/port` (계층 위반 없음)
- 메서드 네이밍: `query{Resource}`

## 구현
### TableauQueryFacade
위치: `domain/port/TableauQueryFacade.java`

[예시 코드](./examples/tableau-query-facade-example.md)

### Bean 등록
위치: `external/config/TableauPortConfig.java`

[예시 코드](./examples/port-config-example.md)

## 주의사항
- Facade는 순수 Java (Spring 의존성 없음)
- Port 인터페이스만 의존
```

**장점**:
- 핵심 정보만 포함
- 빠르게 파악 가능
- 구현에 바로 사용 가능

---

## 과도한 형식 예시 (지양)

```markdown
# Port Facade 설계

## 1. 개요
### 1.1 목적
### 1.2 범위

## 2. 요구사항
### 2.1 기능 요구사항
### 2.2 비기능 요구사항

## 3. 설계 결정사항
### 3.1 결정 1
### 3.2 결정 2
### 3.3 결정 3

## 4. 구현 가이드
### 4.1 클래스 설계
### 4.2 메서드 설계
### 4.3 의존성 설계

## 5. 검증 계획
### 5.1 단위 테스트
### 5.2 통합 테스트
### 5.3 회귀 테스트

## 6. 영향 범위
### 6.1 신규 파일
### 6.2 수정 파일
### 6.3 영향 없음

## 7. 참고 문서
...
```

**문제점**:
- 너무 길어서 핵심 파악 어려움
- 구현 시 참고하지 않는 내용 많음
- 형식에 치중, 실용성 떨어짐

---

## 코드 예시 파일 작성 규칙

### 위치
- `.claude/designs/{모듈명}/{기능명}/examples/{컴포넌트명}-example.md`

### 파일 분리 규칙

예시 코드가 여러 계층(domain, application, infrastructure, presentation)에 걸쳐 있을 경우, **계층별로 파일을 분리**한다.

```
examples/
├── domain-model-example.md           ← enum, 도메인 모델, 포트 인터페이스
├── infrastructure-parsing-example.md ← 파서, 클라이언트, 정규화, 내부 DTO
├── infrastructure-adapter-example.md ← 어댑터, 레지스트리, 설정, 예외 클래스
├── application-service-example.md    ← 애플리케이션 서비스
└── presentation-example.md           ← 컨트롤러, 응답 DTO
```

**분리 기준**:
- 계층(layer) 단위로 분리
- infrastructure처럼 클래스가 많은 계층은 역할별로 추가 분리 가능 (예: parsing / adapter)
- 단일 계층에 클래스가 1~2개만 있으면 별도 파일 불필요, 관련 계층과 합칠 수 있음

**파일명 규칙**: `{계층명}-{역할명}-example.md` (kebab-case)

### 내용
- **전체 구현 코드** 포함 가능 (길이 제한 없음)
- 주석으로 설명 추가
- 사용 예시 포함

### 예시

```markdown
# TableauQueryFacade 구현 예시

​```java
package com.bigxdata.dataworks.tableau.domain.port;

/**
 * Tableau Query Facade
 */
public class TableauQueryFacade {

    private final TableauClientPort port;

    public TableauQueryFacade(TableauClientPort port) {
        this.port = port;
    }

    /**
     * Projects 전체 조회
     */
    public List<ProjectResponseDto> queryProjects(Long connId) {
        ProjectListResponseDto response = port.execute(
            connId,
            ApiOperation.QUERY_PROJECTS,
            null,
            ProjectListResponseDto.class
        );
        return response.projects();
    }

    // ... 기타 메서드
}
​```

## 주요 특징
- 메서드 네이밍: `query{Resource}`
- 파라미터 단순화
- 타입 안전성 보장
```

---

## 문서 작성 체크리스트

### 설계 문서 작성 시
- [ ] 작업 리스트 포함 (체크박스 형식)
- [ ] 배경/핵심 결정 간결하게 작성 (필요시)
- [ ] 코드 예시는 별도 파일로 분리
- [ ] 본문 300~500줄 이내 유지
- [ ] 구현에 불필요한 섹션 제거

### 분석 문서 작성 시
- [ ] 현재 상태 명확히 기술
- [ ] 문제점과 원인 분석
- [ ] 해결 방안 제시 (권장 방안 명시)
- [ ] 코드 위치 명시 (파일:라인)
- [ ] 300줄 이내 권장

### 코드 예시 작성 시
- [ ] 전체 구현 코드 포함
- [ ] 주석으로 설명 추가
- [ ] 사용 예시 포함
- [ ] 길이 제한 없음

---

## 참고

- [CLAUDE.md](CLAUDE.md) - 전체 프로젝트 규칙
- [ARCHITECTURE.md](ARCHITECTURE.md) - 아키텍처 규칙