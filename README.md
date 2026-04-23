# liveklass 과제 BE-Type-A: 수강 신청 시스템

## 프로젝트 개요

크리에이터(강사)가 강의를 개설하고, 수강생이 원하는 강의에 신청·결제·취소할 수 있는 수강 신청 시스템입니다.

**구현 사항**

- 강의 상태 관리 (`DRAFT` → `OPEN` → `CLOSED`)
- 수강 신청 상태 흐름 (`PENDING` → `CONFIRMED` → `CANCELLED`)
- 동시 신청 시 정원 초과 방지 (Pessimistic Lock)
- 결제 확정 후 7일 이내 취소 가능

---

## 기술 스택

| 항목 | 사양 |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5.13 |
| ORM | Spring Data JPA (Hibernate) |
| Build | Gradle |
| DB (운영) | MySQL 8 |
| DB (테스트) | H2 In-memory |

---

## 실행 방법

### 실행 요구사항

- Docker, Docker Compose
- Java 21

### 실행

```bash
docker-compose up
```

### 테스트 실행

```bash
./gradlew test
```
> 테스트는 H2 In-memory DB를 사용함

---

## 요구사항 해석 및 가정

강의 정원에 대한 요구사항이 모호하여 실제 운영중인 서비스를 참고하였으며 정원 무제한 강의 존재하는 것을 확인했습니다.
이를 반영하여 max_capacity를 Nullable로 설계 하였으며 Null인 경우 정원 검증을 생략합니다.


**인증/인가**

User 인증·인가는 외부 서비스에서 처리된다고 가정합니다.
과제 요구사항에 명시된 userId를 `X-User-Id` 헤더로 전달받는 형태로 진행


**결제 시스템**

외부 결제 시스템 연동은 구현하지 않으며, 결제 확정 API 호출 시 `PENDING` → `CONFIRMED` 상태 변경으로 대체합니다.


**취소 정책**

수강 취소는 결제 확정(`CONFIRMED`) 후 7일 이내에만 가능합니다. 이후 취소 시도는 예외를 반환합니다.


**중복 신청**

동일 사용자가 동일 강의에 `PENDING` 또는 `CONFIRMED` 상태의 신청이 존재하는 경우 중복 신청으로 간주하고 거부합니다. 취소(`CANCELLED`) 후 재신청은 허용합니다.


**정원 계산**

현재 신청 인원은 `PENDING` + `CONFIRMED` 상태의 Enrollment 수를 기준으로 계산합니다.

---

## 설계 결정과 이유

`presentation` → `application` → `domain` → `infrastructure` 4계층으로 구성합니다. 
비즈니스 규칙(상태 전이, 정원 검증, 취소 가능 여부)은 Service가 아닌 도메인 메서드 안에 위치합니다.

### Pessimistic Lock 선택 이유

동시에 여러 사용자가 마지막 자리에 신청하는 상황을 처리하기 위해 비관적 락(`SELECT FOR UPDATE`)을 선택했습니다.

수강 신청 도메인은 인기 강의 오픈 시점에 트래픽이 집중되는 특성상 충돌 빈도가 높습니다. 낙관적 락은 충돌이 드문 환경을 전제하므로, 충돌이 잦을 경우 `OptimisticLockException`를 통한 재시도가 오히려 DB 부하를 키울 가능성이 높습니다.
비관적 락은 충돌을 원천 차단하기 때문에 정원 초과를 확실하게 방지합니다.

>현재는 단일 인스턴스 환경이므로 비관적 락으로 처리하였으며 분산 환경으로 확장 시에는 Redis 분산 락 또는 메시지 큐 방식으로의 전환을 고려할 수 있습니다.

Flow: `Course` 행 잠금 → 현재 신청 인원 COUNT → 정원 검증 → `Enrollment` 저장

### 상태값 VARCHAR(20) 사용

MySQL `ENUM` 타입은 값 추가 시 DDL 변경이 필요합니다. 
`VARCHAR(20)`으로 관리하며 Application Layer에서 상태 Enum을 통해 관리합니다.

---

## API 목록 및 예시

### API 문서
http://localhost:8080/swagger-ui.html

---




## 데이터 모델 설명
### 전체 ERD
<img width="623" height="551" alt="image" src="https://github.com/user-attachments/assets/7db27058-1537-4fb2-a05e-6b08c64e1c2b" />

### 인덱스

``` SQL
-- 정원 COUNT 쿼리 성능
INDEX idx_enrollment_course_id (course_id)

-- 내 수강 신청 목록 조회
INDEX idx_enrollment_user_id (user_id)

-- 강의 목록 상태 필터
INDEX idx_course_status (status)
```


<img width="326" height="535" alt="image" src="https://github.com/user-attachments/assets/c592b9ce-84c7-4a48-85db-e049d7d707b2" />

### Course
- id `BIGINT`: 강의 고유 식별자
- title `VARCHAR(255)`: 강의 이름
- description `TEXT`: 강의 설명 (긴 설명을 고려해 TEXT 선택)
- price `DECIMAL`: 수강료
- max_capacity `INTEGER`: 최대 수강 인원 (null = 무제한) 실제 서비스에서 정원 무제한 강의가 존재함을 확인하여 Nullable로 설계
- status `VARCHAR(20)`: 강의 상태 (`DRAFT`: 초안(신청불가) / `OPEN`: 모집 중(신청가능) / `CLOSED`: 모집마감(신청 불가))
- start_date `DATE`: 강의 시작일
- end_date `DATE`: 강의 종료일
- instructor_id `BIGINT`: 강사 식별자
- created_at `DATETIME`: 생성일시
- updated_at `DATETIME`: 수정일시

<img width="340" height="459" alt="image" src="https://github.com/user-attachments/assets/2fc92a67-bbd5-4c57-a91c-fca6a7249a46" />

### Enrollment
- id `BIGINT`: 수강 신청 고유 식별자
- course_id `BIGINT`: 강의 고유 식별자 외래키
- status `VARCHAR(20)`: 수강 신청 상태 (`PENDING`: 신청 완료, 결제 대기 /`CONFIRMED`: 결제 완료, 수강 확정 /`CANCELLED`: 취소됨)
- confirmed_at `DATETIME`: 결제 확정 일자
- cancelled_at `DATETIME`: 취소 일자
- created_at `DATETIME`: 생성일시
- updated_at `DATETIME`: 수정일시
- user_id `BIGINT`: 수강자 ID
- waitlist_position `INTEGER`: 대기열 순번

---

## 테스트 실행 방법

> 테스트 작성 후 추가 예정

---

## 미구현 / 제약사항

> 개발 완료 후 작성 예정

---

## AI 활용 범위

> 제출 전 작성 예정
