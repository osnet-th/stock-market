---
title: "feat: 포트폴리오 마감 메일 알림"
type: feat
status: active
date: 2026-03-27
origin: docs/brainstorms/2026-03-26-portfolio-email-notification-brainstorm.md
---

# feat: 포트폴리오 마감 메일 알림

## Enhancement Summary

**Deepened on:** 2026-03-27
**Sections enhanced:** 5
**Research agents used:** Spring Mail 베스트 프랙티스, 아키텍처 리뷰, 보안 리뷰, 성능 리뷰

### Key Improvements
1. **서비스 분리**: PortfolioEvaluationService(평가금액 계산) + PortfolioNotificationService(알림 오케스트레이션) 분리로 크로스 도메인 결합 감소
2. **고유 종목 일괄 조회**: O(users×stocks) → O(distinct stocks) 외부 API 호출 최적화
3. **보안 강화**: OAuthAccount.email CRLF 인젝션 방지, HTML 이스케이프, `starttls.required: true`
4. **NotificationRequest에 recipientEmail 추가**: infrastructure 어댑터가 OAuthAccountRepository에 의존하지 않도록 경계 수정

### New Considerations Discovered
- Gmail SMTP 기본 timeout이 무한이므로 connectiontimeout/timeout/writetimeout 필수 설정
- HTML 이메일은 table 기반 레이아웃 + inline CSS만 사용 (Outlook 호환)
- `@Scheduled`는 기본 단일 스레드 — 스레드 풀 확장 필요
- 한국 공휴일은 cron으로 처리 불가 — 별도 체크 로직 필요

---

## Overview

본장 마감(15:30) 후 알림을 신청한 사용자에게 포트폴리오 평가 결과를 이메일(Gmail SMTP)로 발송하는 기능. 각 주식별 현재가/평가금액과 전체 평가금액을 HTML 테이블로 구성하여 발송한다.

## Problem Statement / Motivation

- 사용자가 장 마감 후 포트폴리오 평가 결과를 별도 접속 없이 확인할 수 없음
- GitHub Issue: #14

## Key Decisions (from brainstorm)

| # | 결정 사항 | 선택 | 이유 |
|---|----------|------|------|
| 1 | 알림 신청 | 포트폴리오 페이지 토글 버튼 | User 모델에 notificationEnabled 추가 |
| 2 | 발송 시점 | 본장 마감 후 15:30 1회 | @Scheduled cron |
| 3 | SMTP | Gmail SMTP + 앱 비밀번호 | 무료, 일 500통 충분 |
| 4 | 메일 형식 | HTML 테이블 | 주식명, 수량, 현재가, 평가금액, 손익률 |
| 5 | 수신 주소 | OAuthAccount.email | 카카오 OAuth에 등록된 이메일 |
| 6 | 평가금액 계산 | 서버에서 StockPriceService 사용 | 현재가 벌크 조회 후 계산 |

(see brainstorm: docs/brainstorms/2026-03-26-portfolio-email-notification-brainstorm.md)

## Technical Approach

### Architecture

서비스를 **평가금액 계산**과 **알림 오케스트레이션**으로 분리하여 크로스 도메인 결합을 최소화한다.

```
[Scheduler] PortfolioNotificationScheduler (@Scheduled 15:30 MON-FRI)
       ↓
[Application] PortfolioNotificationService (notification 도메인)
       ↓  (1) 알림 대상 사용자 조회 (UserRepository)
       ↓  (2) 사용자별 이메일 조회 (OAuthAccountRepository)
       ↓  (3) 포트폴리오 평가 위임
       ↓
[Application] PortfolioEvaluationService (portfolio 도메인)
       ↓  (1) 전체 대상자 포트폴리오 일괄 조회
       ↓  (2) 고유 종목 추출 → StockPriceService 1회 벌크 조회
       ↓  (3) 사용자별 평가금액 계산 → DTO 반환
       ↓
[Infrastructure] PortfolioReportRenderer (HTML 생성)
       ↓
[Infrastructure] EmailNotificationService (JavaMailSender)
       ↓
[External] Gmail SMTP
```

> **Research Insight — 아키텍처 리뷰:**
> - PortfolioNotificationService가 4개 도메인(user, portfolio, stock, notification)에 의존하면 god class 위험
> - 평가금액 계산을 PortfolioEvaluationService로 분리하면 각 서비스 의존성 2~3개로 감소
> - HTML 렌더링은 infrastructure 레이어 책임 — application 서비스에서 포맷 로직 분리

### Implementation Phases

#### Phase 1: 인프라 설정 + User 모델 변경

