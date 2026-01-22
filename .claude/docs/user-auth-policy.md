## 사용자 정보
1. id(auto increment)
2. 이름
3. 닉네임(중복 불가)
4. 연결된 OAuth 계정 정보 (별도 테이블, user_id로 참조)
5. 전화번호
6. 사용자 상태(활성화, 삭제됨)
7. 권한(USER, SIGNING_USER)
8. 생성 날짜
9. 삭제 날짜

## 사용자 로그인 규칙
- 한명의 사용자는 kakao 와 google oauth 를 통해 모두 로그인이 가능하다.
- oauth 계정 판별은 provider + issuer + subject 기준으로 한다.
- 로그인 시 provider + issuer + subject 로 조회하여 일치하는 oauth 계정이 있으면 해당 사용자로 로그인한다.
- 일치하는 oauth 계정이 없으면 SIGNING_USER 권한으로 가입중 상태의 사용자를 생성한다.
- 가입중 상태 사용자는 "신규가입" 또는 "기존계정연결" 중 하나를 선택한다.
  - **신규가입 선택 시**:
    - 이름, 닉네임, 전화번호를 입력받는다.
    - 닉네임 중복을 검사한다.
    - 중복이 없으면 USER 권한의 활성화 상태로 전환한다.
  - **기존계정연결 선택 시**:
    - 닉네임과 전화번호를 입력받는다.
    - 닉네임으로 사용자를 조회하고 전화번호 일치 여부를 확인한다.
    - 일치하는 사용자가 있으면 해당 사용자에 oauth 계정을 추가하고 연결한다.
    - 일치하는 사용자가 없으면 신규가입 플로우로 전환한다.
- 가입중 상태에서 로그아웃 후 재로그인 시, provider + issuer + subject로 조회되어 다시 회원정보 입력 화면으로 이동한다.
- oauth 정보는 별도의 테이블로 관리하고, 연결된 사용자 정보(user_id)를 FK로 갖는다.
  - oauth 정보에는 provider, issuer, subject, 이메일, 연결 시각을 포함한다.

## 접근 권한
- 모든 사용자는 반드시 활성화 상태여야만 다른 리소스에 접근이 가능하다.
- 가입중은 권한으로 관리한다.
  - 가입중인 권한을 가진 사용자는 가입에 대한 리소스에는 접근이 가능하다.

---

## 도메인 모델 구성

### User (Aggregate Root)
**책임**:
- 사용자 상태 및 권한 관리
- 회원 가입 완료 처리
- 활성화/삭제 상태 전환
- 연결된 OAuth 계정 ID 목록 관리

**속성**:
- id (Long, PK)
- name (String) - 이름
- nickname (String, unique) - 닉네임
- phoneNumber (String) - 전화번호
- oauthAccountIds (List<Long>) - 연결된 OAuth 계정 ID 목록
- status (UserStatus enum) - ACTIVE, DELETED
- role (UserRole enum) - USER, SIGNING_USER
- createdAt (LocalDateTime)
- deletedAt (LocalDateTime, nullable)

**주요 도메인 메서드**:
- `completeSignup(name, nickname, phoneNumber)`: 가입 완료 처리
- `isActive()`: 활성화 상태 확인
- `canAccessResource()`: 리소스 접근 가능 여부
- `matchesForConnection(nickname, phoneNumber)`: 계정 연결 매칭 확인
- `addOAuthAccount(oauthAccountId)`: OAuth 계정 ID 추가
- `removeOAuthAccount(oauthAccountId)`: OAuth 계정 ID 제거
- `delete()`: 사용자 삭제 처리

### OAuthAccount (Entity)
**책임**:
- OAuth 인증 정보 관리
- 동일 계정 판별 (issuer + subject)
- 사용자와의 연결 관계 유지

**속성**:
- id (Long, PK)
- userId (Long, FK) - User ID 참조
- provider (OAuthProvider enum) - GOOGLE, KAKAO
- issuer (String) - OAuth issuer
- subject (String) - OAuth subject
- email (String) - OAuth 이메일
- connectedAt (LocalDateTime) - 연결 시각

**주요 도메인 메서드**:
- `matches(provider, issuer, subject)`: provider + issuer + subject 일치 여부
- `isSameProvider(provider)`: 동일 provider 확인
- `connectToUser(userId)`: 사용자 연결

**제약 조건**:
- provider + issuer + subject 조합은 unique
- User와 연관관계 없이 userId (Long)로만 참조

### Value Objects

**Nickname**:
- 닉네임 유효성 검증 (길이 2~20자, 영어/한글/숫자만 허용)
- 중복 검사는 Repository 레이어에서 처리

**PhoneNumber**:
- 전화번호 형식 검증
- 정규화 처리 (하이픈 제거 등)

**OAuthIdentifier**:
- provider + issuer + subject 조합
- 동등성 비교 로직

### Domain Services

**OAuthConnectionService**:
**책임**:
- User와 OAuthAccount 간 연결 관리
- OAuth provider 중복 연결 검증
- 여러 Aggregate 간 협력 로직 처리

**주요 메서드**:
- `canConnectProvider(user, provider)`: 해당 provider 연결 가능 여부 확인
  - user의 oauthAccountIds로 OAuthAccount 조회
  - 동일 provider가 이미 존재하는지 검증
- `connectOAuthAccount(user, oauthAccount)`: OAuth 계정 연결
  - provider 중복 검증
  - oauthAccount에 userId 설정
  - user에 oauthAccountId 추가
- `hasProvider(user, provider)`: 특정 provider 보유 여부 확인
  - user의 oauthAccountIds로 OAuthAccount 조회
  - provider 존재 여부 반환
- `disconnectOAuthAccount(user, oauthAccountId)`: OAuth 계정 연결 해제
  - user에서 oauthAccountId 제거
  - oauthAccount의 userId 해제

**의존성**:
- UserRepository: User 조회 및 저장
- OAuthAccountRepository: OAuthAccount 조회 및 저장

### 도메인 관계 및 규칙

**관계 설계**:
- User ← OAuthAccount: ID 기반 참조 (연관관계 없음)
- User는 여러 OAuthAccount를 가질 수 있음 (조회는 Repository를 통해)

**도메인 규칙**:
1. 닉네임은 시스템 전체에서 unique 해야 함
2. 하나의 User는 같은 provider의 OAuth를 중복 연결할 수 없음
3. SIGNING_USER 권한은 ACTIVE 상태로 전환 시 USER 권한으로 변경
4. DELETED 상태의 사용자는 어떤 리소스에도 접근 불가 
