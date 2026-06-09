# Configuration Refactoring: OS Environment Variables

## 1. Previous Approach
The project previously relied on the `me.paulschwarz:spring-dotenv` plugin in the backend to automatically parse the root `.env` file (`spring.dotenv.directory=../../`) during Spring Boot startup. While convenient, this approach created a hard dependency on the plugin for configuration loading and could lead to inconsistencies across different execution environments (Docker, Linux servers, Windows) if they did not support or expect this automatic loading behavior.

## 2. New Approach
The project now strictly uses standard Operating System Environment Variables.
There is only one single `.env` file at the root of the project. No duplicated or module-specific `.env` files are allowed.
Instead of Spring Boot parsing the file itself, the shell or operating system is responsible for loading the variables from `.env` into the process environment before executing `mvn spring-boot:run`. Spring Boot then resolves these variables natively via standard `${VARIABLE_NAME}` placeholders.

## 3. Removed Dependencies
- `me.paulschwarz:spring-dotenv` (version 4.0.0) was removed from `apps/backend/pom.xml`.

## 4. Files Modified
- `apps/backend/pom.xml`: Removed `spring-dotenv` dependency.
- `apps/backend/src/main/resources/application.yml`: Removed the `spring.dotenv.directory` configuration block.
- `apps/backend/.env`: Deleted (to ensure only the root `.env` is used).
- `.env.example`: Added a comment explaining the new OS-level environment variable requirement.
- `.env`: Enclosed `MAIL_PASS` in double quotes to prevent bash parsing errors when sourcing the file.
- `README.md`: Added detailed "Startup Instructions" for both Linux/macOS and Windows PowerShell.

## 5. Validation Results
- The application compilation and test suite run successfully using standard environment variables injected by the shell.
- Gemini API key, Resend API key, Email From address, RabbitMQ, PostgreSQL, and Redis configurations are successfully resolved by Spring Boot natively.
- No redundant `.env` files exist.

## 6. Linux / macOS Instructions
To run the application, load the environment variables using `set -a` and `source` before starting Spring Boot:
```bash
set -a
source .env
set +a
cd apps/backend
mvn spring-boot:run
```

## 7. Windows PowerShell Instructions
To run the application on Windows, parse the `.env` file and set the variables for the current process:
```powershell
Get-Content .env | ForEach-Object {
    if ($_ -match '^\s*([^#][^=]*)=(.*)$') {
        [Environment]::SetEnvironmentVariable($matches[1], $matches[2], "Process")
    }
}
cd apps/backend
mvn spring-boot:run
```
