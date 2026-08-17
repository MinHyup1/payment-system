# 프로젝트 컨벤션

> 이 문서는 `payment-system` 저장소의 작업 규칙을 정한다.
> 설계 내용은 [설계서](payment-system-plan.md)에, **어떻게 작업할지**는 이 문서에 있다.
>
> 원칙 — **규칙은 문서가 아니라 도구가 지킨다.** 여기 적힌 것 중 자동 검증이 가능한 것은 전부 CI나 테스트로 옮기고, 이 문서는 "왜 그렇게 정했는가"를 남기는 용도로 쓴다.

---

## 목차

| # | 장 | 내용 |
|---|---|---|
| 1 | [Git](#1--git) | 브랜치, 커밋, 머지, 태그 |
| 2 | [이슈와 PR](#2--이슈와-pr) | 작업 단위, 라벨, 템플릿, 머지 전 체크 |
| 3 | [코드 (Java)](#3--코드-java) | 모듈 경계, 패키지, 네이밍, Java 25 · Lombok 사용 지침 |
| 4 | [테스트](#4--테스트) | 계층별 도구, 이름, 증명 태그 |
| 5 | [문서](#5--문서) | ADR, 성능 리포트, README |

---

## 1 — Git

### 1.1 브랜치 구조

브랜치는 **세 종류만** 쓴다.

```
main  ←──(머지)──  dev  ←──(머지)──  issue/{id}-{설명}
```

| 브랜치 | 역할 | 규칙 |
|---|---|---|
| `main` | 배포 가능 상태 | **Phase 완료 시점에만** dev에서 머지. 항상 초록(테스트 통과) 상태 유지 |
| `dev` | 통합 브랜치 | 모든 이슈 브랜치가 여기로 모인다. 기본 작업 대상 |
| `issue/{id}-{설명}` | 이슈 단위 작업 | dev에서 따고 dev로 머지. 머지 후 삭제 |

**hotfix 브랜치는 두지 않는다.** 아직 운영 배포가 없으므로 긴급 수정 경로가 필요 없다. 배포가 생기는 시점에 이 규칙을 다시 정한다.

### 1.2 브랜치 이름

```
issue/{이슈번호}-{영문-케밥-설명}
```

| 예시 | 설명 |
|---|---|
| `issue/12-idempotency-key` | 멱등키 처리 구현 |
| `issue/27-outbox-relay` | Outbox Relay 구현 |
| `issue/34-fix-consumer-lag` | 컨슈머 lag 문제 수정 |

- 이슈 번호는 **GitHub 이슈를 먼저 만들고** 부여된 번호를 쓴다. 이슈 없는 브랜치는 만들지 않는다.
- 설명은 **영문 소문자 + 하이픈**. 3~4단어 이내. 한글·대문자·언더스코어 금지.
- 브랜치 이름은 목록에서 스캔하기 위한 것이다. 정확한 설명은 이슈 제목이 담당하므로 여기서는 짧게 쓴다.

### 1.3 작업 흐름

```bash
# 1. 이슈 생성 (번호를 받는다)
gh issue create --title "멱등키로 중복 승인 차단" --label "enhancement,needs-proof"

# 2. dev 최신화 후 브랜치 생성
git switch dev && git pull
git switch -c issue/12-idempotency-key

# 3. 작업 · 커밋

# 4. push 후 PR 생성 (base = dev)
git push -u origin issue/12-idempotency-key
gh pr create --base dev --fill

# 5. 머지 (--no-ff) 후 브랜치 삭제
gh pr merge --merge --delete-branch
```

### 1.4 커밋 메시지

**Conventional Commits + 한글 본문.**

```
<type>(<scope>): <제목>

<본문 — 무엇을 했는지가 아니라 왜 했는지>

Refs #12
```

**타입**

| type | 쓰는 경우 |
|---|---|
| `feat` | 기능 추가 |
| `fix` | 버그 수정 |
| `docs` | 문서만 변경 |
| `test` | 테스트 추가·수정 (프로덕션 코드 변경 없음) |
| `refactor` | 동작 변화 없는 구조 개선 |
| `perf` | 성능 개선 (측정치를 본문에 남긴다) |
| `build` | Gradle, 의존성, Dockerfile |
| `ops` | docker compose, Grafana 대시보드, k6 시나리오 |
| `chore` | 그 외 잡무 |

**스코프** — 모듈 또는 영역 이름을 쓴다: `payment`, `ledger`, `settlement`, `pg-adapter`, `shared-kernel`, `outbox`, `kafka`, `api`, `clearing`, `ops`

**규칙**

- 제목은 **한글**, 50자 이내, 마침표 없음, 명사형 또는 평서형("~한다"). 명령형 영어 문법은 따르지 않는다.
- 본문은 **왜**를 쓴다. 무엇을 바꿨는지는 diff가 이미 말한다.
- 이슈 참조는 커밋에 `Refs #12`, 이슈를 닫는 것은 **PR 본문의 `Closes #12`** 로만 한다. 커밋마다 close 키워드를 쓰면 중간 커밋이 이슈를 닫는다.
- 한 커밋은 한 가지 일만. 리팩터링과 기능 추가를 섞지 않는다.

**예시**

```
feat(payment): 멱등키 선점으로 중복 승인 차단

PG 호출 전에 idempotency_record에 INSERT하고 PK 충돌을
락 대신 사용한다. 애플리케이션 레벨 락은 인스턴스가 늘면
깨지므로 DB 제약 하나로 직렬화를 보장한다.

Refs #12
```

### 1.5 머지 전략

| 방향 | 전략 | 이유 |
|---|---|---|
| `issue/*` → `dev` | **Merge commit (`--no-ff`)** | 작업 단위가 그래프에 남는다. 이슈 하나가 어떤 커밋들로 구성됐는지 나중에 추적 가능 |
| `dev` → `main` | **Merge commit (`--no-ff`)** | Phase 완료 지점이 main 히스토리에 명시적으로 남는다 |

- 이슈 브랜치가 dev보다 뒤처졌으면 **머지 전에 `git rebase dev`** 로 최신화한다. dev를 이슈 브랜치로 머지해오지 않는다 — 머지 커밋이 양방향으로 얽히면 그래프를 못 읽는다.
- 이미 push한 이슈 브랜치를 rebase하면 force push가 필요하다. **본인 이슈 브랜치에 한해 `--force-with-lease` 를 허용**한다.
- 충돌은 이슈 브랜치에서 해결한다. dev에서 해결하지 않는다.

### 1.6 태그

Phase 완료 시 `main`에 태그를 붙인다.

```
phase-0   Phase 0 완료 (스캐폴딩 + Mock PG)
phase-1   Phase 1 완료 (동기 승인 + 멱등성)
...
```

Phase 완료 기준은 설계서 11장에 정의돼 있다 — **동작하는 것 + 증명하는 테스트가 모두 있어야** 태그를 붙인다.

정식 릴리스 버저닝(`v1.0.0`)은 배포가 생기기 전까지 쓰지 않는다.

### 1.7 하지 않는 것

- ❌ `main` / `dev` 직접 push — 반드시 PR을 거친다
- ❌ `main` / `dev` force push
- ❌ 이슈 없는 브랜치
- ❌ 여러 이슈를 한 브랜치에서 작업
- ❌ 빌드가 깨진 상태로 dev 머지
- ❌ 생성된 산출물 커밋 — `build/`, `.gradle/`, k6 결과, 로그 (`.gitignore` 참조)
- ❌ 시크릿·실 PG 키 커밋. 로컬 값은 `.env` (gitignore 대상)

---

## 2 — 이슈와 PR

### 2.1 이슈 = 작업 단위

**이슈 하나 = 브랜치 하나 = PR 하나.** 이 대응이 깨지면 추적이 무너진다.

이슈를 쪼개는 기준은 "**하루 이틀 안에 끝나고, 끝났는지 판정할 수 있는가**"다. 설계서의 Phase는 이슈가 아니라 이슈들의 묶음이다.

| 나쁜 이슈 | 좋은 이슈 |
|---|---|
| "Phase 1 구현" | "멱등키로 중복 승인 차단 (P01)" |
| "성능 개선" | "가상 스레드 × Hikari 풀 크기 매트릭스 측정" |
| "테스트 추가" | "동시 100요청 멱등성 통합 테스트 작성" |

### 2.2 라벨

GitHub 기본 라벨(`bug` `documentation` `enhancement` `question` …)을 그대로 쓴다. **필요해질 때 1개씩 추가한다.** 미리 체계를 만들어두지 않는다.

직접 만든 라벨은 하나뿐이다.

| 라벨 | 용도 |
|---|---|
| `needs-proof` | 설계서 05장의 문제(`P01`~`P10`) 중 하나를 다루는 이슈. **증명 테스트 없이 닫을 수 없다** |

`needs-proof` 가 이 프로젝트의 핵심 장치다. 설계서가 "증명 칸이 비어 있으면 구현하지 않은 것으로 친다"고 선언했으므로, 라벨이 그 선언을 이슈 트래커에서 강제한다.

**어느 문제인지는 라벨이 아니라 이슈 본문 참고란에 적는다** (예: `설계서 05장 P02`). 번호별 필터가 필요하면 테스트 태그(`-DincludeTags=P02`, 4.3 참조)로 검증하는 쪽이 정확하다. 단계(Phase) 추적도 라벨로 하지 않는다 — 설계서 11장 로드맵이 그 역할을 한다.

### 2.3 이슈 본문

템플릿은 [`.github/ISSUE_TEMPLATE/`](../.github/ISSUE_TEMPLATE) 에 있다. 핵심은 **완료 기준**이다.

```markdown
## 배경
왜 필요한가. 설계서 어느 절과 연결되는가.

## 할 일
- [ ] 구체적인 작업 항목

## 완료 기준
어떤 상태가 되면 이 이슈를 닫는가. 검증 가능한 문장으로.
예) 같은 멱등키 동시 100요청 → PG Mock 호출 1회, 승인 레코드 1건

## 참고
설계서 05장 P01
```

**완료 기준이 "잘 동작한다" 수준이면 그 이슈는 아직 정의가 안 된 것이다.** 쪼개거나 다시 쓴다.

### 2.4 PR

- **제목** — 커밋 메시지 제목과 같은 형식: `feat(payment): 멱등키 선점으로 중복 승인 차단`
- **base 브랜치** — 항상 `dev`
- **본문** — 템플릿([`.github/pull_request_template.md`](../.github/pull_request_template.md)) 사용. `Closes #12` 를 반드시 포함한다
- **크기** — 리뷰 가능한 크기로. 변경 파일이 20개를 넘어가면 이슈를 잘못 쪼갠 것이다
- **Draft** — 작업 중에도 PR을 먼저 열어두면 CI가 계속 돌아 문제를 일찍 잡는다

### 2.5 머지 전 체크리스트

PR 템플릿에 들어가는 항목. **하나라도 안 되면 머지하지 않는다.**

- [ ] 이슈의 완료 기준을 전부 만족한다
- [ ] `./gradlew build` 통과 (테스트 포함)
- [ ] ArchUnit 모듈 경계 테스트 통과
- [ ] `needs-proof` 라벨이 붙은 이슈라면, 그 문제를 증명하는 테스트가 이 PR에 있다
- [ ] 새 설정값·환경변수를 추가했다면 README 또는 compose 파일에 반영
- [ ] 설계 판단이 바뀌었다면 설계서 또는 ADR을 함께 수정

---

## 3 — 코드 (Java)

### 3.1 모듈 경계가 최우선 규칙

설계서 03장의 모듈 구조는 **나중에 서비스를 분리하기 위한 것**이므로, 경계를 어기면 프로젝트의 주장 자체가 무너진다.

- `ledger` / `settlement` 는 `payment-app` 을 **의존하지 않는다.** 통신은 `shared-kernel` 의 이벤트 레코드로만.
- `payment-domain` 은 **프레임워크 의존 0.** Spring 애노테이션이 들어가는 순간 실패다.
- 모듈 간 DB 스키마 분리(`payment`, `ledger`, `settlement`). **크로스 스키마 조인 금지.**
- 이 규칙들은 Gradle 의존성 그래프로 1차, **ArchUnit 테스트로 2차** 차단한다. 문서가 아니라 테스트가 지킨다.

### 3.2 패키지 구조

베이스 패키지는 `com.payment` 로 한다.

```
com.payment.<module>.<layer>
```

| 모듈 | 패키지 |
|---|---|
| payment-domain | `com.payment.domain` |
| payment-app | `com.payment.app` |
| ledger | `com.payment.ledger` |
| settlement | `com.payment.settlement` |
| pg-adapter | `com.payment.pg` |
| shared-kernel | `com.payment.shared` |

레이어는 각 모듈 안에서 `api` / `application` / `domain` / `infra` 로 나눈다. 기술별(`controller`, `service`, `repository`)이 아니라 **의존 방향별**로 나눈다 — 안쪽(domain)이 바깥쪽(infra)을 모르는 구조를 패키지가 드러내야 한다.

### 3.3 네이밍

| 대상 | 규칙 | 예 |
|---|---|---|
| 유스케이스 | `{동사}{대상}UseCase` | `ApprovePaymentUseCase` |
| 도메인 이벤트 | 과거형 명사 | `PaymentApproved`, `PaymentCanceled` |
| 외부 어댑터 | `{대상}Adapter` | `TossPgAdapter`, `MockPgAdapter` |
| Kafka 컨슈머 | `{대상}Consumer` | `LedgerEventConsumer` |
| REST 컨트롤러 | `{리소스}Controller` | `PaymentController` |
| 요청·응답 DTO | `{유스케이스}Request` / `Response` | `ApprovePaymentRequest` |
| 테스트 | 4장 참조 | |

- 축약어 금지. `pmt`, `txn`, `amt` 대신 `payment`, `transaction`, `amount`.
- 예외는 도메인 용어를 쓴다: `PaymentAlreadyCanceledException` (O), `InvalidStateException` (X).

### 3.4 Java 25 기능 사용 지침

설계서 03장에서 정한 용도로만 쓴다. **"신기능을 썼다"는 어필이 아니라 그 기능이 아니면 안 되는 이유가 있어야 한다.**

| 기능 | 쓸 곳 | 쓰지 말 곳 |
|---|---|---|
| `record` | DTO, 이벤트, 값 객체 | 가변 상태가 필요한 엔티티 |
| `sealed interface` + 패턴 매칭 | 결제 상태, 도메인 이벤트, PG 응답 | 케이스가 열려 있는 확장 지점 |
| Virtual Threads | PG 호출 등 I/O 대기 구간 | CPU 바운드 작업 |
| Scoped Values | 요청 컨텍스트(멱등키, traceId, merchantId) 전파 | 가변 상태 공유 |
| Structured Concurrency *(preview)* | 리스크 체크 + 한도 조회 병렬 fan-out | 그 외 전부 — **선택 과제이며 실패하면 걷어낸다** |

- 의존성 주입은 **생성자 주입만.** 필드 `@Autowired` 금지.
- `sealed` 상태를 switch로 다룰 때 **`default` 절을 쓰지 않는다.** default를 쓰면 상태 추가 시 컴파일러가 잡아주지 못한다 — sealed를 쓰는 이유 자체가 사라진다.

### 3.5 Lombok

**쓴다.** 다만 "쓴다/안 쓴다"보다 **어떤 애노테이션을 쓰느냐**가 사고를 가르므로, 목록을 못박는다.

**record와의 역할 분담** — 이 경계를 먼저 정하지 않으면 같은 목적에 두 가지 방식이 섞인다.

| 대상 | 방식 |
|---|---|
| 도메인 이벤트, DTO, 값 객체(`Money` 등) | **record** — 불변이 기본값이고 Lombok이 필요 없다 |
| JPA 엔티티, Spring 빈 | **Lombok** — 가변 상태와 프레임워크 요구사항(기본 생성자)이 있는 곳 |

**허용**

| 애노테이션 | 용도 |
|---|---|
| `@Getter` | 엔티티 접근자 |
| `@RequiredArgsConstructor` | 생성자 주입 (`final` 필드 기준) |
| `@Builder` | 필드가 많은 객체 생성. 특히 테스트 픽스처 |
| `@Slf4j` | 로거 선언 |
| `@NoArgsConstructor(access = AccessLevel.PROTECTED)` | JPA가 요구하는 기본 생성자. **`PROTECTED` 로 좁힌다** |

**금지**

| 애노테이션 | 이유 |
|---|---|
| `@Data` | `@Setter` + `@EqualsAndHashCode` + `@ToString` 을 한꺼번에 연다. 도메인 불변성이 무너지고, JPA 엔티티에서는 연관관계까지 `toString`·`equals` 에 끌려 들어가 지연로딩과 얽힌 사고가 난다 |
| `@Setter` | 상태 변경은 **의미 있는 이름의 메서드**로 한다. `payment.approve(tid)` 이지 `payment.setStatus(APPROVED)` 가 아니다. 설계서 04장의 상태 전이 규칙이 setter 하나로 우회된다 |
| `@AllArgsConstructor` | 필드 순서를 바꾸면 호출부가 **조용히** 깨진다. 타입이 같은 필드가 인접하면 컴파일도 통과한다. 결제 금액과 취소 금액이 둘 다 `long` 인 이 도메인에서는 특히 위험하다 |

**주의해서 쓸 것**

- `@EqualsAndHashCode` — JPA 엔티티에는 쓰지 않고 **식별자 기준으로 직접 구현**한다. 영속성 컨텍스트 안팎에서 동등성 기준이 달라진다.
- `@ToString` — **카드번호·유효기간·CVC가 있는 클래스에는 붙이지 않는다.** 붙여야 하면 해당 필드에 `@ToString.Exclude` 를 반드시 건다 (3.8 로깅 규칙과 연결).

**강제 방법**

문서가 아니라 컴파일러가 막는다. 루트 [`lombok.config`](../lombok.config) 에서 금지 항목을 `ERROR` 로 설정했다.

```properties
lombok.data.flagUsage = ERROR
lombok.setter.flagUsage = ERROR
lombok.allArgsConstructor.flagUsage = ERROR
```

**버전** — Lombok은 javac 내부 API에 의존해 JDK 메이저 버전 전환 때 깨진 전력이 있다. **JDK 25를 지원하는 버전**을 쓰고, Phase 0에서 실제 컴파일로 확인한 뒤 버전을 고정한다. 여기서 막히면 record + 명시적 생성자로 후퇴하는 것이 대안이다.

### 3.6 금액과 시간

결제 도메인에서 가장 많이 터지는 두 가지라 별도로 못박는다.

**금액**

- `long` **최소 화폐 단위(원)** + 통화 코드. `double`·`float`은 물론 `BigDecimal`도 쓰지 않는다.
- 원시 `long`을 그대로 돌리지 않고 `Money` 값 객체로 감싼다. 통화가 다른 금액의 연산은 예외.
- DB 컬럼은 `BIGINT`.

**시간**

- 저장·전송은 전부 **UTC `Instant`**. `LocalDateTime`은 저장에 쓰지 않는다 — 타임존이 없어 서버 설정에 의존한다.
- KST 변환은 표시 계층에서만.
- 시간은 **주입받는다** (`Clock` 빈). `Instant.now()` 직접 호출은 테스트를 불가능하게 만든다.

### 3.7 예외

- 도메인 예외는 `shared-kernel` 의 sealed 계층으로 두고, 각 예외가 **에러 코드**를 갖는다.
- REST 응답은 `@RestControllerAdvice` 한 곳에서 변환한다. 컨트롤러에서 try-catch 하지 않는다.
- **결제에서 "모른다"는 예외가 아니라 상태다.** PG 타임아웃을 예외로 던져 실패 처리하지 말고 `UNKNOWN` 으로 전이시킨다 (설계서 P02).
- 조건부 UPDATE의 `affected rows = 0` 은 예외가 아니라 **분기**다. 누군가 먼저 전이시켰다는 정상 정보다.

### 3.8 로깅

- **구조화 로깅(JSON)**, `traceId` 를 모든 로그에 포함한다.
- 로그 레벨: `ERROR`는 사람이 개입해야 하는 것만. 재시도로 해결되는 것은 `WARN`.
- **절대 로그에 남기지 않는다** — 카드번호 전체, CVC, 유효기간, 비밀번호. 카드번호는 앞 6 + 뒤 4만 남기고 마스킹한다. 이 규칙 위반은 리뷰에서 무조건 반려한다.
- 로그로 상태를 추적하지 않는다. 돈의 상태 변화는 `payment_history` 테이블에 남기고, 로그는 디버깅 보조다.

### 3.9 포맷팅

- **Spotless + google-java-format (AOSP)** — 들여쓰기 4칸, 줄 길이 120.
- `./gradlew spotlessApply` 로 정리하고, CI에서 `spotlessCheck` 로 강제한다.
- **포맷은 논쟁하지 않는다.** 도구가 정한 대로 따르고 리뷰에서 다루지 않는다.

---

## 4 — 테스트

설계서 10장의 "테스트 목록이 곧 이 프로젝트의 주장 목록이다"를 실행 규칙으로 옮긴 것.

### 4.1 계층과 도구

| 계층 | 도구 | 클래스 이름 |
|---|---|---|
| 도메인 단위 | JUnit 5 | `PaymentStateMachineTest` |
| 불변식 속성 | jqwik | `LedgerInvariantPropertyTest` |
| 통합 | Testcontainers (Postgres + Kafka) | `OutboxRelayIntegrationTest` |
| 동시성 | CountDownLatch + Awaitility | `IdempotencyConcurrencyTest` |
| 카오스 | Toxiproxy | `PgTimeoutChaosTest` |
| 계약 | 직렬화 왕복 | `PaymentEventContractTest` |
| 부하 | k6 (`ops/load/`) | 자바 테스트 아님 |

**임베디드 Kafka를 쓰지 않는다.** 리밸런싱과 offset 동작을 재현하지 못하면 P04를 증명할 수 없다. Testcontainers로 실제 브로커를 띄운다.

### 4.2 테스트 이름

메서드명은 **한글 스네이크 케이스**로 쓴다. 결제 도메인은 조건이 복잡해서 영문으로는 문장이 안 읽힌다.

```java
@Test
void 같은_멱등키로_동시_100요청시_승인은_1건만_생성된다() { }

@Test
void PG_응답이_유실되면_UNKNOWN으로_전이하고_조회로_수렴한다() { }
```

- `@DisplayName` 은 **쓰지 않는다.** 메서드명이 이미 한글이라 중복이다.
- 구조는 `// given` `// when` `// then` 주석으로 구분한다.
- 이름은 **결과**를 말한다. `테스트_승인()` 이 아니라 `승인시_원장에_차변대변_2건이_기록된다()`.

### 4.3 증명 태그

설계서 05장의 문제를 증명하는 테스트에는 태그를 붙인다.

```java
@Tag("P01")
class IdempotencyConcurrencyTest { }
```

```bash
./gradlew test --tests '*' -DincludeTags=P01   # P01 증명 테스트만 실행
```

이 태그가 있으면 "P01을 해결했다"는 주장을 **명령어 한 줄로 검증**할 수 있다. 카오스 시나리오 6종도 마찬가지로 스크립트 한 줄로 재현 가능해야 한다(설계서 10장).

### 4.4 규칙

- 테스트는 **서로 독립**이어야 한다. 실행 순서에 의존하는 테스트는 반려.
- `Thread.sleep()` 금지. 비동기 수렴 검증은 **Awaitility**로 조건과 타임아웃을 명시한다.
- 테스트 데이터는 픽스처 빌더로 만든다. 각 테스트는 **자기가 신경 쓰는 필드만** 지정한다.
- 커버리지 숫자를 목표로 삼지 않는다. 목표는 **설계서의 증명 칸을 전부 채우는 것**이다.

---

## 5 — 문서

| 문서 | 위치 | 언제 쓰는가 |
|---|---|---|
| 설계서 | `docs/payment-system-plan.md` | 설계 판단이 바뀔 때 갱신 |
| 컨벤션 | `docs/CONVENTIONS.md` | 이 문서 |
| ADR | `docs/adr/NNNN-{제목}.md` | 되돌리기 어려운 기술 결정을 내릴 때 |
| 성능 리포트 | `PERFORMANCE.md` | Phase 7. 측정 환경 / 변경 / before / after / 해석 |
| 실행 가이드 | `README.md` | Phase 8. 5분 안에 돌려볼 수 있게 |

**ADR 형식** — 배경 / 결정 / 대안과 기각 이유 / 결과와 트레이드오프.

설계서 11장 Phase 8이 예고한 ADR 세 개는 처음부터 예약돼 있다:

- 왜 승인 경로에 Kafka를 쓰지 않았는가
- 왜 Kafka EOS를 쓰지 않는가
- 왜 대사 불일치를 자동 교정하지 않는가

**실패한 결정도 남긴다.** 효과 없던 튜닝, 걷어낸 기능(예: Structured Concurrency)의 기록이 이 프로젝트에서는 성공 기록만큼 값이 있다.
