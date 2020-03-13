package de.sparkteams.jootainer

import org.gradle.api.Project
import org.jooq.meta.jaxb.Generate

open class JootainerExtension {
    var packageName: String = "xxx";
    var migrationDir: String = "src/main/resources/db/migration"
    var outputDir: String = "src/main/kotlin"
    var generate: Generate? = null

}

