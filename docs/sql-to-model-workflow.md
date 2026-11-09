# SQL → Domain Models → Module

Simple workflow for generating JPA models from the MySQL schema into
`Domain/Models`, then using those models inside a feature module such as
`Manga`.

## Flow

```
database/schema.sql
        │
        ▼
./gradlew generateJava
        │
        ▼
com.example.HMS.Domain.Models
        │
        ├── MangaRepository
        ├── MangaService
        ├── MangaController
        └── Manga DTOs
```

The generated model is the shared domain/persistence model. The module contains
the repository, service, DTOs, controller, and exceptions.

## Target structure

```
src/main/java/com/example/HMS/
├── Domain/
│   └── Models/
│       └── Manga.java          # generated from the manga table
└── Manga/
    ├── controller/
    │   └── MangaController.java
    ├── dto/
    │   ├── MangaRequest.java
    │   └── MangaResponse.java
    ├── repo/
    │   └── MangaRepository.java
    ├── service/
    │   └── MangaService.java
    └── exception/
        └── ...
```

> `Domain.Models` keeps the generated model separate from module code. If you
> prefer Java naming conventions, use `domain.models` instead.

## 1. Point the generator at Domain/Models

In `build.gradle`:

```gradle
hibernateTools {
    packageName = 'com.example.HMS.Domain.Models'
    outputFolder = 'src/main/java'
    hibernateProperties = 'hibernate-generator.properties'
    revengFile = 'manga.reveng.xml'
    generateAnnotations = true
    useGenerics = true
}
```

Only `packageName` changes from the current setup. Generation now writes:

```
src/main/java/com/example/HMS/Domain/Models/Manga.java
```

## 2. Choose which tables to generate

### Only the Manga table

Keep `src/main/resources/manga.reveng.xml`:

```xml
<hibernate-reverse-engineering>
  <table-filter match-catalog="hms" match-schema=".*" match-name="manga"/>
</hibernate-reverse-engineering>
```

### All tables

Remove the `revengFile` line from the `hibernateTools` block:

```gradle
hibernateTools {
    packageName = 'com.example.HMS.Domain.Models'
    outputFolder = 'src/main/java'
    hibernateProperties = 'hibernate-generator.properties'
    generateAnnotations = true
    useGenerics = true
}
```

Then every table in `hms` is generated into `Domain/Models`.

## 3. Generate

```bash
./gradlew generateJava
```

Expected output for the manga table:

```
src/main/java/com/example/HMS/Domain/Models/Manga.java
```

The generator reads `src/main/resources/hibernate-generator.properties`, which
must contain valid local MySQL credentials. That file is gitignored.

## 4. Do not edit the generated model

The generator cannot infer application rules, but don't add them to the
generated file — the next `generateJava` overwrites it. Put them in the module
instead:

| Rule | Where |
| --- | --- |
| `id` UUID string (DB has no default) | `MangaService.create` |
| `created_at` / `updated_at` on insert | `MangaService.create` |
| `updated_at` refresh on update | `MangaService.update` |
| `status` / `type` defaults | `MangaService.applyRequest` |
| `Date` → `LocalDateTime` for the API | `MangaResponse.from` |

Example:

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

The generated `Date` fields are fine. Convert them at the DTO boundary:

```java
private static LocalDateTime toLocalDateTime(Date date) {
    return date == null
            ? null
            : LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
}
```

With this split you can delete the generated model, rerun `generateJava`, and
the project still compiles and runs — no manual re-editing.

## 5. Import the model into the module

The module now imports `com.example.HMS.Domain.Models.Manga`.

### Repository

```java
package com.example.HMS.Manga.repo;

import com.example.HMS.Domain.Models.Manga;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MangaRepository extends JpaRepository<Manga, String> {
}
```

### Service

```java
package com.example.HMS.Manga.service;

import com.example.HMS.Domain.Models.Manga;
import com.example.HMS.Manga.repo.MangaRepository;
import org.springframework.stereotype.Service;

@Service
public class MangaService {
    private final MangaRepository repository;

    public MangaService(MangaRepository repository) {
        this.repository = repository;
    }

    public Manga findById(String id) {
        return repository.findById(id)
                .orElseThrow();
    }
}
```

### DTO mapping

Keep `MangaRequest` and `MangaResponse` in the module. They import the model
only to read/write fields:

```java
import com.example.HMS.Domain.Models.Manga;

public static MangaResponse from(Manga manga) {
    return new MangaResponse(
            manga.getId(),
            manga.getTitle(),
            manga.getSlug(),
            manga.getStatus(),
            manga.getType()
    );
}
```

The controller still depends on the service and DTOs, not on the model directly.

## 6. Verify

```bash
./gradlew compileJava
./gradlew test
./grad.sh run
```

Checklist:

- [ ] `Domain/Models/Manga.java` exists and has `@Entity` / `@Table(name = "manga")`.
- [ ] No old copy remains under `Manga/entity/`.
- [ ] `MangaRepository`, `MangaService`, and DTOs import `Domain.Models.Manga`.
- [ ] App starts with `spring.jpa.hibernate.ddl-auto=validate`.
- [ ] Tests pass.

## Direct vs staging output

### Direct (simple)

```gradle
outputFolder = 'src/main/java'
```

Generated files land directly in `Domain/Models` and are ready to use.

**Use when:** you are bootstrapping the project and want the model in place.

### Staging (safer for regeneration)

```gradle
outputFolder = 'build/generated-sources/hibernate'
```

Generated files land under:

```
build/generated-sources/hibernate/com/example/HMS/Domain/Models/
```

Copy the models you want into `src/main/java/com/example/HMS/Domain/Models/`
as generated; application rules stay in the service/DTO layer.

**Use when:** the schema changes often and you want to diff before replacing
models.

## When to use a separate entity

For a basic module, using the generated JPA model directly as the domain model
is enough.

Use a separate persistence entity only when you need to protect the domain from
JPA concerns:

```
Domain/Models/Manga.java          # pure domain model
Manga/entity/MangaEntity.java     # JPA entity
Manga/repo/MangaJpaRepository.java
Manga/service/MangaMapper.java
```

That adds mapping code. Start simple; split later only if needed.

## Notes

- The generator creates models, not Spring repositories/services/controllers.
- Use generated models as-is; keep application rules in the service/DTO layer
  so `generateJava` can be rerun safely.
- If you move `Manga.java` out of `Manga/entity`, delete the old file so there
  are not two `@Entity` classes for the same table.
- Keep table-specific filters in `manga.reveng.xml` until you are ready to
  generate the whole schema.
