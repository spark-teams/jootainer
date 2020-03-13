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
import kotlin.system.exitProcess

open class JootainerPlugin : Plugin<Project> {

    class KPostgreSQLContainer(imageName: String) : PostgreSQLContainer<KPostgreSQLContainer>(imageName)

    override fun apply(project: Project) {
        project.extensions.create("jootainer", JootainerExtension::class.java)

        val generateJooqFiles =
            project.tasks.register<GenerateJooqFiles>("jootainer", GenerateJooqFiles::class.java).get()

        project.afterEvaluate {
            var ext = project.extensions.getByType(JootainerExtension::class.java)
            generateJooqFiles.extension = ext;
        }

        project.tasks.getByName("compileKotlin").dependsOn(generateJooqFiles)
        project.tasks.getByName("compileJava").dependsOn(generateJooqFiles)
    }
}

open class GenerateJooqFiles : DefaultTask() {
    lateinit var extension: JootainerExtension

    val outputPackageName: String
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

    @InputDirectory
    fun getMigrationDir(): File {
        return File(this.migrationDirectory); }

    @OutputDirectory
    fun getOutputDir(): File {
        return File(this.outputDirectory); }


    @TaskAction
    fun run() {
        logger.info("starting generateJooq task with the following settings")
        logger.info("package name : " + this.outputPackageName)

        if (this.outputPackageName == "xxx") {
            logger.error("shit, why can I not set configuration options?")
            exitProcess(1)
        }

        logger.info("starting postgres container")
        val container = JootainerPlugin.KPostgreSQLContainer("postgres:11-alpine")
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
            .withJdbc(
                Jdbc()
                    .withDriver("org.postgresql.Driver")
                    .withUrl(url)
                    .withUser(user)
                    .withPassword(password)
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
        container.stop()
    }


}

