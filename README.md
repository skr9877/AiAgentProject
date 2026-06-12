# AI Agent Chatbot

Spring Boot + WebSocket 기반 AI 상담 챗봇. Gemini API로 자연어를 처리하고 Tool Calling으로 DB / 외부 API 데이터를 조회한다.

---

## 기술 스택

| 계층 | 기술 |
|---|---|
| 웹 프레임워크 | Spring Boot 3.2.6 |
| WebSocket | Spring WebSocket (raw, STOMP 없음) |
| AI | Google Gemini API (gemini-2.5-flash) |
| DB | Oracle (JDBC / NamedParameterJdbcTemplate) |
| 외부 API | RestTemplate |
| 프론트엔드 | Thymeleaf + Vanilla JS |
| 빌드 | Maven / JDK 17 |

---

## 실행

```powershell
# JAVA_HOME을 JDK 17로 지정
$env:JAVA_HOME = "D:\Programming\jdk\jdk17"
mvn spring-boot:run -Dfile.encoding=UTF-8
```

접속: `http://localhost:8080`

---

## 설정 파일

| 파일 | 용도 |
|---|---|
| `application.properties` | 서버 포트, 기본값, DB/AI 설정 구조 |
| `ai.properties` | 실제 API 키 / DB 접속 정보 (gitignore 권장) |

**ai.properties 예시**

```properties
gemini.api-key=YOUR_KEY
gemini.model=gemini-2.5-flash
gemini.endpoint-base=https://generativelanguage.googleapis.com/v1

spring.datasource.url=jdbc:oracle:thin:@HOST:1521/ORCL
spring.datasource.username=admin
spring.datasource.password=password
```

Oracle 없이 실행할 때는 `application.properties`의 `spring.autoconfigure.exclude` 줄을 그대로 두면 DB 없이 기동된다.

---

## 프로젝트 구조

```
src/main/java/com/example/chatbot/
│
├── config/
│   ├── AppConfig.java              RestTemplate Bean (타임아웃 설정)
│   └── WebSocketConfig.java        /ws/{sessionId} 엔드포인트 등록
│
├── controller/
│   └── ChatController.java         GET / → chat.html, sessionId UUID 발급
│
├── handler/
│   └── ChatWebSocketHandler.java   WebSocket 수신 → ChatService 위임
│
├── model/                          AI 통신 전용 DTO
│   ├── ChatRequest.java            { text, categories[] }
│   └── ChatResponse.java           { message }
│
├── service/
│   ├── ChatService.java            세션 맵 관리 + 대화 이력 유지 + AI 응답 조율
│   ├── GeminiService.java          Gemini REST API 호출 / 응답 파싱
│   └── ResponseFilterService.java  AI 응답 후처리 (이미지 태그 검증)
│
└── tools/
    ├── registry/                   툴 인프라
    │   ├── ToolDefinition.java     툴 스펙 (name / description / parameters)
    │   └── ToolRegistry.java       카테고리 → 툴 매핑 + dispatch()
    │
    ├── order/                      주문 도메인
    │   ├── Order.java              주문 모델
    │   ├── OrderRepositoryTool.java  DB 툴 → get_orders_by_customer_db, get_order_detail_db
    │   ├── OrderApiTool.java         API 툴 → get_orders_by_customer_api, get_order_detail_api
    │   ├── db/
    │   │   └── OrderRepository.java  JDBC: orders 테이블 조회
    │   └── api/
    │       └── OrderApiClient.java   RestTemplate: 외부 주문 API 호출
    │
    └── user/                       유저 도메인
        ├── User.java               유저 모델
        ├── UserRepositoryTool.java   DB 툴 → get_user, search_users
        └── db/
            └── UserRepository.java   JDBC: users 테이블 조회
```

---

## Tool Calling 흐름

```
브라우저
  └─ WebSocket ──► ChatWebSocketHandler
                        │
                        ▼
                   ChatService
                        │ categories 전달
                        ▼
                   GeminiService ◄──► Gemini API
                        │
                        │ tool_call 수신 시
                        ▼
                   ToolRegistry.dispatch()
                        │
          ┌─────────────┼─────────────┐
          ▼             ▼             ▼
  OrderRepository  OrderApiClient  UserRepository
  Tool (JDBC)      Tool (REST)     Tool (JDBC)
```

---

## 카테고리 → 툴 매핑

| 버튼 선택 | AI에게 제공되는 툴 |
|---|---|
| 주문조회 | `get_orders_by_customer_db` `get_order_detail_db` `get_orders_by_customer_api` `get_order_detail_api` |
| 유저조회 | `get_user` `search_users` |
| 선택 없음 | 전체 6개 툴 |

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

## 새 Tool 추가하는 법

1. `tools/{도메인}/` 아래 모델, Repository/ApiClient 생성
2. `{도메인}Tool.java`에 `TOOL_SPECS` 정의
3. `ToolRegistry` — `getToolsForCategories()` switch에 카테고리 추가, `dispatch()` case 추가
4. `chat.html` — 카테고리 버튼 추가

---

## 주요 설정값

| 키 | 기본값 | 설명 |
|---|---|---|
| `server.port` | 8080 | 서버 포트 |
| `chat.max-connections` | 100 | 최대 동시 WebSocket 세션 수 |
| `chat.max-history-turns` | 10 | 유지할 대화 이력 턴 수 |
| `java.server.url` | http://localhost:8080 | 외부 주문 API 베이스 URL |
