# AI TOOL GUIDANCE

This file provides guidance when working with code in this repository.

## Technology Stack

This is a Vaadin application built with:

- Java
- Spring Boot
- jOOQ with a PostgreSQL database
- Flyway for database schema management
- Maven build system

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
