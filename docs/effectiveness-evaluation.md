# 사용자 효과 평가 API

이 문서는 계정 선택과 평가 실행, 배포 순서, 기존 자료의 제한을 설명한다.
모든 관리자 API는 기존 `/admin/**` ADMIN 권한 검사를 사용한다.

## 배포

별도 SQL 파일은 필요하지 않다. 운영 DB에는 아래 SQL을 담당자가 직접 실행한다.
현재 저장소의 `ddl-auto=update` 설정을 운영에서도 유지하고 애플리케이션 DB 계정에 DDL 권한이 있다는 전제이다. 운영에서 이를 재정의했다면 새 컬럼·테이블도 Spring 배포 전에 별도로 생성해야 한다.

1. Spring 배포 전에 기존 CHECK 제약 이름을 확인한다.

```sql
SELECT conname, pg_get_constraintdef(oid) AS definition
FROM pg_constraint
WHERE conrelid = 'training_records'::regclass
  AND contype = 'c'
  AND pg_get_constraintdef(oid) LIKE '%analysis_quality_status%';
```

2. Spring 배포 전에 다음 SQL을 실행한다. 위 조회에서 기존 제약 이름이 다르면 DROP CONSTRAINT 대상을 실제 이름으로 바꾼다. 관련 제약이 여러 개라면 모두 PARTIAL을 허용하는지 확인한다.

```sql
BEGIN;
ALTER TABLE training_records
    ALTER COLUMN performance_score TYPE numeric(7,4);
ALTER TABLE training_records
    DROP CONSTRAINT IF EXISTS training_records_analysis_quality_status_check;
ALTER TABLE training_records
    ADD CONSTRAINT training_records_analysis_quality_status_check
    CHECK (analysis_quality_status IN ('PASS', 'PARTIAL', 'FAIL'));
COMMIT;
```

3. Spring을 먼저 배포한다. `ddl-auto=update`가 훈련 측정 컬럼, 설문 버전·응답 컬럼, evaluation_cohorts / evaluation_participants / evaluation_runs 테이블을 생성했는지 시작 로그와 DB에서 확인한다. PARTIAL과 새 측정 필드를 수신할 수 있어야 한다.
4. FastAPI를 배포한다. 새 분석은 SPEECH_ANALYZER_V3 / ANALYSIS_POLICY_V3이다.

Hibernate `ddl-auto=update`만으로 기존 숫자 범위나 CHECK 변경이 적용된다고 가정하지 않는다. 두 서버의 새 기능을 배포해도 효과 평가 대상 계정은 자동 지정되지 않으며 관리자 API에서 선택한다.

Spring 배포 후 과거 불안 입력 시각을 채우려면 다음 SQL을 선택적으로 실행한다. 평가 계산은 anxiety_recorded_at이 없으면 score_applied_at을 사용하므로 필수는 아니다.

```sql
UPDATE training_records
SET anxiety_recorded_at = score_applied_at
WHERE anxiety_recorded_at IS NULL
  AND anxiety_score IS NOT NULL
  AND score_applied_at IS NOT NULL;
```

조회 성능이 필요할 때 다음 인덱스를 선택적으로 추가한다. 새 컬럼·테이블이 만들어진 Spring 배포 후 실행한다. 기능 실행의 필수 조건은 아니다.

```sql
CREATE INDEX IF NOT EXISTS ix_training_evaluation_user_end ON training_records(user_id, ended_at);
CREATE INDEX IF NOT EXISTS ix_diagnosis_evaluation_user_time ON diagnosis_results(user_id, created_at);
CREATE INDEX IF NOT EXISTS ix_evaluation_runs_results ON evaluation_runs USING gin(results);
CREATE INDEX IF NOT EXISTS ix_evaluation_participant_user ON evaluation_participants(user_id);
```

과거 난이도·시나리오 버전·문항별 응답·누락된 입력 시각은 추정해서 채우지 않는다.
입력 시각을 복원할 때도 기존 반영 시각이 있는 불안 입력만 사용한다.
FastAPI를 먼저 이전 버전으로 되돌린 뒤 Spring을 되돌릴 수 있지만, 이미 저장된 PARTIAL 자료를 읽을 수 없는 구버전 Spring으로의 복귀는 별도 데이터 호환 조치가 필요하다. DB 컬럼과 새 테이블은 즉시 삭제하지 않는다.

## 계정과 명단

