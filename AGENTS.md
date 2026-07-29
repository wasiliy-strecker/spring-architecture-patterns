# Repository Guidelines

## Purpose

This repository is a production-minded Spring Boot reference application for a
return and refund workflow. Keep examples connected to the business use case;
do not add isolated pattern demonstrations that bypass the module boundaries.

## Architecture

The application is a Spring Modulith with the top-level modules `intake`,
`inspection`, `resolution`, `refund`, and `query`. Within each module, dependencies
point inward from adapters to application services and then to the domain.

Domain and application code must not depend on Spring, JPA, servlet APIs, or
another module's internal packages. Cross-module collaboration uses an exposed
API or a named event interface and is verified by Spring Modulith and ArchUnit.

## Build and verification

- `./mvnw clean verify`: format-check, compile, test, verify architecture, and
  package the application.
- `./mvnw test -Dtest=ApplicationModulesTest,ArchitectureRulesTest`: run the
  fast architecture smoke suite.
- Use Java 21 language features without preview APIs. CI also verifies Java 25.

Use Google Java Format through Spotless. Add focused domain tests for business
rules and PostgreSQL Testcontainers tests for persistence behavior. Do not
replace integration tests with H2.

## Security and configuration

Never commit credentials, customer data, or local environment files. Keep
secrets in environment variables and commit only sanitized examples.
