package de.sparkteams.jootainer

import org.jooq.meta.jaxb.Generate

open class JootainerExtension {
    var image: String = "postgres:11-alpine"
    var packageName: String = "db.generated"
    var migrationDir: String = "src/main/resources/db/migration"
    var outputDir: String = "src/main/kotlin"
    var generate: Generate? = null

}

