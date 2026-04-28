# liveklass 과제 BE-Type-A: 수강 신청 시스템

## 프로젝트 개요

크리에이터(강사)가 강의를 개설하고, 수강생이 원하는 강의에 신청·결제·취소할 수 있는 수강 신청 시스템입니다.

**기본 구현 사항**

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

수강 취소는 결제 확정(`CONFIRMED`) 후 7일 이내에만 가능합니다.(7일째되는 당일을 포함합니다.) 이후 취소 시도는 예외를 반환합니다.


**중복 신청**

동일 사용자가 동일 강의에 `PENDING` 또는 `CONFIRMED` 상태의 신청이 존재하는 경우 중복 신청으로 간주하고 거부합니다. 취소(`CANCELLED`) 후 재신청은 허용합니다.


**정원 계산**

현재 신청 인원은 `PENDING` + `CONFIRMED` 상태의 `Enrollment` 수를 기준으로 계산합니다.


**대기 리스트**

수강 인원이 가득찬 상태에서는 등록 요청을 거부하도록 설계 하였습니다. 그렇기에 예약 API를 추가하였으며 `ENROLLMENT.STATUS`의 `WAITING` 상태를 추가하고 대기열 시스템을 구현하였습니다.

 
**대기자 자동 `PENDING` 상태 변경**

수강 인원이 가득찬 강의에서 `CANCEL`이 발생하였을 경우 가장 빠른 대기열 번호를 가진 수강 신청을 `PENDING` 상태로 변환합니다.

---

## 설계 결정과 이유

`presentation` → `application` → `domain` → `infrastructure` 4계층으로 구성합니다. 
비즈니스 규칙(상태 전이, 정원 검증, 취소 가능 여부)은 Service가 아닌 도메인 메서드 안에 위치합니다.
비즈니스 규칙을 도메인 메서드 안에 위치하도록 한 이유는 검증 규칙의 경우 Service 레이어 각 메서드별로 위치시 반복 작성되는 코드가 증가하고 이 과정 중 빠뜨릴 수 있는
규칙이 있을 수 있기 때문에 도메인 메서드 내부에 위치하였으며 재사용을 통한 동일한 규칙이 적용되도록 보장할 수 있습니다.

### Pessimistic Lock 선택 이유

동시에 여러 사용자가 마지막 자리에 신청하는 상황을 처리하기 위해 비관적 락(`SELECT FOR UPDATE`)을 선택했습니다.

수강 신청 도메인은 인기 강의 오픈 시점에 트래픽이 집중되는 특성상 충돌 빈도가 높습니다. 낙관적 락은 충돌이 드문 환경을 전제하므로, 충돌이 잦을 경우 `OptimisticLockException`를 통한 재시도가 오히려 DB 부하를 키울 가능성이 높습니다.
비관적 락은 충돌을 원천 차단하기 때문에 정원 초과를 확실하게 방지합니다.

>현재는 단일 인스턴스 환경이므로 비관적 락으로 처리하였으며 분산 환경으로 확장 시에는 Redis 분산 락 또는 메시지 큐 방식으로의 전환을 고려할 수 있습니다.

Flow: `Course` 행 잠금 → 현재 신청 인원 COUNT → 정원 검증 → `Enrollment` 저장

### 상태값 VARCHAR(20) 사용

MySQL `ENUM` 타입은 값 추가 시 DDL 변경이 필요합니다. 
`VARCHAR(20)`으로 관리하며 Application Layer에서 상태 Enum을 통해 관리합니다.

### 인덱스 설계
인덱스의 설계 기준은 카디널리티가 높으며 조회 활용도가 높고 수정빈도와 무관하게 조회 이득이 클 경우의 컬럼들을 설정하였습니다

1. ```idx_enrollment_user_id (userId)``` 
userId는 카디널리티가 높은 컬럼이며 수정 빈도가 낮으며
사용자별 enrollment 조회기능이 잦으므로 추가하였습니다.

