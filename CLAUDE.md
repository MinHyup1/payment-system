# CLAUDE.md

Behavioral guidelines to reduce common LLM coding mistakes. Merge with project-specific instructions as needed.

**Tradeoff:** These guidelines bias toward caution over speed. For trivial tasks, use judgment.

## 1. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:
- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them - don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

## 2. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

## 3. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it - don't delete it.

When your changes create orphans:
- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

## 4. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:
- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:
```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.

---

**These guidelines are working if:** fewer unnecessary changes in diffs, fewer rewrites due to overcomplication, and clarifying questions come before implementation rather than after mistakes.

---

# 프로젝트: payment-system

Java 25 + Spring Boot 4 + Kafka 기반 결제 시스템.

## 먼저 읽을 문서

| 문서 | 내용 |
|---|---|
| [docs/CONVENTIONS.md](docs/CONVENTIONS.md) | **작업 규칙 전체** — Git, 이슈/PR, 코드, 테스트 |
| [docs/payment-system-plan.md](docs/payment-system-plan.md) | 설계서 — 문제 10가지(P01~P10), 로드맵 Phase 0~9 |

작업 전에 컨벤션 문서를 확인한다. 아래는 그중 **자주 어기게 되는 것만** 추린 것이다.

## 절대 규칙

- **이슈 없이 브랜치를 만들지 않는다.** GitHub 이슈 생성 → `issue/{id}-{설명}` 브랜치 → PR base는 `dev`.
- **`main` / `dev` 직접 push 금지.** 머지는 항상 `--no-ff`.
- **모듈 경계를 넘지 않는다.** `ledger`/`settlement` 는 `payment-app` 을 의존하지 않고, `payment-domain` 은 프레임워크 의존이 0이다. ArchUnit 테스트가 이를 검증한다.
- **금액은 `long` 최소 화폐 단위 + `Money` VO.** `double`도 `BigDecimal`도 쓰지 않는다.
- **시간은 UTC `Instant`, `Clock` 주입.** `Instant.now()` 직접 호출과 `LocalDateTime` 저장 금지.
- **카드번호 전체·CVC·유효기간을 로그에 남기지 않는다.** 카드번호는 앞 6 + 뒤 4만.
- **`sealed` 타입 switch에 `default` 절을 쓰지 않는다.** 상태 추가 시 컴파일러가 누락을 잡아야 한다.
- **Lombok에서 `@Data` · `@Setter` · `@AllArgsConstructor` 는 금지.** `lombok.config` 가 컴파일 에러로 막는다. 이벤트·DTO·값 객체는 record, Lombok은 JPA 엔티티와 Spring 빈에서만.
- **베이스 패키지는 `com.payment`.**
- **`Thread.sleep()` 대신 Awaitility.** 테스트 메서드명은 한글 스네이크 케이스.

## 설계 판단 (바꾸려면 먼저 상의)

- 승인 경로(동기 HTTP)는 **Kafka에 직접 produce하지 않는다.** Outbox 테이블에 같은 트랜잭션으로 쓴다.
- 상태 전이는 서비스 코드의 `if` 가 아니라 **DB 조건부 UPDATE**로 강제한다. `affected rows = 0` 은 예외가 아니라 분기다.
- PG 타임아웃은 실패가 아니라 **`UNKNOWN` 상태**다. 성공·실패를 단정하지 않는다.
- Kafka는 **at-least-once + 소비 측 멱등**. EOS를 쓰지 않는 이유는 ADR에 남긴다.
- 대사(reconciliation) 불일치는 **자동 교정하지 않는다.**

## 완료의 정의

`needs-proof` 라벨이 붙은 작업은 **그 문제를 증명하는 테스트가 있어야** 완료다.
설계서 05장 기준 — 증명 칸이 비어 있으면 구현하지 않은 것으로 친다.
