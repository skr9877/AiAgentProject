# AI Agent Chatbot

Spring Boot + WebSocket 기반 AI 상담 챗봇.  
Spring AI `ChatClient`로 자연어를 처리하고 `@Tool` 애노테이션으로 DB / 외부 API 데이터를 조회한다.

---

## 기술 스택

| 계층 | 기술 |
|---|---|
| 웹 프레임워크 | Spring Boot 3.1.12 |
| WebSocket | Spring WebSocket (raw, STOMP 없음) |
| AI 추상화 | Spring AI 1.1.8 (ChatClient) |
| AI 제공자 (현재) | Google Gemini (gemini-2.5-flash) |
| DB | Oracle XE 21c (MyBatis) |
| ORM | MyBatis 3.0.3 + mybatis-spring-boot-starter |
| 공통 유틸 | Lombok |
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
| `application.properties` | 서버 포트, Spring AI 설정 | X (커밋됨) |
| `ai.properties` | Gemini API 키 (직접 생성 필요) | O |
| `db.properties` | 데이터소스 접속 정보 | X (커밋됨) |

### Gemini API 키 설정

`ai.properties.sample`을 복사해 `ai.properties`로 만든 뒤 실제 키 입력:

```bash
cp src/main/resources/ai.properties.sample src/main/resources/ai.properties
```

```properties
spring.ai.google.genai.api-key=YOUR_GEMINI_API_KEY
```

---

## 프로젝트 구조

```
src/main/java/com/example/chatbot/
│
├── common/
│   └── MasterMap.java                  LinkedHashMap<String,Object> 확장 (엔티티 없는 테이블용)
│
├── config/
│   ├── datasource/
│   │   └── PrimaryDataSourceConfig.java  DataSource / SqlSessionFactory / @MapperScan 설정
│   ├── AiConfig.java                   ChatClient Bean (Gemini 연결)
│   ├── AppConfig.java                  RestTemplate, ObjectMapper Bean
│   └── WebSocketConfig.java            /ws/{sessionId} 엔드포인트 등록
│
├── chatting/
│   ├── controller/ChatController.java
│   ├── handler/ChatWebSocketHandler.java   WebSocket 수신, userId 추출
│   └── service/
│       ├── ChatService.java            세션·이력 관리, ChatClient 호출
│       └── ResponseFilterService.java  AI 응답 후처리 (이미지 태그 검증)
│
└── tools/                              ← Primary DataSource 매퍼 스캔 대상
    ├── order/
    │   ├── entity/Order.java           @Data (Lombok)
    │   ├── mapper/OrderMapper.java     @Mapper 인터페이스
    │   ├── api/OrderApiClient.java     RestTemplate: 외부 주문 API 호출
    │   └── service/
    │       ├── OrderRepositoryTool.java  @Tool: DB 주문 조회
    │       └── OrderApiTool.java         @Tool: API 주문 조회
    ├── user/
    │   ├── entity/User.java            @Data (Lombok)
    │   ├── mapper/UserMapper.java      @Mapper 인터페이스
    │   └── service/
    │       └── UserRepositoryTool.java   @Tool: 유저 조회
    └── restaurant/
        ├── mapper/RestaurantMapper.java  @Mapper 인터페이스 (MasterMap 반환)
        └── service/
            └── RestaurantRepositoryTool.java  @Tool: 식당 조회

src/main/resources/
├── mapper/
│   └── primary/                        Primary DB XML 쿼리 파일
│       ├── OrderMapper.xml
│       ├── UserMapper.xml
│       └── RestaurantMapper.xml
└── logback-spring.xml                  로그 설정
```

---

## 데이터소스 구조

### 현재: Primary (Oracle coupleapp)

`PrimaryDataSourceConfig`가 `com.example.chatbot.tools` 하위 `@Mapper` 인터페이스를 전부 스캔한다.  
XML은 `classpath:mapper/primary/*.xml`에서 로드한다.

```
primary.datasource.*  →  PrimaryDataSourceConfig  →  @MapperScan("tools")
                                                       classpath:mapper/primary/*.xml
```

### 새 데이터소스 추가 방법

**1. `db.properties`에 접속 정보 추가**

