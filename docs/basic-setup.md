# Basic Setup Plan — MySQL + Manga Module (Repository Pattern)

## Goal

Connect the Spring Boot app to MySQL and expose a basic `Manga` module using the
repository pattern (Spring Data JPA abstraction).

```
HTTP -> MangaController -> MangaService -> MangaRepository (interface) -> MySQL
```

> **Implementation status: complete (2026-11-09).**
> `./gradlew test` passes 18 tests, and a live MySQL smoke test passed for
> create, list/filter, get by ID, get by slug, update, duplicate slug (409),
> validation (400), delete (204), missing ID (404), and row removal.

## 0. Implementation TODO

> Work top to bottom. A task is only done when its **Verify** step passes.
> Run all commands from the project root.

### Phase 1 — Database

- [x] Create the `hms` database and load `database/schema.sql`.
- [x] **Verify:** `mysql -u root -p hms -e "SHOW TABLES; DESCRIBE manga;"`
      lists the `manga` table with the expected columns.

### Phase 2 — Build configuration

- [x] Add the JPA, validation, and MySQL dependencies to `build.gradle`.
- [x] Add the Hibernate Tools Gradle plugin and `hibernateTools { ... }` block.
- [x] **Verify:** `./gradlew tasks --all` completes without build-script errors
      and lists the `generateJava` task.

### Phase 3 — Connection configuration

- [x] Configure `application.properties` with env-based datasource settings and
      `spring.jpa.hibernate.ddl-auto=validate`.
- [x] Add local `hibernate-generator.properties`; add it to `.gitignore`; commit a
      placeholder-only `hibernate-generator.properties.example`.
- [x] **Verify:** `git status` does not show `hibernate-generator.properties`, and
      `./gradlew bootRun` starts without a datasource connection error (then
      stop it with `Ctrl+C`).

### Phase 4 — Entity generation

- [x] Add `manga.reveng.xml` with a filter for the `manga` table.
- [x] Run `./gradlew generateJava`.
- [x] **Verify:** `src/main/java/com/example/HMS/Manga/entity/Manga.java`
      exists and contains `@Entity`, `@Table(name = "manga")`, and a field for
      every column in the table.
- [x] Keep the generated entity unchanged; UUID/timestamp/default rules live in
      `MangaService` and `MangaResponse`.
- [x] **Verify:** delete `Manga.java`, rerun `generateJava`, and `./gradlew
      compileJava` still passes.

### Phase 5 — Repository

- [x] Create `MangaRepository` extending `JpaRepository<Manga, String>` with
      `findBySlug`, `existsBySlug`, and paged query methods.
- [x] **Verify:** the Spring context loads and the repository bean is created
      (or a `@DataJpaTest` repository test passes).

### Phase 6 — Service

- [x] Implement `MangaService` CRUD, slug generation, duplicate-slug rejection,
      not-found handling, and entity ↔ DTO mapping.
- [x] **Verify:** unit tests for create, get, update, delete, duplicate slug,
      and missing ID pass.

### Phase 7 — DTOs and validation

- [x] Create `MangaRequest` and `MangaResponse`.
- [x] Add validation annotations (`@NotBlank`, size/format rules).
- [x] **Verify:** an invalid request body returns `400 Bad Request` with field
      messages in a controller test.

### Phase 8 — Controller

- [x] Create `MangaController` with the endpoints in section 8.
- [x] **Verify:** `@WebMvcTest` covers list, get by ID, get by slug, create,
      update, and delete; all pass.

### Phase 9 — Error handling

- [x] Add `MangaNotFoundException` and `GlobalExceptionHandler`.
- [x] **Verify:** unknown ID returns `404`, duplicate slug returns `409`, and
      validation failure returns `400`.

### Phase 10 — Final acceptance

- [x] Add the service, controller, and repository tests from section 10.
- [x] **Verify:** `./gradlew test` passes.
- [x] **Verify:** `./gradlew build` passes.
- [x] **Verify:** start the app and smoke-test `POST`, `GET`, `PUT`, and
      `DELETE /api/manga`; confirm the `manga` table changes as expected.

### Phase 11 — API documentation (OpenAPI + Scalar)

- [x] Add `springdoc-openapi-starter-webmvc-api` to `build.gradle`.
- [x] Add the static `src/main/resources/static/scalar.html` page.
- [x] **Verify:** `GET /v3/api-docs` returns an OpenAPI JSON document.
- [x] **Verify:** `GET /scalar.html` returns the Scalar HTML page and renders
      the endpoints in a browser.

## 1. Project structure

Add files under the existing module folder:

