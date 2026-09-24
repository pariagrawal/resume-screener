# Resume Screener API

AI-powered resume screening API that analyzes resume-JD fit using Google Gemini LLM.

## Tech Stack
- Java 17, Spring Boot 4.1.1
- Google Gemini LLM API
- PostgreSQL
- Spring Data JPA
- WebFlux (for async API calls)

## Features
- **Keyword Screening** (`POST /api/v1/screen`) — fast skill-matching engine
- **AI Screening** (`POST /api/v1/screen/ai`) — Gemini LLM-powered contextual analysis with scoring, skill matching, experience assessment, and recommendations

## Setup
1. Clone: `git clone https://github.com/pariagrawal/resume-screener.git`
2. Copy `application.properties.example` to `application.properties`
3. Add your Gemini API key and PostgreSQL credentials
4. Run: `mvn spring-boot:run`
5. Health check: `http://localhost:8080/api/v1/screen/health`

## Author
**Paridhi Agrawal** — Ex-BlackRock SDE | Java · Spring Boot · Kafka · Distributed Systems