| API | 역할 |
| --- | --- |
| GET `/admin/evaluations/accounts?search=&page=0&size=30` | 이름·아이디·이메일로 계정 검색 |
| GET `/admin/evaluations/accounts/{userId}/assessments` | 해당 계정의 검사 ID, 점수, 문항 응답·버전 확인 |
| GET `/admin/evaluations/cohorts` | 저장한 참여자 명단 확인 |
| POST `/admin/evaluations/cohorts` | 명단 생성 |
| PUT `/admin/evaluations/cohorts/{id}/participants` | 명단의 계정·참여 기간 교체 |
| POST `/admin/evaluations/runs` | 평가 실행 및 결과 고정 |
| GET `/admin/evaluations/runs` | 실행 목록 조회 |
| GET `/admin/evaluations/runs/{id}` | 고정된 조건·개인별 결과·요약 조회 |
| GET `/admin/evaluations/runs/{id}/export` | 같은 실행의 개인별 지표 CSV |

명단 생성:

```json
{"name":"2026년 10월 베타"}
```

참여자 지정:

```json
{
  "members": [
    {"userId":101,"source":"BETA","startedAt":"2026-10-09T09:00:00"},
    {"userId":102,"source":"BETA","startedAt":"2026-10-15T09:00:00","endOverride":"2026-10-29T09:00:00"}
  ]
}
```

유입 분류는 BETA / ORGANIC / UNKNOWN이다. 베타 명단 소속과 계정의 가입 정보를 자동으로 동일시하지 않는다.
여러 명단과 직접 선택한 계정은 userId로 중복 제거한다. 여러 명단의 분류가 다르면 두 분류를 모두 유지하므로 분류별 인원의 합은 전체 인원보다 클 수 있다.
중복 명단의 개인별 기간이 다르면 명시적인 개인 설정이 필요하다. 제외가 포함보다 우선하며 제외 사유를 저장한다. 빈 선택은 오류이다.

## 평가 실행 예시

재검사와 훈련 중 불안 변화:

```json
{
  "name":"베타 및 일반 유입 14일 평가",
  "cohortIds":[1],
  "userIds":[201,202],
  "excludedUsers":{"202":"내부 테스트 계정"},
  "participantOverrides":{
    "201":{"source":"ORGANIC","startedAt":"2026-10-10T09:00:00"}
  },
  "policy":{
    "windowMode":"ELAPSED_DAYS",
    "anchor":"PARTICIPATION",
    "targetCount":14,
    "metrics":["SELF_ASSESSMENT","SUBJECTIVE_ANXIETY"],
    "grouping":"DAY",
    "earlyCount":3,
    "lateCount":3,
    "baselineLookbackDays":7,
    "assessmentGraceDays":3,
    "allowLegacyAssessments":false,
    "matchTrainingConditions":true
  }
}
```

`asOf`를 생략하면 실행 시점이다. 미래 시점은 허용하지 않는다. 평가가 끝나지 않은 사람은 NOT_DUE로 남는다.
이 예시의 14일과 3일은 설정 예시이며 효과 검증을 보장하는 기준이 아니다.
`participantOverrides`에는 `baselineAssessmentId`, `postAssessmentId`를 지정할 수 있다. 검사 소유자·시각·설문 버전은 자동 선택과 동일하게 검증한다.

### 기간 설정

| windowMode | 종료 조건 |
| --- | --- |
| FIXED_PERIOD | policy.start / policy.end |
| ELAPSED_DAYS | 개인별 시작 이후 targetCount일 |
| TRAINING_DAYS | 서로 다른 targetCount일의 참여 인정 훈련 |
| TRAINING_SESSIONS | targetCount회의 참여 인정 훈련 |

개인별 시작은 PARTICIPATION / FIRST_TRAINING / FIRST_ASSESSMENT 중 선택한다.
참여일이 없는데 PARTICIPATION을 선택하면 MISSING_ANCHOR이다. 임의로 가입일을 대신 사용하지 않는다.
마일스톤의 최대 관찰 기간은 horizonDays이며 기본 90일이다. 기간 안에 아직 조건을 못 채우면 NOT_DUE, 관찰 기간이 끝났으면 MILESTONE_NOT_REACHED이다.
개인별 endOverride는 종료 시점을 대체한다. 기간 길이가 다른 결과는 별도 strata로 집계한다.
훈련은 시작이 관찰 시작 이후이고 종료가 관찰 종료 이내인 기록을 사용한다. 경계는 양 끝을 포함한다. 인접한 기간을 별도 실행할 때 같은 경계 시각의 기록이 중복 사용되지 않도록 지정해야 한다.
날짜는 기존 서버의 LocalDateTime 기준이며 운영 서버는 Asia/Seoul을 사용해야 한다. 자정을 넘긴 세션은 종료일에 한 번 귀속한다.

