# Agent-Secretary (Java Version)

사용자 맥락(일정, 건강, 날씨) 기반의 선제적 Slack 비서 서비스입니다. v1.1부터 Java 21 및 Spring Boot 3.x 기반의 헥사고날 아키텍처로 전환되었습니다.

## Tech Stack
- **Language**: Java 21
- **Framework**: Spring Boot 3.2.4
- **Architecture**: Hexagonal Architecture (Ports and Adapters)
- **Database**: SQLite (Local-first Data Policy)
- **Communication**: Slack Bolt SDK for Java
- **Async**: CompletableFuture (Virtual Threads ready)

## Project Structure
```text
src/main/java/com/agent/secretary/
├── AgentSecretaryApplication.java (Main)
├── application/
│   ├── port/ (Inbound/Outbound Interfaces)
│   └── service/ (Application Services)
├── domain/
│   ├── model/ (Domain Entities - Records)
│   └── service/ (Pure Business Logic)
├── infrastructure/
│   ├── adapter/ (Outbound Adapters: persistence, external APIs)
│   └── config/ (Framework Configurations)
└── interfaces/
    └── slack/ (Inbound Adapter: Slack Controller & Block Kit)
```

## Key Features
- **Focus Score Calculation**: 수면 점수, 일정 밀집도, 회복 수준을 기반으로 한 실시간 집중도 분석.
- **Morning Briefing**: 매일 아침 날씨 및 첫 일정 브리핑.
- **Golden Time Alert**: 높은 집중도 포착 시 Deep Work 제안.
- **Emergency Boost**: 낮은 집중도와 임박한 일정 감지 시 회복 제안.

## How to Run
```bash
./gradlew bootRun
```

## Environment Variables
- `SLACK_BOT_TOKEN`: Slack 봇 토큰
- `SLACK_SIGNING_SECRET`: Slack 사이닝 시크릿
- `OPENWEATHER_API_KEY`: OpenWeatherMap API 키
