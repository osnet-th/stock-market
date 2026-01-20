# Repository Guidelines

## Project Structure & Module Organization
This is a Spring Boot 4.0.1 service built with Gradle and Java 21. Source code lives under `src/main/java` in the base package `com.thlee.stock.market.stockmarket`. Application configuration is in `src/main/resources` (see `application.properties`). Tests live under `src/test/java` mirroring the main package path, for example `src/test/java/com/thlee/stock/market/stockmarket/StockMarketApplicationTests.java`.

## Build, Test, and Development Commands
Use the Gradle wrapper to ensure consistent builds:
- `./gradlew build` builds the project and runs tests.
- `./gradlew bootRun` runs the Spring Boot app locally.
- `./gradlew test` runs all tests.
- `./gradlew test --tests "com.thlee.stock.market.stockmarket.StockMarketApplicationTests"` runs a single test class.
- `./gradlew clean` removes build outputs.

## Coding Style & Naming Conventions
Use standard Java conventions: 4-space indentation, PascalCase for classes (e.g., `StockMarketApplication`), camelCase for methods/fields, and lower-case dot-separated package names matching the folder structure. Keep code in the `com.thlee.stock.market.stockmarket` package unless adding a new module. Lombok is enabled; prefer Lombok annotations for boilerplate where appropriate.

## Testing Guidelines
Tests use the JUnit Platform via `spring-boot-starter-test`. Place tests under `src/test/java` and keep names ending in `Tests` (e.g., `StockMarketApplicationTests`). Run the full suite with `./gradlew test` before submitting changes.

## Commit & Pull Request Guidelines
No commit history is available in this workspace, so there is no established message convention to follow. Use clear, imperative commit messages (e.g., "Add price service") and keep commits focused. For pull requests, include a concise description of changes, testing performed, and any relevant screenshots or logs if behavior changes.

## Configuration Tips
Local configuration should go in `src/main/resources/application.properties`. Avoid committing secrets; use environment variables or local override files when needed.

## CIRITICAL 
Any Git-related operation must be preceded by an explanation of the intended action and carried out only upon approval.