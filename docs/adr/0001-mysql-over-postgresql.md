# ADR 0001 — 저장소로 PostgreSQL 대신 MySQL 을 쓴다

- 상태: 채택
- 날짜: 2026-08-20
- 관련: [이슈 #15](https://github.com/MinHyup1/payment-system/issues/15), 설계서 04·07·11장

---

## 배경

설계서 초안은 전 구간이 PostgreSQL 17 기준으로 작성됐다. 스키마를 만들기 직전에 이 선택을 다시 검토했다. 아직 Flyway 마이그레이션도 docker compose 도 없어 전환 비용이 가장 싼 시점이고, Phase 2(Outbox + Kafka) 이후에는 스키마·CDC·테스트가 전부 딸려와 훨씬 비싸진다.

판단 기준은 "일반적으로 어느 DB 가 나은가" 가 아니라 **이 시스템이 DB 에 요구하는 것** 으로 잡았다. 결제 시스템의 지배적 워크로드는 이렇다.

- `payment` 행이 상태 전이마다 반복 UPDATE 된다 (`PENDING → APPROVED → CANCELED` …)
- `outbox_event` 는 INSERT → UPDATE(`SENT`) → 배치 DELETE 라는 고빈도 갱신 사이클을 돈다
- 조회는 `payment_id` 기준 단건 조회가 지배적이다
- Phase 6 에서 CDC 로 Outbox Relay 를 교체한다

## 결정

**MySQL 8.4 (InnoDB) 를 쓴다.**

근거는 다음 다섯 가지다.

### 1. UPDATE 쓰기 증폭이 없다

PostgreSQL 은 모든 인덱스 엔트리가 튜플의 **물리적 위치(ctid)** 를 가리킨다. UPDATE 는 새 튜플 버전을 만들므로, 값이 바뀌지 않은 컬럼의 인덱스까지 **전부** 갱신된다. 인덱스가 N 개면 쓰기가 N 배로 증폭된다. HOT update 로 완화되지만 *갱신 컬럼에 인덱스가 없고 페이지에 여유 공간이 있을 때* 만 발동하는 조건부 최적화다.

InnoDB 는 세컨더리 인덱스가 **기본키** 를 가리킨다. 인덱스가 걸리지 않은 컬럼만 바꾸면 클러스터드 인덱스 하나만 건드린다. `payment` 는 상태 전이로 반복 UPDATE 되는 테이블이라 이 차이가 그대로 걸린다.

### 2. 복제 트래픽도 같은 이유로 적다

PostgreSQL 은 **물리 WAL 바이트** 를 복제한다. 위의 쓰기 증폭이 그대로 복제 트래픽 증폭이 된다. MySQL binlog 는 논리적 변경만 전송한다. 읽기 복제 분리(설계서 11장 Phase 7 확장 계획)를 실제로 하게 되면 여기서 차이가 난다.

### 3. 무중단 스키마 변경이 해결된 문제다

`gh-ost`, `pt-online-schema-change` 로 대형 테이블 ALTER 를 무중단으로 친다. 수년간 검증된 도구다. PostgreSQL 쪽은 `pg_repack` 정도이고 생태계가 얇다. 24/7 결제 시스템에서 이건 실제로 발생하는 문제이고, `jsonb` 가 편리한 것보다 훨씬 중요하다.

### 4. 커넥션 확장성

PostgreSQL 은 커넥션 하나당 OS 프로세스다 (17 기준). 앱 인스턴스가 늘면 PgBouncer 가 사실상 필수 컴포넌트가 되어 장애 지점이 하나 늘어난다. InnoDB 는 스레드 기반이라 그대로 받는다. 설계서 13장이 "ECS Fargate 2 서비스" 로 스케일아웃을 전제하므로 이 축이 유효하다.

### 5. PK 단건 조회 궁합

InnoDB 는 **클러스터드 인덱스** 라 PK 조회가 B-tree 한 번에 행 전체를 준다. PostgreSQL 은 인덱스 → 힙 접근이 한 단계 더 있다. `payment_id` 단건 조회는 결제 API 의 지배적 패턴이다. (반대로 세컨더리 인덱스 조회는 InnoDB 가 PK 를 거쳐 두 번 타므로 손해다 — 트레이드오프다.)

### 그리고 기술 외적 근거 하나

국내 결제·핀테크 실무 스택은 Aurora MySQL 계열이 압도적이다. 운영 노하우·도구·인력이 그쪽에 쌓여 있고, 이 프로젝트가 겨냥하는 환경과 맞는다. 이걸 1~5 와 같은 급의 근거로 취급하지는 않되, 동률일 때 기울이는 요소로는 인정한다.

## 대안과 기각 이유

### PostgreSQL 17 (초안의 선택)

기능 자체는 이쪽이 더 풍부하고, 실제로 **기각하면서 잃는 것이 분명히 있다.**

| 잃는 것 | 왜 아쉬운가 | 대체 수단 |
|---|---|---|
| 부분 인덱스 (`WHERE status='NEW'`) | `outbox_event` 폴링에 정확히 맞는 기능 | 일반 복합 인덱스 `(status, created_at)`. `SENT` 행을 배치 삭제해 테이블을 작게 유지하므로 실질 손해는 작다 |
| 선언적 파티셔닝 | 유니크 키 제약이 없어 자유롭다 | MySQL RANGE 파티셔닝 + 아래 "결과" 절의 제약. 이번 결정으로 파티셔닝 계획 자체를 보류했다 |
| `pg_advisory_xact_lock` | 트랜잭션 종료 시 자동 해제라 커넥션 풀과 안전하게 맞물린다 | `SKIP LOCKED` 기반 작업 테이블. MySQL `GET_LOCK()` 은 **세션 스코프라 커넥션 풀 반납 시 락이 남아 위험** 하므로 쓰지 않는다 |
| `jsonb` | 타입·인덱싱이 편하다 | Outbox payload 는 어차피 통째로 읽어 Kafka 로 보낼 뿐 검색 대상이 아니다. `JSON` 컬럼으로 충분 |
| 기본 격리 수준 READ COMMITTED | 동시성 추론이 쉽다 | MySQL 기본값은 REPEATABLE READ + 갭 락. 아래 "결과" 절 참조 |
| `INSERT ... ON CONFLICT DO NOTHING` 후 트랜잭션 유지 | 멱등키 중복 처리에 깔끔하다 | MySQL 은 유니크 위반이 문(statement)만 롤백하고 트랜잭션은 살아 있어 **오히려 다루기 쉽다** |

기각 이유는 "PostgreSQL 이 나쁘다" 가 아니라, **위 기능들의 이득보다 결정 절의 1·3 번이 이 워크로드에서 더 크게 작용한다** 는 판단이다.

### 비용은 근거로 쓰지 않는다 (오해 기록)

검토 과정에서 "PostgreSQL 은 비용을 아끼려는 곳이 쓰는 DB" 라는 통념이 나왔으나 **사실이 아니다.** 둘 다 무료 오픈소스이고, 라이선스는 오히려 PostgreSQL(BSD 계열)이 MySQL(Oracle GPL v2 듀얼)보다 관대하다. RDS·Aurora 가격도 사실상 동일하다. 이 축은 결정 근거에서 제외했다. 기록으로 남기는 이유는, 틀린 근거로 옳은 결정에 도달한 경위가 나중에 이 문서를 읽을 때 오해를 부르지 않게 하기 위해서다.

### 둘 다 지원하는 추상화

기각. 단일 배포 대상 시스템에 DB 중립 계층을 두면 양쪽의 좋은 기능(`SKIP LOCKED`, 파티셔닝)을 전부 최소공배수로 깎아내게 된다. 하나를 고르고 그 DB 를 제대로 쓴다.

## 결과와 트레이드오프

### 즉시 따라오는 제약 세 가지

**1. `payment` 월별 파티셔닝 계획을 보류한다.**

MySQL 파티셔닝은 **모든 유니크 키가 파티션 키를 포함해야 한다.** `payment` 는 PK `payment_id` 와 UQ `(merchant_id, order_id)` 를 갖는데, `created_at` 으로 파티셔닝하려면 두 키 모두에 `created_at` 이 들어가야 한다. 그러면 UQ 는 `(merchant_id, order_id, created_at)` 이 되고 — **같은 `(merchant_id, order_id)` 가 다른 달에 중복 삽입될 수 있다.** P01(중복 결제 방지)의 물리적 방어선이 무너진다.

파티셔닝은 부하 테스트로 필요성이 증명되기 전까지 하지 않는다. 필요해지면 그때 다음 중에서 고른다.

- 중복 방지 유니크 제약만 별도 비파티션 테이블로 분리
- `merchant_id` 기준 KEY 파티셔닝 (UQ 에 이미 `merchant_id` 가 있어 성립하나, 월별 정리 이점은 사라진다)
- 파티셔닝 대신 오래된 데이터를 아카이브 테이블로 이관

**정합성 제약이 성능 최적화보다 우선한다.** 이 순서는 협상 대상이 아니다.

**2. 기본 격리 수준이 REPEATABLE READ + 갭 락이다.**

PostgreSQL 의 READ COMMITTED 보다 데드락 패턴이 난해하다. 특히 조건부 UPDATE 전이와 멱등키 INSERT 가 동시에 몰리는 P01 시나리오에서 실제 거동을 확인해야 한다. Phase 1 의 동시 100요청 테스트가 이걸 겸한다.

**3. `useAffectedRows` 를 확인해야 한다.**

Connector/J 기본값(`useAffectedRows=false`)에서 `executeUpdate()` 는 *변경된* 행이 아니라 *조건에 매칭된* 행 수를 반환한다. 이 설계는 `affected rows = 0` 을 상태 전이 실패의 분기 신호로 쓰므로 핵심 메커니즘에 걸린다.

조건부 UPDATE 는 `WHERE status = 'PENDING'` 처럼 현재 상태를 조건에 포함하므로 이미 전이된 행은 애초에 매칭되지 않아 실제로는 정상 동작한다. 어긋나는 경우는 **SET 값이 기존 값과 동일한 UPDATE** 뿐이다. 그래도 Phase 1 에서 이 거동을 검증하는 테스트를 둔다.

### 나중에 영향받는 것

- **Phase 6 CDC** — Debezium MySQL 커넥터(binlog)를 쓴다. `binlog_format=ROW`, `binlog_row_image=FULL`, GTID 활성화가 전제다. PostgreSQL logical replication slot 의 "컨슈머가 죽으면 WAL 이 무한정 쌓여 디스크가 찬다" 는 실패 모드가 없어지는 것은 **이 결정의 부수적 이득** 이다.
- **느린 쿼리 추적** — `pg_stat_statements` 대신 Performance Schema (`events_statements_summary_by_digest`) 와 slow query log 를 쓴다.
- **Testcontainers** — `MySQLContainer` 로 교체.
- **클라우드 전환** — RDS / Aurora MySQL. JDBC URL 만 바뀐다는 13장의 조건은 그대로 유효하다.

### 이 결정을 되돌린다면

Flyway 마이그레이션이 쌓이고 CDC 가 붙은 뒤에는 되돌리는 비용이 크다. 되돌릴 만한 신호는 이런 것들이다 — Outbox 폴링이 부분 인덱스 부재로 병목이 되거나, 갭 락 데드락이 P01 경로에서 반복 재현되거나, `payment` 파티셔닝이 실제로 필요해졌는데 위 세 대안이 전부 만족스럽지 않은 경우. 그때는 이 문서를 갱신하지 말고 **새 ADR 을 쓴다.**
