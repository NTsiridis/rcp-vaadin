# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Goal

Build a Vaadin-based web application that mimics Eclipse RCP (Rich Client Platform) architecture — including concepts like Perspectives, Views, Editors, and a pluggable workbench.

## Technology Stack

- **Java 21** (configured in `.idea/misc.xml`)
- **Vaadin** — server-side Java web UI framework
- **Eclipse RCP** architectural patterns (not the actual RCP runtime — replicated in web form)

## Build & Run Commands

```bash
# Run in development mode (hot reload via Vite dev server)
mvn spring-boot:run

# Run tests
mvn test

# Run a single test class
mvn test -Dtest=MyTestClass

# Build production JAR (bundles frontend assets)
mvn package -Pproduction

# Just compile (skips frontend bundling)
mvn compile
```

The app starts at `http://localhost:8080`.

## Project Layout

```
src/main/java/com/rcpvaadin/   # Application source
src/main/resources/            # application.properties, static assets
src/test/java/com/rcpvaadin/   # Tests
```

Base package: `com.rcpvaadin`
Entry point: `Application.java` (`@SpringBootApplication`)

## Architecture Intent

Mimic Eclipse RCP core abstractions as Vaadin components:

- **Workbench** — top-level application shell
- **Perspectives** — named layouts of views/editors
- **Views** — reusable side panels (e.g., navigator, outline)
- **Editors** — primary content areas, potentially tab-based
- **Plugin/Extension point system** — modular registration of views, editors, and perspectives

Each RCP concept should map to a well-defined Vaadin component or layout abstraction, keeping UI logic decoupled from business logic.
