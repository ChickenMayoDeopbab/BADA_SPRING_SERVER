# 바다 Spring Server
Spring Boot & Java 기반 백엔드 서버입니다.<br>
회원 관리, 인증(JWT/OAuth2), 자가진단, 훈련 기록 저장 등 핵심 비즈니스 로직과 API를 제공합니다.

<br>

# 사용 스택

| 분야 | 기술 |
| --- | --- |
| Framework | Spring Boot |
| Language | Java 21 |
| Database | PostgreSQL, Redis |
| Persistence | Spring Data JPA, QueryDSL |
| Security | Spring Security, OAuth2, JWT |
| AI | OpenAI |
| API Docs | springdoc-openapi |
| Storage | AWS S3 |
| Push | Firebase Cloud Messaging |

<br>

# AI 사용 내역

| 구분 | 활용 내용 |
| --- | --- |
| 자가진단 결과 요약 | 자가진단 문항과 사용자 응답을 종합해 전화 통화 성향과 부담감을 요약 |

<br>

### 개발 과정의 AI 활용

| 도구 | 활용 내용 |
| --- | --- |
| Claude | 기능 구현, 오류 수정, 테스트 코드 작성 보조 |

<br>

# 사용한 AI 모델

| 구분 | 모델 | 용도 |
| --- | --- | --- |
| LLM | GPT-4o-mini | 콜포비아 자가진단 응답 결과 요약 |

<br>

# 오픈소스 패키지

| 구분 | 패키지 |
| --- | --- |
| 웹·검증 | Spring Boot, Spring Web, Spring Validation |
| 인증·보안 | Spring Security, Spring OAuth2 Client, JJWT |
| DB | Spring Data JPA, Spring Data Redis, PostgreSQL JDBC |
| 쿼리 | QueryDSL |
| 코드 생성 | Lombok, QueryDSL APT, Jakarta Annotation API, Jakarta Persistence API |
| API 문서 | springdoc-openapi |
| 메일·템플릿 | Spring Mail, Thymeleaf |
| AI SDK | OpenAI Java SDK |
| AWS 연동 | AWS SDK for Java v2 (S3, KMS) |
| 푸시 알림 | Firebase Admin SDK |
| 테스트 | Spring Boot Test, Spring Security Test, JUnit Platform |

<br>

# 저장소 구조
**Spring Server** - 현재 저장소<br>
백엔드 서버로 핵심 비즈니스 로직과 API를 담당합니다.<br>
https://github.com/ChickenMayoDeopbab/BADA_SPRING_SERVER<br>

**Fast API**<br>
바다의 AI 서버로 실시간 음성 인식(STT), LLM 기반 대화 생성, 음성 합성(TTS)과 같은 AI 전화 훈련 파이프라인을 담당합니다.<br>
https://github.com/ChickenMayoDeopbab/BADA_FASTAPI_SERVER<br>

**App**<br>
React Native(Expo) 기반 모바일 애플리케이션입니다.<br>
https://github.com/ChickenMayoDeopbab/BADA_APP<br>
