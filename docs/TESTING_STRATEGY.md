# Testing strategy

The project is tested in layers: fast isolated tests for service logic, tests against controlled dependencies for adapters and APIs, provider contracts between services, and a small end-to-end suite against the running stack. Everything except the end-to-end suite runs locally without Docker.

## Test layers

| Layer | Location | Gradle task |
|---|---|---|
| Unit | `src\test\java`, next to the production packages | module `test` |
| Integration | `*IT` classes under `src\test\java` | module `test` |
| Component | Cucumber suites in song-service and resource-processor | module `test` |
| Contract | `src\contractTest` in song-service and resource-service | module `contractTest` |
| End-to-end | `e2e-tests\src\test` | `:e2e-tests:test` |

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

| Interaction | Provider | Consumer | Cases |
|---|---|---|---|
| Create song metadata | song-service | resource-processor | 200, 409 |
| Delete song metadata | song-service | resource-service | 200 |
| Download a resource | resource-service | resource-processor | 200, 404 |
| Uploaded resource ID | resource-service | resource-processor | message body, exchange, routing key |

HTTP contracts run on standalone MockMvc with real controllers and exception handlers but mocked services, so they check the API shape, not persistence. The messaging contract uses `MessagingContractBase`, which runs the real upload flow with an in-memory RabbitTemplate instead of a broker; the Gradle base-class mapping selects it for contracts in the `messaging` directory.

Both providers publish stubs to `build\stubs-repo`, and publication depends on contract verification. No consumer test loads those stubs yet, so consumer compatibility is not verified. Generated tests and stubs under `build` are outputs: change the YAML, base classes or Gradle configuration instead.

## End-to-end tests

The `e2e-tests` module has five Cucumber scenarios over the public HTTP APIs: upload and wait for metadata, download unchanged, delete removes file and metadata, non-MP3 refused, unknown ID returns 404. They expect the Docker Compose stack (services, Eureka, PostgreSQL, RabbitMQ, LocalStack) and allow up to 30 seconds for asynchronous processing.

If resource-service or song-service cannot be reached, the scenarios are skipped and Gradle still succeeds. Check the report for skipped scenarios before treating a green run as proof.

## Running the tests

Run from the repository root in PowerShell. Docker is needed only for end-to-end tests.

```powershell
# Unit, integration and component tests
.\gradlew.bat :song-service:test :resource-service:test :resource-processor:test --console=plain

# Contract tests
.\gradlew.bat :song-service:contractTest :resource-service:contractTest --console=plain
```

End-to-end tests need the Compose stack and the database variables from `compose.yaml`:

```powershell
docker compose up -d --build
docker compose port --index 1 song-service 8080

$env:E2E_RESOURCE_SERVICE_URL = "http://localhost:8080"
$env:E2E_SONG_SERVICE_URL = "http://localhost:<reported-port>"
.\gradlew.bat :e2e-tests:test --rerun-tasks --console=plain
```

Song-service runs two replicas on dynamic host ports, so set `E2E_SONG_SERVICE_URL` explicitly; the default `http://localhost:8081` will not match. `--rerun-tasks` prevents a cached result from being reused.
