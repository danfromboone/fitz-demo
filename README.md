# fitz-demo

Sample web application demonstrating [**fitz**](https://github.com/danfromboone/fitz) — explicit composition-root Dependency Injection (`ApplicationContext`), live-reconfigurable database pooling (`DataContext` / `MutableDataSource`), layered & encrypted configuration properties, and Jakarta Servlet request dispatch (`DispatcherServlet`).

---

## Architecture (`dfb.demo` & `dfb.demo.domain`)

- **`ApplicationContext`**: A `@WebListener` (`ServletContextListener`) that acts as the composition root—wiring the property pipeline, `DataContext`, repositories (`FooRepository`, `GooRepository`), services (`FooService`, `GooService`), controllers (`FooController`), and registering `DispatcherServlet` with the servlet container.
- **`DataContext`**: Registers a `"database\\..*"` listener on `MutablePropertySource` and updates `MutableDataSource` (`dfb.fitz.jdbc`) with a new `DatabaseConfig` whenever database properties change—without re-instantiating repositories or services.
- **`FooController` / `FooService` / `FooRepository` / `GooService` / `GooRepository`**: Sample domain components wired via pure constructor injection.

```text
default.properties ──┐
                     ├──► CompositePropertySource ──► DecryptingPropertySource ──► DataContext
local.properties   ──┘                                                                 │
(PollingPropertySource)                                                                ▼
                                                                             MutableDataSource
                                                                              (DatabaseConfig)
                                                                                 ┌─────┴─────┐
                                                                                 ▼           ▼
                                                                           FooRepository GooRepository
                                                                                 │           │
                                                                                 ▼           ▼
                                                                            FooService   GooService
                                                                                 │
                                                                                 ▼
                                                                           FooController
                                                                                 │
                                                                                 ▼
                                                                         DispatcherServlet
```

---

## Multi-Repo Development Setup

When `fitz` and `fitz-demo` are checked out as sibling directories:
```text
IdeaProjects/
├── fitz/
└── fitz-demo/
```
`settings.gradle` uses `includeBuild("../fitz")` so Gradle and IntelliJ link both repositories as live modules in the same workspace without needing `publishToMavenLocal`.

---

## Building and Running

### Run Unit & Integration Tests
```bash
./gradlew test
```

### Build WAR Archive
```bash
./gradlew war
```
The packaged WAR is generated at `build/libs/fitz-demo-1.0-SNAPSHOT.war`.

---

## License

Licensed under the [Apache License, Version 2.0](LICENSE).
