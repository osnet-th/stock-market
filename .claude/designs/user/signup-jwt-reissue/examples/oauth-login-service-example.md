# OAuthLoginService.completeSignup() 수정 예시

```java
// 변경 전
public void completeSignup(SignupCompleteRequest request) {
    User user = userRepository.findById(request.userId())
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

    Nickname nickname = new Nickname(request.nickname());
    PhoneNumber phoneNumber = new PhoneNumber(request.phoneNumber());

    if (userRepository.existsByNickname(nickname)) {
        throw new RuntimeException("이미 사용중인 닉네임입니다.");
    }

    user.completeSignup(request.name(), nickname, phoneNumber);
    userRepository.save(user);
}

// 변경 후
public SignupCompleteResponse completeSignup(SignupCompleteRequest request) {
    User user = userRepository.findById(request.userId())
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

    Nickname nickname = new Nickname(request.nickname());
    PhoneNumber phoneNumber = new PhoneNumber(request.phoneNumber());

    if (userRepository.existsByNickname(nickname)) {
        throw new RuntimeException("이미 사용중인 닉네임입니다.");
    }

    user.completeSignup(request.name(), nickname, phoneNumber);
    userRepository.save(user);

    // role이 USER로 변경된 새 Access Token 발급
    String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getRole());

    return new SignupCompleteResponse(accessToken);
}
```