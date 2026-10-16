# 훈련 성장 대시보드 API

## 제공 기능

| 경로 | 용도 |
| --- | --- |
| `GET /api/v1/dashboard/overview` | 훈련량, 경험 범위, 기간별 지표, 활동 달력 |
| `GET /api/v1/dashboard/comparison` | 전체 변화와 동일 조건 변화 |
| `GET /api/v1/dashboard/scenarios` | 시나리오별 목록 및 선택 상황의 시계열 |
| `GET /api/v1/training-records` | 기간·상황·조건에 해당하는 원래 훈련 기록 |

기존 인증 헤더를 사용한다. JSON 클라이언트는 `Accept: application/json`을 보낸다.
성공 응답은 기존 `ApiResponse` 형식인 `{ "status": 200, "data": ... }`이다.
기존 `/dashboard/metrics`, `/dashboard/weekly-summary`의 계약은 유지된다.

## 공통 조회 조건

| 파라미터 | 기본값 / 조건 |
| --- | --- |
| `from`, `to` | 둘 다 생략하면 오늘 포함 최근 28일. 지정 시 둘 다 필수. `YYYY-MM-DD` |
| `interval` | `WEEK`. `DAY` 또는 `WEEK` |
| `sessionType` | 생략하면 전체. 지정 시 `SCENARIO`, `CUSTOM` |
| `scenarioId` | 양수 시나리오 ID |
| `scenarioVersion` | 훈련 당시 저장된 시나리오 버전과 정확히 일치 |
| `difficulty` | 훈련 당시 난이도 문자열과 정확히 일치 |
| `aiPersonality` | `KIND`, `NORMAL`, `TOUGH`, `RUDE` |
| `scriptLevel` | 1: 추천 문장, 2: 조언, 3: 힌트 없음 |

- 모든 조건은 AND로 적용된다. 다른 사용자의 기록은 조회되지 않는다.
- 기간은 한국 시간 `Asia/Seoul`의 훈련 시작일 기준이며 양 끝 날짜를 포함한다.
- 기간은 2000년 이후부터 오늘까지, 최대 366일이다. 역전·미래·잘못된 형식은 400이다.
- 주간은 월요일~일요일이다. 첫 주와 마지막 주는 요청 기간에 맞게 잘린다.
- 오늘을 포함하거나 잘린 주간에는 `isPartial: true`를 반환한다.
- 시계열의 빈 날짜·주간도 반환한다. 활동 횟수는 0, 측정값은 null이다.
- `meta`는 적용 기간, 기준 기간, 간격, 필터, 시간대, `generatedAt`, `aggregationPolicyVersion`을 제공한다.
- `generatedAt`은 응답 생성 기준 시각이다. 여러 요청 사이의 데이터 고정 토큰은 아니다.

## 개요

```http
GET /api/v1/dashboard/overview?from=2026-09-20&to=2026-10-17&interval=WEEK
Accept: application/json
```

| 응답 필드 | 설명 |
| --- | --- |
| `summary.trainingCount` | 시나리오·커스텀 훈련 횟수 |
| `summary.trainingDays` | 훈련한 날짜 수. 출석 기능의 출석일과 다름 |
| `summary.totalDurationSeconds` | 사용자 발화 시간만이 아닌 전체 통화 시간 합계 |
| `summary.warmupCount` | 같은 기간·필터에 해당하는 워밍업 횟수 |
| `experience.scenarioCount` | `sessionType + scenarioId` 기준 경험한 상황 수 |
| `experience.newScenarioCount` | 남아 있는 전체 기록의 최초 훈련일이 조회 기간에 속하는 상황 수 |
| `experience.unknownScenarioTrainingCount` | 시나리오 ID가 없는 과거 훈련 수 |
| `hintSettings` | `"1"`, `"2"`, `"3"`, `"UNKNOWN"` 설정별 훈련 횟수 |
| `metrics` | 지표 키별 기간 대표값 |
| `series` | `{from,to,isPartial,trainingCount,metrics}` 목록 |
| `activity` | `{date,trainingCount}` 일별 목록 |
| `coverage` | 훈련 횟수, 자기보고 응답 수, 지표별 사용 기록 수 |

