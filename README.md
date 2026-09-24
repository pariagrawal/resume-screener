# Resume Screener API

AI-powered resume screening API that analyzes resume-JD fit using Google Gemini LLM.

## Tech Stack
- Java 17, Spring Boot 4.1.1
- Google Gemini LLM API
- PostgreSQL, Spring Data JPA
- Spring WebFlux (async Gemini API calls)
- Apache PDFBox (PDF extraction)
- OpenCSV (CSV parsing)
- Jackson 3.x (JSON processing)
- Lombok

## Features
- **Keyword Screening** — Fast skill-matching engine that extracts and compares keywords
- **AI Screening** — Gemini LLM-powered contextual analysis with scoring, skill matching, experience assessment, and recommendations
- **File Upload** — Upload resume and JD as PDF, DOCX, or TXT via REST endpoints
- **CLI Mode** — Run screening directly from terminal with a single command — no curl or Postman needed
- **Downloadable Results** — Returns screening results as a downloadable `.txt` file

## Setup
1. Clone: `git clone https://github.com/pariagrawal/resume-screener.git`
2. Copy `application.properties.example` to `application.properties`
3. Add your Gemini API key and PostgreSQL credentials
4. Build: `./mvnw clean package -DskipTests`
5. Run: `java -jar target/resume-screener-0.0.1-SNAPSHOT.jar`
6. Health check: `http://localhost:8080/api/v1/screen/health`

## Usage

### CLI Mode (Recommended for quick use)

Run screening directly from terminal — no server needed, no curl commands:

```bash
java -jar target/resume-screener-0.0.1-SNAPSHOT.jar <mode> <resume_path> <jd_path> <output_path>
```

| Argument      | Description                                         |
|---------------|-----------------------------------------------------|
| `mode`        | `keyword` (fast matching) or `ai` (Gemini-powered)  |
| `resume_path` | Path to resume file (PDF, DOCX, or TXT)             |
| `jd_path`     | Path to job description file                        |
| `output_path` | Path where the result file will be saved            |

**Examples:**
```bash
# Keyword screening
java -jar target/resume-screener-0.0.1-SNAPSHOT.jar keyword ~/resume.pdf ~/jd.txt ~/result.txt

# AI screening
java -jar target/resume-screener-0.0.1-SNAPSHOT.jar ai ~/resume.pdf ~/jd.txt ~/result.txt
```

When run with no arguments, the app starts as a normal web server.

### REST API

Start the server (no arguments):
```bash
java -jar target/resume-screener-0.0.1-SNAPSHOT.jar
```

#### Endpoints

| Method | Endpoint                          | Description                     |
|--------|-----------------------------------|---------------------------------|
| POST   | `/api/v1/screen/upload/keyword`   | Keyword screening (file upload) |
| POST   | `/api/v1/screen/upload/ai`        | AI screening (file upload)      |
| POST   | `/api/v1/screen`                  | Keyword screening (JSON input)  |
| POST   | `/api/v1/screen/ai`               | AI screening (JSON input)       |
| GET    | `/api/v1/screen/health`           | Health check                    |

#### File Upload
```bash
curl -X POST http://localhost:8080/api/v1/screen/upload/keyword \
  -F "resume=@resume.pdf" \
  -F "jd=@jd.txt" \
  -o result.txt
```

#### JSON Input
```bash
curl -X POST http://localhost:8080/api/v1/screen \
  -H "Content-Type: application/json" \
  -d '{
    "resumeText": "Java developer with 3 years of Spring Boot experience...",
    "jobDescription": "Looking for a Java Backend Developer with Spring Boot, Kafka..."
  }'
```

## Sample Output

### Keyword Screening
```
========================================
   RESUME SCREENING RESULT (Keyword)
========================================

Match Score: 72%

Matched Keywords:
- Java
- Spring Boot
- REST APIs
- PostgreSQL
- Kafka
- Git
- Jenkins

Missing Keywords:
- AWS
- Docker
- Kubernetes

Experience Match: Partial Match
- Required: 3+ years
- Found: 2.5 years

Recommendation: MODERATE MATCH
The candidate meets most technical requirements
but is missing some cloud/DevOps skills.
========================================
```

### AI Screening
```
========================================
   RESUME SCREENING RESULT (AI)
========================================

Overall Score: 78/100

Matched Skills:
Java, Spring Boot, Kafka, PostgreSQL, REST APIs,
Microservices, Git, Jenkins, Jira

Missing Skills:
AWS (EC2, S3, Lambda), Docker, Kubernetes, Terraform

Experience Match:
Candidate has 2.5 years of relevant backend
development experience. Strong domain experience
in financial services is a plus.

Feedback:
Strong backend fundamentals with hands-on Kafka
and Spring Boot experience. Cloud and DevOps gaps
can be addressed with focused upskilling.

Recommendation:
RECOMMEND FOR INTERVIEW — Technical foundation is
solid. Focus interview on system design and cloud
architecture knowledge.

Interview Tips:
- Ask about Kafka consumer group rebalancing
- Probe understanding of Spring Boot auto-configuration
- Discuss experience with distributed caching
- Test knowledge of CI/CD pipeline design
========================================
```

## Project Structure
```
resume-screener/
├── src/main/java/com/paridhi/resume_screener/
│   ├── ResumeScreenerApplication.java
│   ├── cli/
│   │   └── CliRunner.java
│   ├── controller/
│   │   ├── FileScreeningController.java
│   │   └── ScreeningController.java
│   ├── dto/
│   │   ├── AIScreeningResponse.java
│   │   ├── ScreeningRequest.java
│   │   └── ScreeningResponse.java
│   ├── service/
│   │   ├── FileExtractionService.java
│   │   ├── GeminiService.java
│   │   ├── ResultFileService.java
│   │   └── ScreeningService.java
│   └── model/
│       └── ScreeningResult.java
├── src/main/resources/
│   └── application.properties
├── pom.xml
└── README.md
```

## Author
**Paridhi Agrawal** — Ex-BlackRock SDE | Java · Spring Boot · Kafka · Distributed Systems
