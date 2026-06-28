# AI Artist Bio Generation — Root Cause Fix (Configuration Loading)

## Objective
Fix AI Artist Bio generation (Gemini API 403 PERMISSION_DENIED) by making the
backend load `.env` automatically at startup, without requiring manual `source .env`.

## Root Cause
Spring Boot resolves `@Value("${ticketbox.ai.gemini.key:}")` from OS environment
variables only. The monorepo's `.env` file (at the repo root) is **not** a shell
script and is **not** sourced automatically when starting the JVM. Without an
explicit `source .env` (or `export`) the variable `GEMINI_API_KEY` is absent from
the process environment, so `apiKey` resolves to `""`, and Gemini returns
`403 PERMISSION_DENIED` because the `?key=` query parameter is empty.

The previous attempt using `EnvironmentPostProcessor` failed because:
1. Spring Boot 3 removed `spring.factories` support.
2. Even when registered correctly via `META-INF/...imports`, injecting properties at the end of the `Environment` caused resolution issues with placeholder defaults in `application.yml`.
3. `spring-dotenv` requires `.env` to be in the JVM's working directory, which wasn't true.

## Decisions Made
- **Early `main()` Injection**: Instead of relying on complex Spring Boot lifecycle hooks, we manually parse the `.env` file at the very start of the `public static void main(String[] args)` method (before `SpringApplication.run()`).
- **`System.setProperty` Injection**: We push the `.env` variables into `System.setProperty()`. Since Java System properties are natively supported by Spring Boot and have the highest precedence (right below OS env variables), Spring effortlessly picks them up for all `@Value` and `application.yml` resolution.
- **Walk-up discovery**: The loader walks up from the JVM working directory until
  it finds `.env`. This makes it location-agnostic — works when the JVM is launched
  from `apps/backend/` or from the monorepo root.
- **OS wins**: If a variable is already set in the OS environment (e.g., in CI/CD),
  the OS value is kept. `.env` is only injected for keys that are absent.
- **`dotenv-java` library**: Used to parse the `.env` file. Handles comments, quoted
  values, and multiline values correctly.

## Files Created
- None (Deleted the previous failed `DotEnvEnvironmentPostProcessor` files).

## Files Modified
- `apps/backend/src/main/java/com/ticketbox/backend/Application.java` — added `.env` loading logic in `main()`.
- `apps/backend/pom.xml` — added `io.github.cdimascio:dotenv-java:3.0.0`.
- `apps/backend/src/main/java/com/ticketbox/backend/worker/ai/GeminiAiClient.java` — reverted to original (removed `@PostConstruct` and blank-key guard).

## Dependencies
- `io.github.cdimascio:dotenv-java:3.0.0` (zero transitive dependencies)

## Remaining Work
- None. The fix is verified to work perfectly without manual export.

## Open Questions
- None.