**1-1. Gmail SMTP 설정 (application.yml)**

```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: ${GMAIL_USERNAME}
    password: ${GMAIL_APP_PASSWORD}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
            required: true
          connectiontimeout: 5000
          timeout: 3000
          writetimeout: 5000
```

파일: `src/main/resources/application.yml`

> **Research Insight — Spring Mail:**
> - `starttls.required: true` 필수 — 없으면 MITM 공격 시 평문 fallback 위험
> - timeout 미설정 시 기본값이 **무한** — 응답 없는 SMTP 서버에 스레드가 영구 블로킹
> - credentials는 환경변수로 관리, 전용 발송 계정 분리 권장

**1-2. User 도메인에 notificationEnabled 추가**

- `User.java` — `boolean notificationEnabled` 필드 + `enableNotification()`, `disableNotification()` 메서드
- `UserEntity.java` — `@Column(name = "notification_enabled", nullable = false) boolean notificationEnabled` (기본값 false)
- `UserMapper` — 매핑 추가
- DDL은 `ddl-auto: update`로 자동 생성

파일: `user/domain/model/User.java`, `user/infrastructure/persistence/UserEntity.java`

**1-3. UserRepository에 알림 대상자 조회 메서드 추가**

- `List<User> findByNotificationEnabled(boolean enabled)` — 알림 활성화 사용자 목록 조회

파일: `user/domain/repository/UserRepository.java`, `user/infrastructure/persistence/UserRepositoryImpl.java`

**1-4. NotificationRequest에 recipientEmail 필드 추가**

- 기존: `userId`, `title`, `message`
- 추가: `String recipientEmail`
- application 서비스에서 이메일을 조회하여 Request에 포함 → infrastructure 어댑터는 이메일을 그대로 사용

파일: `notification/domain/NotificationRequest.java`

> **Research Insight — 아키텍처:**
> EmailNotificationService(infrastructure)가 OAuthAccountRepository에 의존하면 cross-domain 위반.
> application 서비스에서 이메일을 resolve하여 NotificationRequest에 담아 전달하는 것이 올바른 경계.

#### Phase 2: 알림 토글 API

**2-1. UserProfileService에 알림 토글 메서드 추가**

- `toggleNotification(Long userId, boolean enabled)` — User 조회 → `enableNotification()` 또는 `disableNotification()` → 저장

**2-2. UserProfileController에 엔드포인트 추가**

- `PATCH /api/users/me/notification` — `{ "enabled": true/false }`
- userId는 `SecurityContextHolder`에서 추출 (Authorization 헤더 재파싱 금지)

**2-3. UserProfileResponse에 notificationEnabled 필드 추가**

파일: `user/application/UserProfileService.java`, `user/presentation/UserProfileController.java`, `user/application/dto/UserProfileResponse.java`

> **Research Insight — 보안:**
> 기존 UserProfileController가 Authorization 헤더를 수동 파싱하는데, 새 엔드포인트는 `SecurityContextHolder.getContext().getAuthentication().getPrincipal()`에서 userId를 추출해야 함 (IDOR 방지)

#### Phase 3: 포트폴리오 평가 서비스

**3-1. PortfolioEvaluationService 생성 (portfolio 도메인)**

- `evaluatePortfolios(List<Long> userIds)` — 전체 대상자 포트폴리오 일괄 평가
- 흐름:
  1. `portfolioItemRepository.findByUserIdIn(userIds)` — 전체 포트폴리오 일괄 조회 (N+1 방지)
  2. STOCK 항목에서 고유 종목 코드 추출 (`Set<String> distinctStockCodes`)
  3. `StockPriceService.getPrices()` — 고유 종목에 대해 **1회만** 벌크 조회
  4. `Map<String, StockPriceResponse> priceMap` 구성
  5. 사용자별 그룹핑 → 평가금액 계산 → `Map<Long, PortfolioEvaluation>` 반환

파일: `portfolio/application/PortfolioEvaluationService.java`

> **Research Insight — 성능:**
> - 10 users × 5 stocks = 50 API 호출 → 고유 종목 20개면 20 호출로 감소
> - N+1 DB 쿼리도 1+N → 2건으로 감소 (users + portfolio items)
> - `findByUserIdIn(List<Long>)` 메서드를 PortfolioItemRepository에 추가 필요

**3-2. PortfolioItemRepository에 배치 조회 메서드 추가**

- `List<PortfolioItem> findByUserIdIn(List<Long> userIds)`

파일: `portfolio/domain/repository/PortfolioItemRepository.java`