```
src/main/java/com/example/HMS/Manga/
├── controller/
│   └── MangaController.java
├── dto/
│   ├── MangaRequest.java
│   └── MangaResponse.java
├── entity/
│   └── Manga.java          # generated from MySQL, then reviewed
├── repo/
│   └── MangaRepository.java
├── service/
│   └── MangaService.java
└── exception/
    ├── MangaNotFoundException.java
    └── GlobalExceptionHandler.java
```

API documentation files:

```
src/main/resources/
└── static/
    └── scalar.html           # Scalar page, loads /v3/api-docs
```

The repository is an interface only — Spring Data JPA provides the
implementation, so the service depends on the abstraction, not the database.

## 2. Dependencies and generator plugin

Add to `build.gradle`:

```gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '4.0.10-SNAPSHOT'
    id 'io.spring.dependency-management' version '1.1.7'
    // Reverse engineering: generates JPA entities from the live database.
    id 'org.hibernate.tool.hibernate-tools-gradle' version '8.0.0.Alpha1'
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-webmvc'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-validation'

    // Must be on the compile classpath: generateJava resolves compileClasspath.
    implementation 'com.mysql:mysql-connector-j'

    // Generates the OpenAPI document at /v3/api-docs.
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-api:3.2.0'

    testImplementation 'org.springframework.boot:spring-boot-starter-webmvc-test'
    testImplementation 'org.springframework.boot:spring-boot-starter-data-jpa-test'
    testRuntimeOnly 'com.h2database:h2'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

hibernateTools {
    packageName = 'com.example.HMS.Manga.entity'
    outputFolder = 'src/main/java'
    hibernateProperties = 'hibernate-generator.properties'
    revengFile = 'manga.reveng.xml'
    generateAnnotations = true
    useGenerics = true
}

// Generator-only inputs: keep them out of the application artifact.
tasks.named('processResources') {
    exclude 'hibernate-generator.properties'
    exclude 'manga.reveng.xml'
}
```

> Use `7.3.13.Final` if you prefer the latest stable tool version over the
> `8.0.0.Alpha1` release. The plugin version must be compatible with the
> Gradle wrapper in `gradle/wrapper/gradle-wrapper.properties`.

## 3. MySQL configuration

Create the database and load the existing schema:

```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS hms CHARACTER SET utf8mb4;"
mysql -u root -p hms < database/schema.sql
```

`src/main/resources/application.properties`:

```properties
spring.application.name=HMS

spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/hms?useSSL=false&serverTimezone=UTC}
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD:}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# Schema is managed by database/schema.sql; do not let JPA alter it.
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.open-in-view=false
spring.jpa.show-sql=true
```

> Use environment variables for real credentials. Never commit passwords.

## 4. Entity — generate it from MySQL (no manual mapping)

Do not hand-write `Manga.java`. Hibernate Tools reads the live `manga` table
via JDBC and writes the JPA entity.

### 4.1 Generator connection config

Create `src/main/resources/hibernate-generator.properties` (local only — add it to
`.gitignore`, keep a committed `hibernate-generator.properties.example` with placeholders):

```properties
hibernate.connection.driver_class=com.mysql.cj.jdbc.Driver
hibernate.connection.url=jdbc:mysql://localhost:3306/hms
hibernate.connection.username=root
hibernate.connection.password=your-password
hibernate.default_catalog=hms
hibernate.dialect=org.hibernate.dialect.MySQLDialect
```

This file is only for the generator; the running application uses
`application.properties` (section 3).

### 4.2 Limit generation to the `manga` table

Create `src/main/resources/manga.reveng.xml`:

```xml
<hibernate-reverse-engineering>
  <!-- All three filters are required by Hibernate Tools' binder. -->
  <table-filter match-catalog="hms" match-schema=".*" match-name="manga"/>
</hibernate-reverse-engineering>
```

### 4.3 Run the generator

MySQL must be running with the `hms` schema loaded (section 3).

```bash
./gradlew generateJava
```

Expected output:
`src/main/java/com/example/HMS/Manga/entity/Manga.java`

The task reads `hibernate-generator.properties` and `manga.reveng.xml` from
`src/main/resources`, uses the `packageName`/`outputFolder` from the
`hibernateTools` block, and overwrites the file on every run.

### 4.4 Keep the generated entity as-is

Hibernate Tools only maps columns. It cannot know application rules such as
UUID generation, defaults, or timestamps. Instead of editing the generated file
(which the next `generateJava` would overwrite), keep those rules outside it:

| Rule | Where it lives |
| --- | --- |
| `id` is a UUID string | `MangaService.create` |
| `created_at` / `updated_at` on insert | `MangaService.create` |
| `updated_at` refresh on update | `MangaService.update` |
| `status` / `type` defaults | `MangaService.applyRequest` |
| `Date` → `LocalDateTime` for the API | `MangaResponse.from` |