```properties
secondary.datasource.url=jdbc:mysql://host:3306/db
secondary.datasource.username=user
secondary.datasource.password=pass
secondary.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
```

**2. Config 클래스 생성** (`PrimaryDataSourceConfig.java`를 복사해 수정)

```java
@Configuration
@MapperScan(
    basePackages = "com.example.chatbot.external",   // 새 패키지 지정
    sqlSessionFactoryRef = "secondarySqlSessionFactory"
)
public class SecondaryDataSourceConfig {
    // prefix = "secondary.datasource" 로 DataSource, SqlSessionFactory 빈 등록
    // mapperLocations = "classpath:mapper/secondary/*.xml"
}
```

**3. 매퍼 및 XML 추가**

| 위치 | 내용 |
|---|---|
| `com.example.chatbot.external.xxx.mapper` | `@Mapper` 인터페이스 |
| `resources/mapper/secondary/` | XML 쿼리 파일 |

> `tools` 하위에 새 매퍼를 추가하면 Primary에 자동으로 포함된다. 별도 설정 불필요.

---

## MasterMap 사용 규칙

엔티티 클래스를 만들기 번거로운 테이블(조회 전용, 컬럼이 자주 바뀌는 경우)에 사용한다.

```java
// 매퍼 인터페이스
MasterMap findById(@Param("id") int id);
List<MasterMap> findByName(@Param("name") String name);
```

```xml
<!-- XML -->
<select id="findById" resultType="MasterMap">
    SELECT * FROM MY_TABLE WHERE id = #{id}
</select>
```

> Oracle은 컬럼명을 대문자로 반환한다. `map.get("COLUMN_NAME")` 형태로 접근.  
> AI Tool 결과로 전달될 때는 Jackson이 JSON으로 직렬화하므로 별도 처리 불필요.

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
                   Spring AI ChatClient ◄──► Gemini
                        │
                        │ @Tool 자동 실행
                        ▼
          ┌─────────────┼──────────────────┐
          ▼             ▼                  ▼
    OrderMapper   OrderApiClient    UserMapper / RestaurantMapper
    (MyBatis)     (RestTemplate)    (MyBatis)
```

---

## 카테고리 → Tool 매핑

| 버튼 선택 | 제공되는 Tool |
|---|---|
| 주문조회 | `getOrdersByCustomerFromDb` `getOrderDetailFromDb` `getOrdersByCustomerFromApi` `getOrderDetailFromApi` |
| 유저조회 | `getUser` `searchUsers` |
| 식당조회 | `getRestaurantById` `searchRestaurantsByName` `getRestaurantsByCategory` |
| 선택 없음 | 없음 (일반 대화) |

---

## 새 Tool 추가 방법

1. `tools/{도메인}/mapper/{도메인}Mapper.java` — `@Mapper` 인터페이스 생성
2. `resources/mapper/primary/{도메인}Mapper.xml` — SQL 작성
3. `tools/{도메인}/service/{도메인}Tool.java` — `@Tool` / `@ToolParam` 추가
4. `ChatService.selectTools()` — switch에 새 Tool 빈 추가
5. `_body.html` — 카테고리 버튼 추가

> 엔티티가 필요 없으면 `MasterMap`을 resultType으로 사용.

---

## 로그

| 파일 | 내용 |
|---|---|
| `logs/info.log` | INFO 레벨 애플리케이션 로그 (일 단위 롤링, 30일 보관) |
| `logs/error.log` | ERROR 이상 |
| `logs/db.log` | MyBatis SQL 실행 로그 (DEBUG) |

설정 파일: `src/main/resources/logback-spring.xml`

---

## 주요 설정값

| 키 | 기본값 | 설명 |
|---|---|---|
| `server.port` | 8080 | 서버 포트 |
| `chat.max-connections` | 100 | 최대 동시 WebSocket 세션 수 |
| `chat.max-history-turns` | 10 | 유지할 대화 이력 턴 수 |
| `spring.ai.google.genai.chat.model` | gemini-2.5-flash | Gemini 모델 |
| `java.server.url` | http://localhost:8080 | 외부 주문 API 베이스 URL |

<img width="1904" height="896" alt="image" src="https://github.com/user-attachments/assets/4c1bf1ec-91d5-4af0-9543-68b514b71696" />

