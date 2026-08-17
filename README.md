# payment-system

Java 25 + Spring Boot 4 + Apache Kafka 기반 결제 시스템.

승인 경로는 동기 HTTP로 확정하고, 그 이후의 원장·정산·알림·복구는 Kafka로 분리한다.
이 프로젝트의 초점은 성공 경로가 아니라 **실패 경로** — 미확정 결제 수렴, Outbox, 멱등 소비, 대사(reconciliation)다.

## 문서

- [설계서](docs/payment-system-plan.md) — 터지는 문제 10가지와 각각의 증명 방법, 구현 로드맵 Phase 0~9

## 현재 상태

Phase 0 (스캐폴딩) 착수 전.