### 참여와 분석의 조건

참여 인정은 policy.participation에서 별도로 설정한다.

```json
{
  "participation":{
    "sessionTypes":["SCENARIO","CUSTOM"],
    "endReasons":["SCENARIO_DONE","USER_END","END_CALL","TIMEOUT"],
    "minDurationSeconds":0
  }
}
```

기본값은 위 예시와 같다. ERROR / NO_AUDIO는 기본 참여 집계에서 제외한다. 이는 새로운 보상 확정 규칙이 아니며 이미 공지한 베타 운영 조건과 대조해서 사용해야 한다.
participation은 음성 분석 성공이나 불안 응답을 요구하지 않는다. 난이도·분석 버전 필터도 참여 일수에 영향을 주지 않는다.
분석할 훈련은 별도의 sessionTypes / endReasons / difficulty / scenarioId / scenarioVersion / analyzerVersion / analysisPolicyVersion으로 제한한다. 기본 분석 종료 사유는 SCENARIO_DONE / END_CALL / TIMEOUT이며 USER_END는 불완전 종료 음성 분석과 구분한다.

### 지표와 해석

| 지표 | 척도 | 개선 방향 |
| --- | --- | --- |
| SELF_ASSESSMENT | 동일 설문 1~5 평균 | 감소 |
| SUBJECTIVE_ANXIETY | 훈련 후 자기보고 0~10 | 감소 |
| STABILITY / CONVERSATION / FLUENCY | 동일 버전의 음성 수행 0~100 | 증가 |

자가진단은 시작 이전 baselineLookbackDays 안의 마지막 SIGNUP 검사와 종료 ±assessmentGraceDays 안에서 종료 시점에 가장 가까운 검사를 연결한다. 같은 시각은 생성 ID로 순서를 정한다. 사후 검사는 사전 검사 이후여야 한다.
훈련 지표는 초기 earlyCount개와 후기 lateCount개를 비교하며 두 구간이 겹치면 평가하지 않는다. DAY는 일별 평균을 먼저 계산하므로 하루에 많이 훈련한 사람이 가중되지 않는다.
matchTrainingConditions=true이면 난이도·시나리오 버전·힌트 수준·AI 성격이 같은 기록만 전후 비교할 수 있다. 조건 누락·불일치는 사유로 반환한다. false는 조건 차이를 허용하는 탐색적 설정이다.
음성 분석 버전과 분석 정책 버전은 설정과 관계없이 전후 일치해야 한다. PARTIAL에서도 해당 지표가 PASS면 개별 평가에 사용할 수 있다.
주관적 불안은 음성 PASS나 앱 누적 지수 반영에 종속되지 않는다. 응답 시각이 집계 시점 이후이거나 알 수 없으면 사용하지 않는다. 훈련 종료 전 응답도 제외한다. maximumSubjectiveDelayMinutes의 기본값은 60으로, 종료 후 60분 안에 제출한 응답을 사용한다. 0을 지정하면 응답 지연 제한을 해제하는 탐색적 평가가 가능하다.

`change = before - after`로 저장한다. 따라서 음성 수행이 좋아지면 change가 음수일 수 있으며 direction=IMPROVED로 해석한다.
thresholds를 지정하지 않으면 방향만 보고한다. 기준을 지정하면 지표의 개선 방향으로 해당 변화량을 충족하는지 meetsThreshold로 추가 보고한다. 임의 기준을 검증된 임상적 개선 기준으로 취급하지 않는다.

```json
{"thresholds":{"SELF_ASSESSMENT":0.2,"SUBJECTIVE_ANXIETY":1,"STABILITY":5}}
```

선택 인원, 참여 인원, 10일 참여자와 지표별 비교 가능 인원은 분리한다. SELF_ASSESSMENT를 선택하면 baselineRecordedUsers / postRecordedUsers로 사전·사후 검사 기록 인원을 함께 제공한다. 버전 불일치나 미완료로 비교 불가능한 응답도 실제 기록이 있으면 해당 기록 인원에 포함된다. 미응답은 변화 없음으로 계산하지 않는다.
meanBefore / meanAfter는 동일한 비교 대상자이며 meanChange / medianChange는 개인별 변화의 집계이다.
ci95Lower / ci95Upper는 사람 단위로 2,000회 복원 추출한 평균 변화의 percentile bootstrap 구간이다. 한 명 이하는 null이다. 작은 표본의 구간을 효과 확정으로 해석하지 않는다.
설문·분석 버전이나 평가 기간이 다른 사람이 섞이면 comparable=false와 strata를 반환하며 전체 평균·개선율·구간은 null이다.