The generated entity stays exactly as produced:

```java
@Entity
@Table(name = "manga")
public class Manga implements java.io.Serializable {
    @Id
    @Column(name = "id", unique = true, nullable = false, length = 36)
    public String getId() { ... }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", nullable = false, length = 19)
    public Date getCreatedAt() { ... }

    // ... generated getters/setters, no custom callbacks
}
```

And the service owns the application rules:

```java
@Transactional
public MangaResponse create(MangaRequest request) {
    Manga manga = new Manga();
    applyRequest(manga, request, true);

    Date now = new Date();
    manga.setId(UUID.randomUUID().toString());
    manga.setCreatedAt(now);
    manga.setUpdatedAt(now);
    manga.setSlug(resolveSlug(request.slug(), request.title()));

    return MangaResponse.from(repository.save(manga));
}
```

With this split you can delete `Manga.java`, run `./gradlew generateJava`, and
it still compiles and runs. No manual re-editing after generation.

> If you *do* edit the generated file, `generateJava` overwrites it on the next
> run. Prefer the service-based rules above.

Optional: model `status` and `type` as enums with `@Enumerated(EnumType.STRING)`
later. Keep `String` for the first iteration.

### 4.5 Other ways to generate the same entity

- **IntelliJ IDEA Ultimate:** Database tool window → right-click `manga` →
  *Generate Persistence Mapping → By Database Schema*.
- **Telosys CLI:** create a model from the MySQL schema and generate the whole
  module (entity + repository + service) from templates.
- **Generate into a shared domain package first:** see
  [`sql-to-model-workflow.md`](sql-to-model-workflow.md).

Hibernate Tools generates entities only; sections 5–9 still define the
repository pattern, service, DTOs, and controller.

## 5. Repository — `MangaRepository`

```java
public interface MangaRepository extends JpaRepository<Manga, String> {

    Optional<Manga> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Page<Manga> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    Page<Manga> findByStatus(String status, Pageable pageable);

    Page<Manga> findByTitleContainingIgnoreCaseAndStatus(
            String title,
            String status,
            Pageable pageable
    );
}
```

This is the repository pattern boundary for the module. Service code must not
use `EntityManager` directly.

## 6. Service — `MangaService`

Responsibilities:

- CRUD operations (`list`, `getById`, `getBySlug`, `create`, `update`, `delete`).
- Generate a URL-safe slug from the title when none is supplied.
- Reject duplicate slugs.
- Throw `MangaNotFoundException` when an ID/slug does not exist.
- Map entity ↔ DTO.

```java
@Service
@Transactional(readOnly = true)
public class MangaService {
    private final MangaRepository repository;

    public MangaService(MangaRepository repository) {
        this.repository = repository;
    }

    public Page<MangaResponse> list(String title, String status, Pageable pageable) { ... }

    public MangaResponse getById(String id) { ... }

    @Transactional
    public MangaResponse create(MangaRequest request) { ... }

    @Transactional
    public MangaResponse update(String id, MangaRequest request) { ... }

    @Transactional
    public void delete(String id) { ... }
}
```

## 7. DTOs and validation

- `MangaRequest`: `title` (`@NotBlank`), `slug` (optional), `description`,
  `coverUrl`, `author`, `artist`, `status`, `type`, `releaseYear`.
- `MangaResponse`: the same fields plus `id`, `createdAt`, `updatedAt`.
- Keep entities out of the controller layer.

## 8. Controller — REST API

