# Testing strategy

The project is tested in layers: fast isolated tests for service logic, tests against controlled dependencies for adapters and APIs, provider and consumer contract tests between services, and a small end-to-end suite against the running stack. Everything except the end-to-end suite runs locally without Docker.

## Test layers

| Layer | Location | Gradle task |
|---|---|---|
| Unit | `src\test\java`, next to the production packages | module `test` |
| Integration | `*IT` classes under `src\test\java` | module `test` |
| Component | Cucumber suites in song-service and resource-processor | module `test` |
| Provider contract | `src\contractTest` in song-service and resource-service | module `contractTest` |
| Consumer contract | Stub Runner tests in resource-processor and resource-service | module `test` |
| End-to-end | `e2e-tests\src\test` | `:e2e-tests:e2eTest` |

## Unit tests

JUnit Jupiter, Mockito and AssertJ test single classes without starting Spring. They cover song and resource services, exception mapping, the order of storage, database and messaging calls, the RabbitMQ producer and consumer, and MP3 metadata extraction. The extractor tests run the real Tika parser on MP3 fixtures built in memory.

## Integration tests

- **Repositories** run on H2 in PostgreSQL mode and check persisted fields, generated IDs and the unique S3 key constraint. H2 is not a substitute for real PostgreSQL.
- **`S3StorageIT`** runs the real AWS S3 client against WireMock for upload, download, delete and missing objects.
- **`ResourceControllerIT`** runs the resource-service context with MockMvc and H2. S3 and RabbitTemplate are mocked and song-service is a WireMock stub. It covers upload, download, delete, unsupported content types and storage failures.
- **`HttpClientsIT`** checks the resource-processor REST clients against WireMock, including retries on server errors and no retry on 404.

## Component tests

Component tests drive one service through its entry point with its neighbours replaced. They are written as Cucumber scenarios.

Cucumber describes behaviour in plain-language Given/When/Then steps stored in `.feature` files under `src\test\resources\features`. Java step definitions turn each step into real calls against the service, and a JUnit Platform `CucumberSuite` class runs the features as part of the module's `test` task. This keeps the scenarios readable by non-developers while still running as automated tests. The same approach is used for the end-to-end tests.

## Contract tests

Spring Cloud Contract generates provider tests from the YAML files in `src\contractTest\resources\contracts`.

Consumer-side contract tests run the real clients against WireMock stubs loaded by Spring Cloud Contract Stub Runner. 

Provider stubs are published to `build\stubs-repo` after provider contract verification. The consumer test tasks depend on the appropriate providers' `publishAllPublicationsToStubsRepository` tasks and configure Stub Runner to use that local repository.

## End-to-end tests

The `e2e-tests` module has five Cucumber scenarios over the public HTTP APIs: upload and wait for metadata, download unchanged, delete removes file and metadata, non-MP3 refused, and unknown ID returns 404. They expect the Docker Compose stack (services, Eureka, PostgreSQL, RabbitMQ, LocalStack) and allow up to 30 seconds for asynchronous processing.

The E2E scenarios are excluded from the default `test` lifecycle. The `:e2e-tests:test` task is disabled; run `:e2e-tests:e2eTest` explicitly with the Compose stack running. The step definitions still skip scenarios if either service is unreachable, so check the test report for skipped scenarios before treating a green E2E run as proof that scenarios executed.

## Running the tests

Run from the repository root in PowerShell. These Gradle tasks run unit, integration, component, provider contract and consumer contract tests; the disabled E2E `test` task is not executed.

```powershell
# Run all non-E2E test and provider contract tasks from a clean build
.\gradlew.bat clean test contractTest --console=plain
```

End-to-end tests need Docker and the Compose environment variables configured for the services. Start the stack and obtain a song-service replica's published host port:

```powershell
docker compose up -d --build
docker compose port --index 1 song-service 8080

$env:E2E_RESOURCE_SERVICE_URL = "http://localhost:8080"
$env:E2E_SONG_SERVICE_URL = "http://localhost:<reported-port>"
.\gradlew.bat :e2e-tests:e2eTest --rerun-tasks --console=plain
```