## 기존 앱과 새 설문 형식

GET `/api/diagnosis/questions?type=SIGNUP`의 각 문항에 questionnaireVersion이 추가된다.
새 앱은 표시한 버전과 문항 ID별 응답을 함께 제출한다.

```json
{
  "sessionId":"클라이언트에서 생성한 검사 UUID",
  "type":"SIGNUP",
  "questionnaireVersion":"GET 응답의 questionnaireVersion",
  "answersByQuestionId":[{"questionId":1,"value":4},{"questionId":2,"value":3}]
}
```

실제 문항 전체를 제출해야 한다. 기존 answers 배열 형식도 계속 지원하지만 두 형식을 동시에 제출할 수 없다.
결과는 문항 내용·순서·값을 원본과 함께 저장한다. 문항 ID 형식에서는 조회한 버전이 현재 문항과 다르면 제출을 거절한다.
동일 사용자·검사 UUID의 동일 응답 재전송은 기존 결과를 반환한다. 다른 응답이면 충돌이다.
응답에는 diagnosisResultId와 questionnaireVersion이 추가된다.
설문 버전을 보내지 않은 기존 앱 응답은 questionnaireVersionConfirmed=false이다. 과거 검사 또는 확인되지 않은 버전을 평가하려면 allowLegacyAssessments=true를 명시해야 하며 결과 버전에는 LEGACY_UNVERIFIED가 표시된다. 원본이 없었던 과거 응답은 복원하지 않는다.

앱에서는 실제 선택 전 기본 점수를 제출하지 않도록 하고, 저장 실패를 표시·재시도해야 한다. 사후 검사 안내도 앱 연동 작업이다. 이 두 서버 저장소에는 모바일 화면 변경이 포함되지 않는다.
실제 전화 행동 및 말 끊기·리포트 사용 경험은 별도 설문/인터뷰로 수집할 항목이며 현재 자동 평가 지표 5개에는 포함되지 않는다. 말 끊기 비활성 상태를 사용자 경험 자료로 간주하지 않는다.

## 삭제와 기존 통계

훈련 삭제는 상태 -> 기록 잠금 순서로 점수 반영과 직렬화한다. 남은 반영 기록을 기존 순서로 재계산하고 순서 번호·최초 반영 시각은 보존한다. 새 반영 번호는 현재 최대 번호 다음이다.
재현할 수 없는 계산 버전·원본은 삭제 전에 충돌 응답으로 알린다. 외부 녹음/피드백 삭제 실패 시 기존 트랜잭션 오류를 반환한다. 외부 리소스 삭제와 DB 롤백의 원자성은 기존 흐름의 제한이며 별도 재시도 작업 설계가 필요하다.
원본 삭제와 같은 트랜잭션에서 관련 평가의 해당 사용자 파생 자료를 제거하고 invalidated=true로 표시한다. 무효화된 실행은 요약·CSV를 제공하지 않으며 새로 실행해야 한다. 회원의 전체 훈련 자료 삭제는 해당 사용자의 평가 파생 자료도 제거한다.
조회 시에도 원본 존재 여부를 확인해 실행과 삭제가 겹친 경우를 보완한다.
기존 `/admin/statistics/call-anxiety`는 호환을 위해 유지한다. improvementRate는 앱 내부 혼합 지수 변화이며 효과 평가에는 새 `/admin/evaluations/runs`를 사용한다.

## 검증 실행

Spring 관련 회귀 테스트:

```sh
./gradlew test --tests '*domain.adminstatistics.*' --tests '*domain.diagnosis.*' --tests '*domain.callanxiety.*' --tests '*domain.trainingrecord.*' --tests '*TrainingRecordSessionRecordAdapterTest'
```

임시 PostgreSQL 전용 DB에서의 영속성 테스트(운영 DB 사용 금지, 테스트에서 테이블 생성·삭제):

```sh
BADA_EVALUATION_TEST_DB_URL=jdbc:postgresql://127.0.0.1:55491/bada_evaluation_test BADA_EVALUATION_TEST_DB_USER=postgres ./gradlew test --tests '*EvaluationPersistenceTest'
```

FastAPI:

```sh
python -m pytest tests/unit tests/integration -q
ruff check app tests/unit tests/integration
```
