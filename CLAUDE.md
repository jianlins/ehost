# AGENTS.md - Developer Guide for eHOST

## Project Overview
- **Project Name**: eHOST (Extensible Human Oracle Suite of Tools)
- **Type**: Java 8 Swing desktop application with Spring Boot REST server
- **Build Tool**: Maven
- **Testing Framework**: JUnit 5 (Jupiter)
- **Source**: `src/main/java`
- **Tests**: `src/test/java`

---

## Build & Test Commands

### Build Commands
```bash
# Clean and build the project
mvn clean package

# Build without running tests
mvn clean package -DskipTests

# Install custom JAR dependencies first (required before build)
# Linux/Mac: bash script/mvn_install_jar.sh
# Windows: script\mvn_install_jar.bat
```

> **Requires JDK 8.** The project targets 1.8 and CI builds on Corretto 8. `src/main` does **not**
> compile on JDK 11+: `umls/GetCUI.java` uses `javax.xml.ws.Service`, which was removed from the JDK
> in 11, so the build fails with `cannot access javax.xml.ws.Service`.
>
> Maven picks its JDK from `JAVA_HOME`, **not** from whichever `java` is on `PATH`. A `JAVA_HOME`
> left pointing at a newer JDK will fail the build even when `java -version` reports 1.8. Check with
> `mvn -version` (not `java -version`) and either unset `JAVA_HOME` or point it at a JDK 8.

### Test Commands
```bash
# Run all tests
mvn test

# Run a single test class
mvn test -Dtest=IAATest

# Run a single test method
mvn test -Dtest=IAATest#testMethodName

# Run tests with verbose output
mvn test -X
```

### Running the Application
```bash
# After building, run the JAR
java -jar target/deploy/eHOST.jar

# With workspace parameter
java -jar target/deploy/eHOST.jar -w /path/to/workspace

# With config home parameter
java -jar target/deploy/eHOST.jar -c=/path/to/config
```

---

## Code Style Guidelines

### General Conventions
- **Java Version**: Java 8 (source/target 1.8)
- **Encoding**: UTF-8
- **Package Naming**: Lowercase, dot-separated (e.g., `edu.utah.bmi.nlp`)
- **Class Naming**: PascalCase (e.g., `ProjectLock`, `PropertiesUtil`)
- **Method/Variable Naming**: camelCase (e.g., `getTimeStamper()`, `initialized`)

### Import Conventions
- Use explicit imports (no wildcard `*` imports unless necessary)
- Group imports: standard library → third-party → project-specific
- Sort imports alphabetically within groups

### Formatting
- **Indentation**: 4 spaces (no tabs)
- **Line Length**: No strict limit, but keep lines reasonable
- **Braces**: Opening brace on same line, closing on new line
- **Blank Lines**: Use between logical code sections
- **Comments**: Use Javadoc for public APIs; inline comments sparingly

### Types
- Use explicit types when unclear; prefer readability over brevity
- Use interfaces when possible (e.g., `List<String>` over `ArrayList<String>`)
- Use `final` for constants and variables that won't change

### Naming Conventions
- **Classes**: Nouns, PascalCase (e.g., `FileConverterGUI`)
- **Methods**: Verbs, camelCase (e.g., `loadSystemConfigure`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `DEFAULT_PORT`)
- **Boolean**: Prefix with `is`, `has`, `can` (e.g., `isUnixOS`)

### Error Handling
- Use meaningful exception messages
- Log errors with appropriate level (logger.error, logger.warn)
- Catch specific exceptions rather than generic `Exception`
- Return early on error conditions when appropriate

### Logging
- Use SLF4J for logging
- Follow pattern: `Logger logger = LoggerFactory.getLogger(ClassName.class);`
- Use appropriate log levels: ERROR > WARN > INFO > DEBUG

---

## Project Structure

```
src/
├── main/
│   ├── java/
│   │   ├── main/           # Application entry point
│   │   ├── commons/        # Utility classes
│   │   ├── config/         # Configuration handling
│   │   ├── rest/server/    # REST API server
│   │   ├── adjudicator/   # Adjudication module
│   │   ├── converter/     # File conversion
│   │   └── ...
│   └── resources/
│       └── version.properties
└── test/
    └── java/               # Test classes
```

---

## Key Dependencies
- Spring Boot 2.7.18
- JUnit 5.10.1
- Apache HttpComponents
- Apache POI (Excel processing)
- SLF4J (Logging)

---

## Development Notes

### GUI Testing
The application uses Java Swing/AWT. GUI tests require Xvfb (virtual framebuffer) on headless systems:
```bash
# Linux example
Xvfb :99 -screen 0 1024x768x24 &
export DISPLAY=:99
mvn test
```

### Configuration
- System config: `eHOST.sys` file
- Application config: `application.properties`
- Can specify config home: `-c=/path/to/config`
- Can specify workspace: `-w=/path/to/workspace`

### Common Issues
- Custom JAR dependencies in `/lib` must be installed first via Maven
- GUI requires display; use Xvfb for headless CI/CD environments
- Port 8001 used by default for REST server (auto-increments if in use)
- `cannot access javax.xml.ws.Service` means Maven is on JDK 11+; see the JDK 8 note under
  [Build Commands](#build-commands)

---

## CI/CD
GitHub Actions workflow at `.github/workflows/simple_release.yml`:
- Builds on Ubuntu with JDK 8
- Runs `mvn clean package`
- Creates ZIP release artifact
