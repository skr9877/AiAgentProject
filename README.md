# AI Agent Chatbot

Spring Boot + WebSocket 기반 AI 상담 챗봇.  
Google Gemini API로 자연어를 처리하고 Tool Calling으로 DB / 외부 API 데이터를 조회한다.

---

## 기술 스택

| 계층 | 기술 |
|---|---|
| 웹 프레임워크 | Spring Boot 3.x |
| WebSocket | Spring WebSocket (raw, STOMP 없음) |
| AI | Google Gemini API (gemini-2.5-flash) |
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
| `application.properties` | 서버 포트, 공통 설정, import 선언 | X (커밋됨) |
| `ai.properties` | Gemini API 키 (직접 생성 필요) | O |
| `ai.properties.sample` | ai.properties 샘플 (참고용) | X (커밋됨) |
| `db.properties` | Oracle DB 접속 정보 | X (커밋됨) |

세 파일 모두 서버 시작 시 Spring Boot가 직접 읽는다. (`spring.config.import`)

### ai.properties 설정 방법

```bash
# sample 파일을 복사해서 실제 파일 생성
cp src/main/resources/ai.properties.sample src/main/resources/ai.properties
```

이후 `ai.properties`에 실제 Gemini API 키 입력:

```properties
gemini.api-key=YOUR_GEMINI_API_KEY
gemini.model=gemini-2.5-flash
gemini.endpoint-base=https://generativelanguage.googleapis.com/v1beta
```

> Gemini API 키 발급: https://aistudio.google.com/app/apikey

### db.properties 설정

Oracle DB 사용 시 `db.properties` 수정 후 `application.properties`의 아래 줄 제거:

```properties
spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
```

DB 없이 실행할 때는 위 줄을 그대로 두면 된다.

---

## 프로젝트 구조

```
src/main/java/com/example/chatbot/
│
├── config/
│   ├── AppConfig.java                  RestTemplate, ObjectMapper Bean
│   └── WebSocketConfig.java            /ws/{sessionId} 엔드포인트 등록
│
├── chatting/                           채팅 담당
│   ├── controller/
│   │   └── ChatController.java         GET → userId="user" / POST → userId=파라미터
│   ├── handler/
│   │   └── ChatWebSocketHandler.java   WebSocket 수신, userId 추출, 로그
│   └── service/
│       └── ChatService.java            세션·이력·userId 맵 관리, AI 응답 조율
│
├── gemini/                             Gemini AI 통신 담당
│   └── service/
│       ├── GeminiService.java          Gemini REST API 호출 / 응답 파싱
│       └── ResponseFilterService.java  AI 응답 후처리 (이미지 태그 검증)
│
└── tools/
    ├── registry/
    │   ├── ToolDefinition.java         툴 스펙 (name / description / parameters)
    │   └── ToolRegistry.java           카테고리 → 툴 매핑 + dispatch()
    │
    ├── order/                          주문 도메인
    │   ├── model/
    │   │   └── Order.java
    │   ├── service/
    │   │   ├── OrderRepositoryTool.java  get_orders_by_customer_db, get_order_detail_db
    │   │   └── OrderApiTool.java         get_orders_by_customer_api, get_order_detail_api
    │   ├── repository/
    │   │   └── OrderRepository.java      JDBC: orders 테이블 조회
    │   └── client/
    │       └── OrderApiClient.java       RestTemplate: 외부 주문 API 호출
    │
    └── user/                           유저 도메인
        ├── model/
        │   └── User.java
        ├── service/
        │   └── UserRepositoryTool.java   get_user, search_users
        └── repository/
            └── UserRepository.java       JDBC: users 테이블 조회
```

---

## 사용자 접속 방식

| 방식 | URL | userId |
|---|---|---|
| GET | `http://localhost:8080/chat` | `"user"` (기본값) |
| POST | `http://localhost:8080/chat` + `userId=파라미터` | 전달된 값 사용 |

userId는 WebSocket 연결 시 쿼리 파라미터로 서버에 전달되며 로그에 기록된다.

```
[heesuk1125.kim] 세션 연결 (현재 1개)
[heesuk1125.kim] 사용자 입력: 주문 조회해줘
[heesuk1125.kim] AI 최종 답변: 안녕하세요! ...
[heesuk1125.kim] 세션 해제 (현재 0개)
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

1. `tools/{도메인}/model/` — 엔티티 클래스 생성
2. `tools/{도메인}/repository/` 또는 `client/` — 데이터 접근 클래스 생성
3. `tools/{도메인}/service/` — `{도메인}Tool.java`에 `TOOL_SPECS` 정의
4. `ToolRegistry` — `getToolsForCategories()` switch에 카테고리 추가, `dispatch()` case 추가
5. `_body.html` — 카테고리 버튼 추가

---

## 주요 설정값

| 키 | 기본값 | 설명 |
|---|---|---|
| `server.port` | 8080 | 서버 포트 |
| `chat.max-connections` | 100 | 최대 동시 WebSocket 세션 수 |
| `chat.max-history-turns` | 10 | 유지할 대화 이력 턴 수 |
| `gemini.model` | gemini-2.5-flash | 사용할 Gemini 모델 |
| `java.server.url` | http://localhost:8080 | 외부 주문 API 베이스 URL |
