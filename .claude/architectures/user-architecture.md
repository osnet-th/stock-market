**User 도메인 모델 상세**:

**User (Aggregate Root)**:
- 책임: 사용자 상태/권한 관리, 회원가입 완료 처리, 연결된 OAuth 계정 ID 목록 관리
- 속성:
    - id (Long, PK)
    - name (String)
    - nickname (Nickname VO)
    - phoneNumber (PhoneNumber VO)
    - oauthAccountIds (List<Long>) - 연결된 OAuth 계정 ID 목록
    - status (UserStatus enum)
    - role (UserRole enum)
    - createdAt, deletedAt
- 주요 메서드:
    - completeSignup(name, nickname, phoneNumber)
    - isActive(), canAccessResource()
    - matchesForConnection(nickname, phoneNumber)
    - addOAuthAccount(oauthAccountId), removeOAuthAccount(oauthAccountId)
    - delete()

**OAuthAccount (Entity)**:
- 책임: OAuth 인증 정보 관리, 동일 계정 판별
- 속성:
    - id (Long, PK)
    - userId (Long, FK) - User ID 참조 (연관관계 없음)
    - provider (OAuthProvider enum)
    - issuer, subject, email
    - connectedAt
- 주요 메서드:
    - matches(provider, issuer, subject)
    - isSameProvider(provider)
    - connectToUser(userId)
- 제약: provider + issuer + subject 조합 unique

**OAuthConnectionService (Domain Service)**:
- 책임: User와 OAuthAccount 간 연결 관리, provider 중복 검증
- 주요 메서드:
    - canConnectProvider(user, provider)
    - connectOAuthAccount(user, oauthAccount)
    - hasProvider(user, provider)
    - disconnectOAuthAccount(user, oauthAccountId)
- 의존성: UserRepository, OAuthAccountRepository

**도메인 규칙**:
1. 닉네임은 시스템 전체에서 unique
2. 하나의 User는 같은 provider의 OAuth를 중복 연결 불가
3. SIGNING_USER 권한은 활성화 시 USER 권한으로 전환
4. DELETED 상태 사용자는 리소스 접근 불가
