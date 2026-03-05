# AuthController.completeSignup() 수정 예시

```java
// 변경 전
@PostMapping("/signup")
public ResponseEntity<Void> completeSignup(@RequestBody SignupCompleteRequest request) {
    oauthLoginService.completeSignup(request);
    return ResponseEntity.noContent().build();
}

// 변경 후
@PostMapping("/signup")
public ResponseEntity<SignupCompleteResponse> completeSignup(@RequestBody SignupCompleteRequest request) {
    SignupCompleteResponse response = oauthLoginService.completeSignup(request);
    return ResponseEntity.ok(response);
}
```