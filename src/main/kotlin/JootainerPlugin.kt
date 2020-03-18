package de.sparkteams.jootainer

import org.flywaydb.core.Flyway
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.jooq.codegen.GenerationTool
import org.jooq.meta.jaxb.*
import org.jooq.meta.jaxb.Target
import org.testcontainers.containers.PostgreSQLContainer
import java.io.File

/* somehow Kotlin cannot infer self-referential generic classes */
class KPostgreSQLContainer(imageName: String) : PostgreSQLContainer<KPostgreSQLContainer>(imageName)

open class JootainerPlugin : Plugin<Project> {


    override fun apply(project: Project) {
        project.extensions.create("jootainer", JootainerExtension::class.java)

        val generateJooqFiles =
            project.tasks.register("jootainer", JootainerTask::class.java).get()

        project.afterEvaluate {
            val ext = project.extensions.getByType(JootainerExtension::class.java)
            generateJooqFiles.setExtension(ext)
        }

        project.tasks.getByName("compileKotlin").dependsOn(generateJooqFiles)
        project.tasks.getByName("compileJava").dependsOn(generateJooqFiles)
    }
}

open class JootainerTask : DefaultTask() {
    private lateinit var extension: JootainerExtension

    fun setExtension(extension: JootainerExtension) {
        this.extension = extension
    }

    val outputPackageName: String
        @Input
        get() = this.extension.packageName

    val outputDirectory: String
        @OutputDirectory
        get() = this.extension.outputDir

    val generate: Generate
        @Input
        get() = this.extension.generate ?: Generate()

    val migrationDirectory: String
        @InputDirectory
        get() = this.extension.migrationDir

    val image: String
        @Input
        get() = this.extension.image

    @InputDirectory
    fun getMigrationDir(): File {
        return File(this.migrationDirectory); }

    @OutputDirectory
    fun getOutputDir(): File {
        return File(this.outputDirectory); }


    @TaskAction
    fun run() {
        withContainer {
            this.runMigrations(it)
            this.runCodegen(it)
        }
    }

    private fun <A> withContainer(k: (KPostgreSQLContainer) -> A): A {
        logger.info("starting postgres container")
        val container = KPostgreSQLContainer(image)
        container.start()
        val result = k(container)
        logger.info("stopping postgres container")
        container.stop()
        return result
    }

    private fun runMigrations(container: KPostgreSQLContainer) {
        logger.info("starting flyway migration")
        val flyway = Flyway.configure()
            .dataSource(container.jdbcUrl, container.username, container.password)
            .load()
        flyway.info()
        flyway.migrate()
        logger.info("flyway migration successful")

    }

    private fun runCodegen(container: KPostgreSQLContainer) {
        logger.info("starting jooq code generation")
        val jooqConf: Configuration = Configuration()
            .withJdbc(
                Jdbc()
                    .withDriver("org.postgresql.Driver")
                    .withUrl(container.jdbcUrl)
                    .withUser(container.username)
                    .withPassword(container.password)
            )
            .withGenerator(
                Generator()
                    .withStrategy(Strategy().withName("org.jooq.codegen.DefaultGeneratorStrategy"))
                    .withDatabase(
                        Database()
                            .withName("org.jooq.meta.postgres.PostgresDatabase")
                            .withInputSchema("public")
                    )
                    .withGenerate(this.generate)
                    .withTarget(
                        Target()
                            .withDirectory(outputDirectory)
                            .withPackageName(outputPackageName)
                    )
            )

        GenerationTool.generate(jooqConf)
        logger.info("jooq code generation successful")
    }


}

