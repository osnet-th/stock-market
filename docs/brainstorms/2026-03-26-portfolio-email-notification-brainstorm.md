# 포트폴리오 마감 메일 알림 기능

**Date:** 2026-03-26
**Status:** Brainstorm
**GitHub Issue:** #14

## What We're Building

본장 마감(15:30) 후 알림을 신청한 사용자에게 포트폴리오 평가 결과를 이메일로 발송하는 기능.

메일에는 각 주식별 평가금액과 전체 포트폴리오 총 평가금액이 HTML 테이블 형식으로 포함된다.

### 핵심 요구사항

1. **알림 신청**: 포트폴리오 페이지에 '마감 알림 받기' 토글 버튼 — User 모델에 `notificationEnabled` 필드 추가
2. **발송 시점**: 본장 마감 후 15:30 (Spring `@Scheduled` cron)
3. **발송 대상**: `notificationEnabled = true`인 사용자만
4. **메일 내용**: 주식별 현재가/평가금액 + 전체 평가금액 (HTML 테이블)
5. **SMTP**: Gmail SMTP (앱 비밀번호, 일 500통 제한)
6. **수신 주소**: OAuth 계정(카카오)에 등록된 이메일 사용

## Why This Approach

### 기존 인프라 활용

- `spring-boot-starter-mail` 의존성 이미 존재
- `NotificationService` 인터페이스 + `EmailNotificationService` 스켈레톤 이미 존재
- `@Scheduled` 스케줄러 패턴 2개 기존 예시 있음 (EcosIndicatorBatchScheduler, KeywordNewsBatchScheduler)
- `StockPriceService`로 현재가 벌크 조회 가능
- `OAuthAccount`에 이메일 필드 존재

### Gmail SMTP 선택 이유

- 무료, 별도 서버 불필요
- 일 500통 제한이지만 개인 프로젝트 규모에 충분
- Spring Boot에서 설정 간단 (`spring.mail.*`)

## Key Decisions

1. **알림 신청**: 포트폴리오 페이지 토글 버튼 → User 모델에 `notificationEnabled` 필드 추가
2. **발송 시점**: 본장 마감 후 15:30 1회만 (프리마켓은 추후 검토)
3. **SMTP**: Gmail SMTP + 앱 비밀번호 (환경변수로 관리)
4. **메일 형식**: HTML 테이블 (주식명, 수량, 현재가, 평가금액, 손익률)
5. **수신 주소**: OAuthAccount.email 사용
6. **평가금액 계산**: 서버에서 StockPriceService로 현재가 조회 후 계산

## Resolved Questions

- 발송 시점: 본장 마감 후만 (15:30)
- SMTP: Gmail SMTP
- 메일 형식: HTML 테이블
- 알림 신청 방식: 포트폴리오 페이지 토글

## 영향 범위 (예상)

### 백엔드
- User 도메인: `notificationEnabled` 필드 추가
- EmailNotificationService: 실제 발송 로직 구현
- PortfolioNotificationScheduler: @Scheduled 스케줄러 신규 생성
- PortfolioEvaluationService: 서버 측 포트폴리오 평가금액 계산
- SMTP 설정: application.yml에 spring.mail.* 추가

### 프론트엔드
- 포트폴리오 페이지에 '마감 알림 받기' 토글 버튼
- User API에 알림 설정 토글 엔드포인트