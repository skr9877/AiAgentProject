# AI Agent Chatbot

Spring Boot + WebSocket 기반 AI 상담 챗봇.  
Spring AI `ChatClient`로 자연어를 처리하고 `@Tool` 애노테이션으로 DB / 외부 API 데이터를 조회한다.  
AI 제공자(Gemini, WatsonX 등)를 코드 변경 없이 설정 파일로 교체 가능.

---

## 기술 스택

| 계층 | 기술 |
|---|---|
| 웹 프레임워크 | Spring Boot 3.4.4 |
| WebSocket | Spring WebSocket (raw, STOMP 없음) |
| AI 추상화 | Spring AI 1.1.8 (ChatClient) |
| AI 제공자 (현재) | Google Gemini (gemini-2.5-flash) |
| DB | Oracle (JDBC / NamedParameterJdbcTemplate) |
| 외부 API | RestTemplate |
| 프론트엔드 | Thymeleaf + Vanilla JS |
| 빌드 | Maven / JDK 17 |

---

## 실행

```powershell
mvn spring-boot:run
```

접속: `http://localhost:8080/chat`

---

## 설정 파일

| 파일 | 용도 | gitignore |
|---|---|---|
| `application.properties` | 서버 포트, Spring AI 설정, import 선언 | X (커밋됨) |
| `ai.properties` | Gemini API 키 (직접 생성 필요) | O |
| `db.properties` | Oracle DB 접속 정보 | X (커밋됨) |
| `application-watsonx.properties` | WatsonX 전환 샘플 설정 | X (커밋됨) |

### Gemini API 키 설정

`ai.properties.sample`을 복사해 `ai.properties`로 만든 뒤 실제 키 입력:

```bash
cp src/main/resources/ai.properties.sample src/main/resources/ai.properties
```

```properties
spring.ai.google.genai.api-key=YOUR_GEMINI_API_KEY
```

> Gemini API 키 발급: https://aistudio.google.com/app/apikey

### DB 없이 실행

`application.properties`에 아래 줄 추가:

```properties
spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
```

---

## 프로젝트 구조

```
src/main/java/com/example/chatbot/
│
├── config/
│   ├── AiConfig.java                   ChatClient Bean (Gemini 연결)
│   ├── AppConfig.java                  RestTemplate, ObjectMapper Bean
│   └── WebSocketConfig.java            /ws/{sessionId} 엔드포인트 등록
│
├── chatting/
│   ├── controller/
│   │   └── ChatController.java
│   ├── handler/
│   │   └── ChatWebSocketHandler.java   WebSocket 수신, userId 추출
│   └── service/
│       ├── ChatService.java            세션·이력 관리, ChatClient 호출
│       └── ResponseFilterService.java  AI 응답 후처리 (이미지 태그 검증)
│
└── tools/
    ├── order/
    │   ├── entity/Order.java
    │   ├── db/OrderRepository.java       JDBC: orders 테이블 조회
    │   ├── api/OrderApiClient.java       RestTemplate: 외부 주문 API 호출
    │   └── service/
    │       ├── OrderRepositoryTool.java  @Tool: DB 주문 조회
    │       └── OrderApiTool.java         @Tool: API 주문 조회
    └── user/
        ├── entity/User.java
        ├── db/UserRepository.java        JDBC: users 테이블 조회
        └── service/
            └── UserRepositoryTool.java   @Tool: 유저 조회
```

---

## Tool Calling 흐름

```
브라우저
  └─ WebSocket ──► ChatWebSocketHandler
                        │
                        ▼
                   ChatService
                        │ categories → selectTools()
                        ▼
                   Spring AI ChatClient ◄──► AI 모델 (Gemini 등)
                        │
                        │ @Tool 자동 실행 (루프 자동 처리)
                        ▼
          ┌─────────────┼─────────────┐
          ▼             ▼             ▼
  OrderRepository  OrderApiClient  UserRepository
  Tool (JDBC)      Tool (REST)     Tool (JDBC)
```

---

## 카테고리 → Tool 매핑

| 버튼 선택 | AI에게 제공되는 Tool |
|---|---|
| 주문조회 | `getOrdersByCustomerFromDb` `getOrderDetailFromDb` `getOrdersByCustomerFromApi` `getOrderDetailFromApi` |
| 유저조회 | `getUser` `searchUsers` |
| 선택 없음 | 없음 (일반 대화) |

---

## AI 제공자 전환 방법

코드(ChatService, Tool 클래스) 변경 없이 아래 2가지만 수정하면 됨.

### Gemini → WatsonX 전환

**1. `pom.xml`**
```xml
<!-- 주석 처리 -->
<!-- <dependency>spring-ai-starter-model-google-genai</dependency> -->

<!-- 주석 해제 -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-openai</artifactId>
</dependency>
```

**2. `application.properties`**
```properties
# 제거
# spring.ai.google.genai.api-key=...

# 추가 (WatsonX 모델 서버)
spring.ai.openai.base-url=http://your-model-server
spring.ai.openai.api-key=your-api-key   # → Authorization: Bearer <key> 자동 변환
```

> 상세 설정은 `application-watsonx.properties` 참고

---

## 새 Tool 추가하는 법

1. `tools/{도메인}/entity/` — 엔티티 클래스 생성
2. `tools/{도메인}/db/` 또는 `api/` — 데이터 접근 클래스 생성
3. `tools/{도메인}/service/{도메인}Tool.java` 생성 후 메서드에 `@Tool` / `@ToolParam` 추가
4. `ChatService.selectTools()` — 카테고리 switch에 새 Tool 빈 추가
5. `_body.html` — 카테고리 버튼 추가

---

## WebSocket 메시지 형식

**클라이언트 → 서버**
```json
{ "text": "주문 조회해줘", "categories": ["주문조회"] }
```

**서버 → 클라이언트**
```
고객: 주문 조회해줘
AI: 안녕하세요! ...
SYSTEM: 서버 공지
```

---

## 주요 설정값

| 키 | 기본값 | 설명 |
|---|---|---|
| `server.port` | 8080 | 서버 포트 |
| `chat.max-connections` | 100 | 최대 동시 WebSocket 세션 수 |
| `chat.max-history-turns` | 10 | 유지할 대화 이력 턴 수 |
| `spring.ai.google.genai.chat.model` | gemini-2.5-flash | 사용할 Gemini 모델 |
| `java.server.url` | http://localhost:8080 | 외부 주문 API 베이스 URL |
