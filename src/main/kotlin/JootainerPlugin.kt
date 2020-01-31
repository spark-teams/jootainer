package de.sparkteams.jootainer

import org.flywaydb.core.Flyway
import org.gradle.api.*
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.jooq.codegen.GenerationTool
import org.jooq.meta.jaxb.*
import org.jooq.meta.jaxb.Configuration
import org.jooq.meta.jaxb.Target
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import java.io.File


class JootainerPlugin : Plugin<Project> {

    // helper classes
    class KGenericContainer(imageName: String) : GenericContainer<KGenericContainer>(imageName)

    class KPostgreSQLContainer(imageName: String) : PostgreSQLContainer<KPostgreSQLContainer>(imageName)

    open class GenerateJooqFiles : DefaultTask() {
        var inputDir = "src/main/resources/db/migration"
        var outputDir = "src/main/kotlin"

        @InputDirectory
        fun getInputDir(): File {
            return File(inputDir)
        }

        @OutputDirectory
        fun getOutputDir(): File {
            return File(outputDir)
        }

        @TaskAction

        fun run() {
            logger.info("starting postgres container")
            val container = KPostgreSQLContainer("postgres:11-alpine")
            container.start()
            val url = container.jdbcUrl
            val user = container.username
            val password = container.password
            logger.info("successfully started container: ${url}, ${user}, ${password}")

            /* start flyway */
            logger.info("starting flyway migration")
            val flyway = Flyway.configure()
                .dataSource(url, user, password)
                .load()
            flyway.info()
            flyway.migrate()
            logger.info("flyway migration successful")

            /* jooq code generation */

            logger.info("starting jooq code generation")
            val jooqConf: Configuration = Configuration()
                .withJdbc(Jdbc()
                    .withDriver("org.postgresql.Driver")
                    .withUrl(url)
                    .withUser(user)
                    .withPassword(password)
                )
                .withGenerator(Generator()
                    .withStrategy(Strategy().withName("org.jooq.codegen.DefaultGeneratorStrategy"))
                    .withDatabase(Database()
                        .withName("org.jooq.meta.postgres.PostgresDatabase")
                        .withInputSchema("public")
                    )
                    .withGenerate(Generate()
                        .withRelations(true)
                        .withRecords(true)
                        .withImmutablePojos(true)
//                                    .withFluentSetters(true)
//                                    .withJavaTimeTypes(true)
                        .withIndexes(true)
                        .withRoutines(true)
                    )
                    .withTarget(Target()
                        .withDirectory(outputDir)
                        .withPackageName("de.markant.voila.persistence.jooq"))
                )

            GenerationTool.generate(jooqConf)
            logger.info("jooq code generation successful")
            container.stop()
        }


    }

    override fun apply(project: Project) {
        val generateJooqFiles = project.tasks.register<GenerateJooqFiles>("generateJooqFiles", GenerateJooqFiles::class.java).get()
        project.tasks.getByName("compileKotlin").dependsOn(generateJooqFiles)
        project.tasks.getByName("compileJava").dependsOn(generateJooqFiles)
    }
}