`scriptLevel=3`은 힌트 없음 설정을 뜻하며, 실제 힌트 열람 로그가 아니다.
새 시나리오 여부는 난이도 필터와 무관하게 해당 사용자의 기존 경험을 확인한다.
삭제한 기록은 최초 경험 계산에도 남지 않는다.

## 지표와 결측

| 키 | 단위 | 의미 |
| --- | --- | --- |
| `selfReportedAnxiety` | `SCORE_0_10` | 훈련 후 입력한 원래 `anxietyScore` 평균 |
| `tremorTurnRate` | `PERCENT` | AVTI 측정 턴 중 현재 떨림 판정 기준에 해당한 턴 비율 |
| `longPauseTurnRate` | `PERCENT` | 0.5초 이상 음성이 측정된 턴 중 말 내부 1.5초 이상 멈춤이 있었던 턴 비율 |
| `quietDropTurnRate` | `PERCENT` | 같은 통화의 중앙 음량보다 작아진 턴 비율 |
| `fastRateTurnRate` | `PERCENT` | 같은 통화의 중앙 발화 속도보다 빨라진 턴 비율 |
| `responseDelayMs` | `MILLISECOND` | 실제 상대 발화 재생 완료 신호 이후 대답 시작까지의 평균 시간 |

음량·속도는 같은 통화 안에서의 상대적 변화다. 절대 음량·절대 말 속도나 임상적 정상/비정상 점수가 아니다.
응답 대기는 재생 완료 ACK 등 필요한 자료가 있는 턴만 사용한다. ACK가 없는 과거 클라이언트는 결측이 될 수 있다.
지표 간 측정 가능한 턴 수가 서로 달라도 정상이다. 선택된 최대 3개 피드백 구간의 개수는 분모로 사용하지 않는다.

각 `metrics` 항목은 다음 형태다. 아래 수치는 설명용이다.

```json
{
  "value": 12.5,
  "unit": "PERCENT",
  "aggregation": "MEAN_OF_SESSION_RATES",
  "sampleCount": 4,
  "eligibleTurnCount": 32,
  "status": "READY",
  "exclusionReasons": { "MISSING_SPEECH_METRICS": 2 }
}
```

- `sampleCount`: 이 값 계산에 사용한 훈련 기록 수.
- `eligibleTurnCount`: 사용한 기록들의 측정 가능 턴 수 합계. 자기보고는 0이다.
- 비율은 통화별 비율의 평균이다. 모든 턴을 합쳐 계산한 비율과 다를 수 있다.
- 대답 시작 시간은 통화별 평균의 평균이며 중앙값이 아니다.
- 자기보고는 `MEAN_OF_SELF_REPORTS`, 대답 시작 시간은 `MEAN_OF_SESSION_MEANS`이다.
- 값과 변화량은 마지막에 소수 둘째 자리로 반올림한다.
- `scoreBefore`, `scoreAfter`, 최초 진단 점수, `scoreApplied`는 자기보고 평균에 사용하지 않는다.

| 상태 | 화면 처리 |
| --- | --- |
| `READY` | 측정값 또는 비교 결과 표시 가능 |
| `INSUFFICIENT_DATA` | 일반 집계는 1~2회 관찰값을 제공하지만 비교 변화량은 null. 표본이 적음을 표시 |
| `NO_DATA` | 측정값 없음. 값 또는 변화량은 null |
| `NOT_COMPARABLE` | 동일 조건 비교가 성립하지 않음. 변화량은 null |

null을 0으로 그리지 않는다. 0은 실제 측정된 값이다. 평균이 내려갔다는 이유만으로 회복률을 계산하지 않는다.

## 비교

```http
GET /api/v1/dashboard/comparison?from=2026-09-20&to=2026-10-17&baselineFrom=2026-08-23&baselineTo=2026-09-19
Accept: application/json
```

`baselineFrom`, `baselineTo`는 함께 지정한다. 생략하면 조회 기간 바로 이전의 같은 일수다.
기준 기간은 조회 기간보다 앞서야 하고, 두 기간의 일수가 같아야 한다.
이 파라미터는 비교와 상황별 API에서만 허용된다.

