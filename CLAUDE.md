# AI TOOL GUIDANCE

This file provides guidance when working with code in this repository.

## Project Overview

**FireStock** is a regional fire apparatus inventory system for tracking equipment across fire stations. It allows firefighters to perform shift inventory checks, maintenance crews to manage equipment lifecycle, and provides visibility into stock levels across all stations.

## Specifications

Project specifications are located in the `Spec/` directory:

- `Spec/Context.md` - Project context, problem statement, stakeholders, and success criteria
- `Spec/Domain/` - Domain model entity specifications
- `Spec/UseCases/` - Use case specifications (UC-XX format)
- `Spec/NFRs/` - Non-functional requirements (NFR-XX format)

When implementing features or fixing bugs, consult the relevant specification files to understand requirements and domain concepts.

## Technology Stack

This is a Vaadin application built with:

- Java
- Spring Boot
- jOOQ with a PostgreSQL database
- Flyway for database schema management
- Maven build system

## Database Setup

The application requires PostgreSQL. Start it using Docker/Podman:

```bash
# Using Docker
docker run -d --name firestock-postgres \
  -e POSTGRES_USER=firestock \
  -e POSTGRES_PASSWORD=firestock \
  -e POSTGRES_DB=firestock \
  -p 5432:5432 \
  postgres:16-alpine

# Using Podman
podman run -d --name firestock-postgres \
  -e POSTGRES_USER=firestock \
  -e POSTGRES_PASSWORD=firestock \
  -e POSTGRES_DB=firestock \
  -p 5432:5432 \
  docker.io/postgres:16-alpine
```

Flyway migrations run automatically on application startup.

## Development Commands

### Running the Application

```bash
./mvnw                           # Start in development mode (default goal: spring-boot:run)
./mvnw spring-boot:run           # Explicit development mode
```

The application will be available at http://localhost:8080

### Building for Production

```bash
./mvnw package      # Build production JAR
docker build -t my-application:latest .  # Build Docker image
```

### Testing

```bash
./mvnw test                      # Run all tests
./mvnw test -Dtest=TaskServiceTest  # Run a single test class
./mvnw test -Dtest=TaskServiceTest#tasks_are_stored_in_the_database_with_the_current_timestamp  # Run a single test method
```

## MCP Servers

- Read `MCP-SERVERS.md` for a summary of what servers are available to you.
- Use the MCP servers to fetch up-to-date information instead of relying on your training.
