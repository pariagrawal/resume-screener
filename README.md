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
- **Batch Screening** — Screen an entire directory of resumes against one JD, with per-candidate results and a ranked summary report
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
java -jar target/resume-screener-0.0.1-SNAPSHOT.jar <mode> -resumePath <path> -JDPath <path> -outputPath <path>
```

| Flag            | Description                                                        |
|-----------------|--------------------------------------------------------------------|
| `mode`          | `keyword` (fast matching) or `ai` (Gemini-powered)                 |
| `-resumePath`   | Path to a single resume file **or a directory** of resumes         |
| `-JDPath`       | Path to the job description file                                   |
| `-outputPath`   | Single mode: path for result file / Batch mode: directory for results |

#### Single Resume
```bash
java -jar target/resume-screener-0.0.1-SNAPSHOT.jar keyword \
  -resumePath ~/resume.pdf \
  -JDPath ~/jd.txt \
  -outputPath ~/result.txt
```

#### Batch Screening (Directory of Resumes)

Point `-resumePath` to a folder containing multiple resumes. The screener processes every supported file in the directory, generates an individual result for each candidate, and produces a **SUMMARY_RANKING.txt** that ranks all candidates by score (highest first).

Works the same way even if the folder contains only one resume.

```bash
java -jar target/resume-screener-0.0.1-SNAPSHOT.jar keyword \
  -resumePath ~/resumes/ \
  -JDPath ~/jd.txt \
  -outputPath ~/results/
```

**What gets created:**
```
~/results/
├── result_resume_priya_sharma.txt      # Individual detailed result
├── result_resume_rahul_verma.txt
├── result_resume_amit_patel.txt
├── result_resume_sneha_reddy.txt
├── result_resume_vikram_singh.txt
└── SUMMARY_RANKING.txt                 # Ranked summary of all candidates
```

**Supported formats:** PDF, DOCX, TXT, CSV

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

### Batch Screening — Summary Ranking
```
========================================================
         RESUME SCREENING - CANDIDATE RANKING
========================================================
  Mode           : KEYWORD
  Total Resumes  : 5
  Processed      : 5
========================================================

RANK   CANDIDATE                      SCORE    RECOMMENDATION
─────────────────────────────────────────────────────────────────
1      Sneha Reddy                    88%      STRONG MATCH
2      Priya Sharma                   75%      STRONG MATCH
3      Amit Patel                     52%      MODERATE MATCH
4      Vikram Singh                   28%      WEAK MATCH
5      Rahul Verma                    12%      NO MATCH
─────────────────────────────────────────────────────────────────

Individual detailed results are in the output directory.
========================================================
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