응답:

- `baselineSummary`, `currentSummary`: 기간별 훈련량.
- `metrics[]`: `{key,unit,deltaUnit,overall,matched}`.
- `baselineConditions`, `currentConditions`: `{condition,trainingCount}` 목록.
- `baselineSeries`, `currentSeries`: `{from,to,isPartial,trainingCount,matchedTrainingCounts}` 목록.

`overall`은 기간 전체의 관찰 평균을 비교한다. 서로 다른 상황·난이도 구성의 영향을 포함한다.
`matched`는 아래 조건이 모두 같은 기록의 비교다.

```text
sessionType + scenarioId + scenarioVersion + difficulty + aiPersonality + scriptLevel
```

조건에 null·알 수 없는 값이 있으면 동일 조건 비교에서 제외한다.
음성 지표는 지원하는 측정/판정 버전의 요약만 사용한다. 과거 버전과 섞지 않는다.
자기보고는 음성 요약 버전 또는 음성 요약 유무와 무관하게 사용할 수 있다.

각 조건 그룹에서 **지표별로 두 기간 각각 3회 이상**의 유효한 기록이 있어야 한다.
여러 그룹을 합칠 때 가중치는 `min(기준 기간 표본 수, 현재 기간 표본 수)`이며 두 기간에 동일하게 적용한다.
`matched.baseline/current.aggregation`은 `CONDITION_STANDARDIZED_MEAN`이다.
3회 기준은 제품의 최소 표시 정책으로, 통계적 유의성이나 치료 효과를 뜻하지 않는다.

- `overall`: `{baseline: MetricValue, current: MetricValue, delta, status}`.
- `matched`: 위 필드와 `conditionCount`, `conditions`를 포함한다.
- `conditions[]`: 조건, 기간별 표본 수, 공통 가중치, 기간별 해당 그룹 평균.
- `delta = current - baseline`. 비율 차이의 `deltaUnit`은 `PERCENTAGE_POINT`이다.
- 측정이 부족하면 관찰값을 제공할 수 있어도 delta는 null이다.
- `matched`가 성립하지 않으면 비교용 대표값과 delta가 null이며 전체 관찰값은 `overall`에서 확인한다.
- `matchedTrainingCounts`는 지표별 비교 포함 기록 수다. 전체 훈련 수와 구분해서 표시한다.

제외 사유:

| 코드 | 의미 |
| --- | --- |
| `MISSING_SELF_REPORT` | 자기보고 미입력 |
| `UNSUPPORTED_END_REASON` | 음성 비교 대상 종료 사유가 아님 |
| `MISSING_SPEECH_METRICS` | 새 음성 요약 없음 |
| `UNSUPPORTED_METRICS_VERSION` | 지원하지 않는 측정/판정 버전 |
| `UNAVAILABLE_METRIC` | 해당 지표 측정 불가 또는 해당 항목 없음 |
| `INVALID_MEASUREMENT` | 분자·분모 등 수치 계약 불일치 |
| `UNKNOWN_CONDITION` | 비교 조건 누락 |
| `NO_SHARED_CONDITION` | 상대 기간에 같은 조건의 유효한 측정 없음 |
| `LOW_SAMPLE_SIZE` | 같은 조건이 있지만 한쪽 기간의 유효 표본이 3회 미만 |

하나의 기록은 각 지표별로 하나의 제외 사유에만 포함된다.
`sampleCount + exclusionReasons 합계`는 해당 기간의 훈련 수다.

## 상황별 조회

```http
GET /api/v1/dashboard/scenarios?page=0&size=20
GET /api/v1/dashboard/scenarios?scenarioId=12&includeSeries=true&interval=DAY
```