#### Phase 4: 메일 발송 서비스

**4-1. PortfolioReportRenderer 생성 (infrastructure)**

- HTML 테이블 생성 담당
- `String renderReport(PortfolioEvaluation evaluation, LocalDate date)` — 평가 DTO를 HTML 문자열로 변환
- **모든 사용자 입력 데이터(itemName, memo)를 HTML 이스케이프** (`HtmlUtils.htmlEscape()`)
- table 기반 레이아웃 + inline CSS (Outlook 호환)

파일: `notification/infrastructure/email/PortfolioReportRenderer.java`

> **Research Insight — 보안:**
> - `itemName`에 `<script>` 등 주입 가능 — 반드시 `HtmlUtils.htmlEscape()` 적용
> - HTML 이메일은 `<table>` + inline style만 사용 (Gmail은 `<style>` 블록 strip)
> - 안전한 폰트: `Arial, Helvetica, sans-serif`
> - `role="presentation"`을 table에 추가 (스크린 리더 접근성)

**4-2. EmailNotificationService 구현**

기존 스켈레톤에 실제 구현 추가:
- `JavaMailSender` 필드 주입
- `sendNotification(NotificationRequest request)` — `recipientEmail`로 MimeMessage 생성 → HTML 본문 → 발송
- `MimeMessageHelper` 생성 시 `"UTF-8"` charset 명시
- 이메일 없는 사용자는 로그 경고 후 스킵
- BigxLogger 사용 (기존 slf4j Logger를 BigxLogger로 수정)

파일: `notification/infrastructure/email/EmailNotificationService.java`

**4-3. PortfolioNotificationService 생성 (notification 도메인)**

알림 오케스트레이션 담당:
- `int sendMarketCloseNotifications()` 메서드
- 흐름:
  1. `userRepository.findByNotificationEnabled(true)` — 대상자 조회
  2. 각 사용자별 OAuthAccount에서 이메일 조회 (이메일 없으면 스킵)
  3. `portfolioEvaluationService.evaluatePortfolios(userIds)` — 일괄 평가
  4. 사용자별:
     a. `reportRenderer.renderReport(evaluation, today)` — HTML 생성
     b. `NotificationRequest` 생성 (recipientEmail 포함)
     c. `notificationService.sendNotification(request)` — 발송 (try-catch로 격리)
  5. 성공/실패 건수 반환

파일: `notification/application/PortfolioNotificationService.java`

> **Research Insight — 에러 처리:**
> - `MailAuthenticationException` — 재시도 무의미 (앱 비밀번호 만료). 즉시 로그 + 알림
> - `MailSendException` — 네트워크 일시 장애. 재시도 대상 (Spring Retry `@Retryable` 고려)
> - 각 사용자 발송을 개별 try-catch로 감싸서 한 사용자 실패가 다른 사용자에 영향 없도록

#### Phase 5: 스케줄러

**5-1. PortfolioNotificationScheduler 생성**

- `@Component` + `@RequiredArgsConstructor`
- `@Scheduled(cron = "${batch.schedule.portfolio-notification-cron:0 30 15 * * MON-FRI}", zone = "Asia/Seoul")`
- try-catch로 에러 처리, BigxLogger 로그 기록

파일: `notification/infrastructure/scheduler/PortfolioNotificationScheduler.java`

> **Research Insight — 스케줄러:**
> - `zone = "Asia/Seoul"` 명시 — 서버 timezone과 무관하게 한국 시간 기준 실행
> - 한국 공휴일은 cron으로 처리 불가 — 실행 시점에 별도 공휴일 체크 필요 (추후 검토)
> - `@Scheduled`는 기본 단일 스레드 — 이메일 발송이 길어지면 다른 스케줄러 차단

#### Phase 6: 프론트엔드

**6-1. 포트폴리오 페이지에 알림 토글 버튼 추가**

- 포트폴리오 헤더 영역에 '마감 알림' 토글 스위치
- `PATCH /api/users/me/notification` API 호출
- 현재 상태는 `UserProfileResponse.notificationEnabled`에서 가져옴

**6-2. api.js에 알림 토글 API 추가**

- `toggleNotification(enabled)` — `PATCH /api/users/me/notification`

## System-Wide Impact

### 에러 처리 전략

| 시나리오 | 처리 |
|---------|------|
| 이메일 없는 사용자 | 로그 경고 후 스킵, 다음 사용자 계속 |
| 현재가 조회 실패 (특정 종목) | 해당 종목은 "조회 불가"로 표시, 나머지 정상 발송 |
| SMTP 발송 실패 (MailSendException) | 로그 에러 후 스킵, 다음 사용자 계속 |
| SMTP 인증 실패 (MailAuthenticationException) | 전체 배치 중단, 긴급 로그 |
| 포트폴리오 없는 사용자 | 스킵 (주식이 없으면 발송할 내용 없음) |

