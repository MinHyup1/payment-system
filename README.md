# payment-system

Java 25 + Spring Boot 4 + Apache Kafka 기반 결제 시스템.

승인 경로는 동기 HTTP로 확정하고, 그 이후의 원장·정산·알림·복구는 Kafka로 분리한다.
이 프로젝트의 초점은 성공 경로가 아니라 **실패 경로** — 미확정 결제 수렴, Outbox, 멱등 소비, 대사(reconciliation)다.

## 문서

- [설계서](docs/payment-system-plan.md) — 터지는 문제 10가지와 각각의 증명 방법, 구현 로드맵 Phase 0~9
- [컨벤션](docs/CONVENTIONS.md) — Git · 이슈/PR · 코드 · 테스트 작업 규칙

## 환경 변수

`apps/api` 는 아래 값이 없으면 로컬 개발용 기본값으로 뜬다. **운영에서는 반드시 덮어쓴다.**

| 변수 | 용도 |
|---|---|
| `PAYMENT_ADMIN_JWT_SECRET` | 어드민 API JWT(HS256) 서명 키. 최소 32바이트 |
| `PAYMENT_MERCHANT_M1001_API_KEY` | 가맹점 `m_1001` 의 API Key. 가맹점이 늘면 `payment.security.merchant.api-keys` 에 추가한다 |

인증은 소비자별로 나뉜다 — 가맹점 API 는 `X-API-Key` 헤더, 어드민 API(`/admin/**`)는 `Authorization: Bearer` JWT.

## 현재 상태

Phase 0 진행 중. Gradle 멀티모듈 스캐폴딩 완료.
