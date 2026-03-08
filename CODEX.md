Always respond in Chinese-simplified

# Project Overview
This project is a microservice-based backend system.
Tech stack:
- Java / Spring Boot
- JDK 17+
---
# Architecture
Main modules:
兼容 Java8 httpclient
兼容 Java17 httpclient
RESTFul
Shared modules:
JSON
entity
util
---
# Coding Conventions
Language: Java
Follow:
- Clean architecture
- http client
- json support
Avoid:
- Static business logic
- Direct DB access in controllers
---
# Testing
Testing framework:
JUnit + Mockito
Test layers:
unit tests
integration tests
All new logic must include tests.
---
# Dependency Rules
Allowed:
Controller → Service → Repository
Not allowed:
Controller → Repository
---
# Database Rules
Do NOT:
- modify migrations
- rename existing columns
- remove fields
unless explicitly instructed.
---
# Performance Considerations
Critical paths:
order processing
inventory updates
payment callbacks
Avoid introducing blocking operations.
---
# AI Execution Guidelines
Before modifying code:
1. analyze related modules
2. produce a plan
3. wait for confirmation
After changes:
run tests
summarize risks