Base path: `/api/manga`

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/api/manga` | List with pagination + optional `title`/`status` filters |
| `GET` | `/api/manga/{id}` | Get by ID |
| `GET` | `/api/manga/slug/{slug}` | Get by slug |
| `POST` | `/api/manga` | Create (`201 Created` + `Location`) |
| `PUT` | `/api/manga/{id}` | Update |
| `DELETE` | `/api/manga/{id}` | Delete (`204 No Content`) |

Pagination defaults: `page=0`, `size=20`, `sort=title`.

## 9. Error handling

- `MangaNotFoundException` → `404 Not Found`.
- Validation errors → `400 Bad Request` with field messages.
- Duplicate slug → `409 Conflict`.
- Implement with `@RestControllerAdvice`.

## 10. Testing

- `MangaServiceTest` with mocked `MangaRepository` (unit).
- `MangaControllerTest` with `@WebMvcTest` + `@MockitoBean` service.
- `MangaRepositoryTest` with `@DataJpaTest` + H2 in MySQL mode.
  Spring Boot 4 import:
  `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest`.
- `src/test/resources/application.properties` points tests at in-memory H2, so
  they do not require a running MySQL server.
- Smoke test: start the packaged app against MySQL and exercise the full CRUD
  flow (see section 11).

## 11. Acceptance criteria (verified)

- [x] `./gradlew generateJava` generated
      `src/main/java/com/example/HMS/Manga/entity/Manga.java` from the live
      MySQL table. The entity is used as generated; UUID/timestamp/default rules
      live in `MangaService` and `MangaResponse`.
- [x] `./gradlew test` passed: **18 tests** (1 context, 9 controller,
      3 repository, 5 service).
- [x] `./gradlew bootJar` and app startup against MySQL succeeded with
      `ddl-auto=validate`.
- [x] `POST /api/manga` created a row and generated `smoke-test-manga`.
- [x] `GET /api/manga?title=Smoke&status=ongoing` returned `200`.
- [x] `GET /api/manga/{id}` and `GET /api/manga/slug/{slug}` returned `200`.
- [x] `PUT /api/manga/{id}` updated the row.
- [x] Duplicate slug returned `409`; invalid body returned `400`; missing ID
      returned `404`.
- [x] `DELETE /api/manga/{id}` returned `204` and removed the MySQL row.
- [x] `hibernate-generator.properties` is gitignored and excluded from the
      packaged application artifact.
- [x] `GET /v3/api-docs` returned OpenAPI `3.1.0` with paths `/api/manga`,
      `/api/manga/{id}`, and `/api/manga/slug/{slug}`.
- [x] `GET /scalar.html` returned `200` and serves the Scalar page.

## 12. API documentation — OpenAPI + Scalar

Keep it simple: springdoc generates the OpenAPI JSON, and a static HTML page
renders it with Scalar. No extra controller, no build tooling.

### 12.1 Dependency

```gradle
implementation 'org.springdoc:springdoc-openapi-starter-webmvc-api:3.2.0'
```

This is the API-only starter. It does **not** pull in Swagger UI; Scalar
replaces it.

### 12.2 Scalar page

Create `src/main/resources/static/scalar.html`:

```html
<!doctype html>
<html lang="en">
  <head>
    <title>HMS API Reference</title>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1" />
  </head>

  <body>
    <div id="app"></div>

    <script type="module">
      import { createApiReference } from 'https://cdn.jsdelivr.net/npm/@scalar/api-reference/esm.js'

      createApiReference('#app', {
        // Served by springdoc from the running application.
        url: '/v3/api-docs',
      })
    </script>
  </body>
</html>
```

Spring Boot serves it automatically from static resources, so no controller or
route configuration is required.

### 12.3 Verify

```bash
./grad.sh run
```

Then open:

- `http://localhost:8080/v3/api-docs` — OpenAPI JSON
- `http://localhost:8080/scalar.html` — Scalar API reference

Or from the shell:

```bash
curl -s http://localhost:8080/v3/api-docs | head -c 200
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/scalar.html
```

### 12.4 Notes

- The page loads `@scalar/api-reference` from jsDelivr, so the browser needs
  internet access. The OpenAPI document itself is served locally.
- Same origin means no CORS proxy is needed.
- If you later add Swagger UI, use
  `springdoc-openapi-starter-webmvc-ui`; it is not needed for Scalar.

## Useful commands

Use the wrapper for shortcuts:

```bash
./grad.sh help        # list shortcuts
./grad.sh run         # starts the app (alias for bootRun)
./grad.sh generate    # regenerates models/entities from MySQL
./grad.sh test        # H2-backed tests
./grad.sh build       # full build
```

`./grad.sh run` reads local credentials from `.env` (gitignored). Create it
once:

```bash
cp .env.example .env
# then edit .env and set DB_PASSWORD
```

> The executable is `./grad.sh`, not `./grad`. Running `./grad run` gives
> `No such file or directory`.

While the app is running:

- API docs: `http://localhost:8080/scalar.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Equivalent raw Gradle commands:

```bash
./gradlew run          # starts the app (alias for bootRun)
./gradlew bootRun      # same thing
./gradlew generateJava # regenerates Domain/entity models from MySQL
./gradlew test         # H2-backed tests
./gradlew build        # full build
```

> Avoid `./gradlew runSql`: it is a Hibernate Tools task written for H2 and
> hardcodes the username `sa`, so it fails against the MySQL setup.

## Notes / later steps

- Swing UI: see [`swing-components-plan.md`](swing-components-plan.md) for the
  phased component plan (simple → complex).
- Add Flyway once schema changes start happening outside `database/schema.sql`.
- Add Spring Security for the `accounts`/`roles` tables.
- Add Playwright/ratings or other manga sub-resources only after this base module
  is stable.