- 기본 page=0, size=20. page는 0 이상, size는 1~50이다.
- 현재 조회 기간에 훈련한 상황만 목록에 포함한다. `sessionType + scenarioId`로 묶는다.
- 정렬은 최근 훈련일 내림차순, 동률이면 sessionType·scenarioId 오름차순이다.
- 시나리오 ID가 없는 기록은 목록에서 제외하고 개요의 unknownScenarioTrainingCount로 알린다.
- `includeSeries` 기본값은 false. true이면 scenarioId가 필요하다.
- 목록 응답: `{meta,items,page,size,totalElements,totalPages}`.
- 항목: `{scenarioId,sessionType,scenarioName,summary,firstTrainedAt,lastTrainedAt,conditionBreakdown,metrics,comparison,series}`.
- `firstTrainedAt`은 남아 있는 전체 기록 기준 최초 훈련 시각이다. `lastTrainedAt`은 조회 기간의 최근 시각이다.
- 이름은 조회 기간의 가장 최근 훈련에 저장된 이름이다. 현재 시나리오 편집으로 과거 이름·버전을 덮어쓰지 않는다.
- `conditionBreakdown`으로 여러 버전·난이도·성격·힌트 설정을 구분한다.
- `comparison`은 비교 API와 같은 지표별 overall/matched 구조다.
- series를 요청하지 않으면 `[]`을 반환한다.

## 훈련 기록 연결

```http
GET /api/v1/training-records?from=2026-09-20&to=2026-10-17&scenarioId=12&difficulty=HARD&scriptLevel=3&page=0&size=20
```

기간·sessionType·scenarioId·scenarioVersion·difficulty·aiPersonality·scriptLevel로 필터링한다.
필터를 생략하면 기존 전체 기록 목록 동작을 유지한다. 기간은 지정할 때만 양쪽 날짜를 함께 넣는다.
기록 목록에서는 WARMUP도 조회할 수 있다. 기간 검증은 대시보드와 같다.
이 목록은 조건에 해당하는 원본 기록이므로, 측정 불가·오류 종료 등 대시보드 집계에서 제외된 기록도 포함한다.
따라서 목록 수를 지표 sampleCount로 사용하지 않는다. 기존 기록 상세 API로 피드백과 녹음을 연다.

## 활동과 측정의 적용 범위

- 활동: SCENARIO/CUSTOM 중 SCENARIO_DONE, USER_END, END_CALL, TIMEOUT.
- 워밍업은 위 종료 사유에 해당할 때 별도 warmupCount에만 포함한다.
- ERROR, NO_AUDIO, CRISIS는 성장 집계에 포함하지 않는다.
- 음성 지표: 활동 중 SCENARIO_DONE, END_CALL, TIMEOUT이며 지표별 측정 계약이 유효한 기록.
- 오래된 기록도 활동과 자기보고에는 사용할 수 있다. 새 음성 요약을 과거 기록에서 임의로 추정하지 않는다.

## 내부 연동과 배포

기존 종료 콜백의 선택 필드 `speech_metrics`를 사용한다. 내부 필드는 snake_case이다.
스키마 버전 1, 측정 버전 FEEDBACK_METRICS_V1, 판정 버전 FEEDBACK_POLICY_V1을 저장한다.
각 지표는 status, eligible_turn_count, affected_turn_count 또는 total_delay_ms를 갖는다.
FastAPI가 생성한 요약을 Spring의 training_records.speech_metrics JSONB 컬럼에 저장한다.
요약 생성 실패 시 기존 종료 콜백은 요약 없이 진행한다. 동일 세션의 재시도는 기존 중복 방지 정책을 따른다.

배포 순서는 **컬럼 준비 → Spring 배포 → FastAPI 배포 → 신규 조회 연결**이다.
운영 DB용 SQL은 소스에 자동 실행 파일로 포함하지 않는다. 배포 담당자에게 별도로 전달한다.
기존 행은 컬럼이 null이며, 새 통화부터 음성 요약이 쌓인다.

실제 JSONB/QueryDSL 테스트는 폐기 가능한 로컬 PostgreSQL의 dashboard_test DB에서만 실행한다.
테스트 환경변수는 DASHBOARD_TEST_DATABASE_URL이며 미지정 시 해당 통합 테스트를 건너뛴다.
테스트는 create-drop을 사용하므로 운영 DB를 지정할 수 없게 URL을 검증한다.
