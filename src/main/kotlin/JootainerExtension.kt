package de.sparkteams.jootainer

import org.gradle.api.Project

open class JootainerExtension {
    var packageName: String = "xxx";
    var migrationDir: String = "src/main/resources/db/migration"
    var outputDir: String = "src/main/kotlin"

}