2. ```idx_course_status (status)```
status는 카디널리티가 높진 않지만 수정빈도가 낮으며
상태별 조회 성능에 크게 영향을 주기 때문에 추가하였습니다

3. ```INDEX idx_enrollment_course_status_position (course_id, status, waitlist_position)```
```
-- countByCourseIdAndStatusIn (정원 체크)
WHERE course_id = ? AND status IN (...)

-- countUserWaitingOrder (순번 계산)
WHERE course_id = ? AND status = 'WAITING' AND waitlist_position < ?

-- findFirstByCourseIdAndStatusOrderByWaitlistPositionAsc (대기열 승격)
WHERE course_id = ? AND status = 'WAITING' ORDER BY waitlist_position ASC

-- findMaxWaitlistPositionByCourseId
WHERE course_id = ? → MAX(waitlist_position)
```
4가지의 조회 쿼리를 커버할 수 있으며

기능들의 구조상 조회보다 상태 변경이 잦은 구조기에 인덱스 갱신이 잦기에 갱신에 대한 비용이 증가하지만
정원 체크, 대기열 순번 계산이 수강 신청, 취소, 예약 기능 동작시 항상 실행되는 쿼리이며 해당 기능들은 비관적 락이 적용되어 있으므로
빠른 조회가 필요하다고 판단하여 추가하였습니다.

### waitlistPosition을 순번으로 직접 노출하지 않고 COUNT를 통해 순번을 계산합니다.
waitlistPosition 재정렬을 수행하지 않습니다.
재정렬은 대기자 전체에 대한 UPDATE가 발생하여 비용이 크기 때문에
순서 불일치를 허용하고 실제 순번은 INDEX를 활용한 COUNT 쿼리로 동적 계산합니다.

---

## API 목록 및 예시

### API 문서

1. http://localhost:8080/swagger-ui.html
``docker compose up``을 통해 서버를 구동 시킨 후 접속이 가능하며
샘플 요청/응답을 확인할 수 있습니다.

2. /docs/api-spec.json 파일을 활용
해당 파일의 정보를 복사하여
https://editor.swagger.io/
페이지로 이동 후 좌측 창에 입력시
서버를 구동하지 않고 api 명세 문서를 확인하실 수 있습니다.

---


## 데이터 모델 설명
### 전체 ERD
<img width="623" height="551" alt="image" src="https://github.com/user-attachments/assets/7db27058-1537-4fb2-a05e-6b08c64e1c2b" />

### 인덱스

``` SQL
-- 정원 COUNT 쿼리 성능
INDEX idx_enrollment_course_status_position (course_id, status, waitlist_position)

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
- status `VARCHAR(20)`: 수강 신청 상태 (`PENDING`: 신청 완료, 결제 대기 /`CONFIRMED`: 결제 완료, 수강 확정 /`CANCELLED`: 취소됨/`WAITING`: 대기중 )
- confirmed_at `DATETIME`: 결제 확정 일자
- cancelled_at `DATETIME`: 취소 일자
- created_at `DATETIME`: 생성일시
- updated_at `DATETIME`: 수정일시
- user_id `BIGINT`: 수강자 ID
- waitlist_position `INTEGER`: 대기열 순번

---

## 테스트 실행 방법

프로젝트 루트에 위치한 후
```./gradlew test```
입력

---

## 미구현 / 제약사항

1. PENDING 만료 처리 없음
대기열 기능이 있지만 결제를 하지 않은 상태인 PENDING 수강 신청들에 대한 만료 기능이 미구현 상태입니다.
@Scheduled 또는 배치 처리를 통해 일정 시간이 지난 PENDING 건을 
자동 취소하고 대기열 다음 순번에게 자리를 넘기는 방식으로 해결할 수 있습니다.

---

## AI 활용 범위

1. Docker, Docker compose 파일 구성
2. API 문서화, README.md 초안 작성 및 첨삭
3. 테스트 코드 생성 및 코드 리뷰
