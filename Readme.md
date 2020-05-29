# Jootainer 
`jootainer` is a simple gradle plugin that simplifies code generation with [Jooq](https://www.jooq.org/) by starting a temporary Postgresql container via Docker.
Therefore, `jootainer` helps you to keep your build process self-contained and deterministic.

`jootainer` performs the following steps:
1. Start a postgresql container via [test-containers](https://www.testcontainers.org/)
2. Run [Flyway](https://flywaydb.org/) migration scripts
3. Run [jooq-codegen](https://www.jooq.org/doc/latest/manual/code-generation/codegen-configuration/)

In other words: `jootainer = test-containers + flyway + jooq-codegen`

## Usage
Add `jootainer` to your `plugins` section
```kotlin
plugins {
    // ...
    id("de.sparkteams.jootainer") version "0.9.2"
}
```

Or in Groovy syntax:
```groovy
plugins {
  id "de.sparkteams.jootainer" version "0.9.2"
}
```

and (if you're unhappy with the defaults) configure it like this:
```kotlin
configure<de.sparkteams.jootainer.JootainerExtension> {
    image = "postgres:11-alpine"
    packageName = "com.myproject.generated.jooq"
    migrationDir = "${projectDir}/src/main/resources/db/migration"
    outputDir = "${projectDir}/src/main/kotlin"
    generate = org.jooq.meta.jaxb.Generate().withJavaTimeTypes(false)

}
```
### Configuration options

| Option | Description|
| -------| ----- | 
| `image` | image name (default: `postgres:11-alpine`)|
| `packageName` | name of the package for generated classes (default `db.jootainer`)";
| `migrationDir` | directory in which jootainer looks for flyway migrations (default `src/main/resources/db/migration`)
| `outputDir` | directory to write the generated files (default: src/main/kotlin)
| `generate`  | an instance of jooq's `Generate` class  (see jooq)


## License
see [License](./LICENSE)

## Contributing

This software is a working early version, it covers our specific use cases but might need more configuration
 options for your case. We welcome especially feedback that tells us how *you* 
 use Jooq and how `jootainer` could be useful for you.