### 보안 체크리스트

- [ ] OAuthAccount.email에 CRLF 문자 검증 추가 (이메일 헤더 인젝션 방지)
- [ ] HTML 이메일 내 사용자 입력 데이터 `HtmlUtils.htmlEscape()` 적용
- [ ] SMTP `starttls.required: true` 설정
- [ ] 새 엔드포인트는 `SecurityContextHolder`에서 userId 추출
- [ ] 메일 제목에 금액 정보 미포함 (잠금화면 미리보기 노출 방지)

### 성능 최적화

- **고유 종목 일괄 조회**: O(users×stocks) → O(distinct stocks)
- **N+1 쿼리 제거**: `findByUserIdIn()` 배치 조회
- **스케줄러 스레드 격리**: `zone = "Asia/Seoul"` + 기존 스케줄러와 독립 실행

## Acceptance Criteria

### Functional Requirements

- [ ] User 모델에 notificationEnabled 필드 추가 (기본값 false)
- [ ] 포트폴리오 페이지에서 알림 토글 ON/OFF 가능
- [ ] 평일 15:30에 알림 활성화 사용자에게 메일 발송
- [ ] 메일에 주식별 현재가/평가금액 HTML 테이블 포함
- [ ] 메일에 전체 포트폴리오 총 평가금액 포함
- [ ] OAuthAccount.email로 수신 주소 조회
- [ ] 이메일 없는 사용자는 스킵
- [ ] 발송 실패해도 다른 사용자 발송에 영향 없음
- [ ] Gmail SMTP 설정은 환경변수로 관리
- [ ] 사용자 입력 데이터 HTML 이스케이프

### Non-Functional Requirements

- [ ] `@Scheduled` cron 표현식 외부 설정 가능 + `zone = "Asia/Seoul"`
- [ ] BigxLogger 사용 (프로젝트 로깅 규칙)
- [ ] SMTP timeout 설정 (connectiontimeout: 5000, timeout: 3000, writetimeout: 5000)
- [ ] `starttls.required: true` 필수

## Dependencies & Risks

| 리스크 | 영향 | 완화 방안 |
|--------|------|----------|
| Gmail 일 500통 제한 | 사용자 500명 초과 시 발송 불가 | 개인 프로젝트 규모에서 충분 |
| Gmail 앱 비밀번호 필요 | 2FA + 앱 비밀번호 생성 필요 | 환경변수로 관리, 전용 계정 분리 |
| 현재가 API 장 마감 후 지연 | 마감 직후 가격 미반영 가능 | 15:30 발송으로 충분한 여유 |
| 이메일 없는 OAuth 계정 | 카카오에서 이메일 미제공 가능 | 스킵 처리 + 로그 |
| HTML 인젝션 | itemName에 악성 HTML 삽입 가능 | HtmlUtils.htmlEscape() 적용 |
| 이메일 헤더 인젝션 | OAuth email에 CRLF 삽입 가능 | 이메일 포맷 검증 + CRLF 거부 |
| 스케줄러 단일 스레드 블로킹 | 이메일 발송이 길면 다른 스케줄러 차단 | 스레드 풀 확장 검토 |

## Sources & References

### Origin

- **Brainstorm document:** [docs/brainstorms/2026-03-26-portfolio-email-notification-brainstorm.md](docs/brainstorms/2026-03-26-portfolio-email-notification-brainstorm.md)
  - Key decisions: Gmail SMTP, 15:30 발송, HTML 테이블 형식, 포트폴리오 토글

### Internal References

- 스케줄러 패턴: `economics/infrastructure/scheduler/EcosIndicatorBatchScheduler.java`
- NotificationService 인터페이스: `notification/domain/NotificationService.java`
- EmailNotificationService 스켈레톤: `notification/infrastructure/email/EmailNotificationService.java`
- StockPriceService 벌크 조회: `stock/application/StockPriceService.java`
- OAuthAccount 이메일: `user/domain/model/OAuthAccount.java`
- User 도메인: `user/domain/model/User.java`

### External References

- Spring Boot Email: https://docs.spring.io/spring-boot/3.3/reference/io/email.html
- Gmail App Password: https://support.google.com/mail/answer/185833
- HTML Email CSS 호환성: https://www.caniemail.com/

### Related Work

- GitHub Issue: #14