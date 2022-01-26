package origo.myapp

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.mchange.v2.c3p0.ComboPooledDataSource
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.jackson.*
import io.ktor.metrics.micrometer.*
import io.ktor.response.*
import io.ktor.routing.*
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import mu.KotlinLogging
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.Location
import org.flywaydb.core.api.configuration.ClassicConfiguration
import java.io.File
import javax.sql.DataSource
import org.ktorm.database.Database
import org.ktorm.dsl.from
import org.ktorm.dsl.insert
import org.ktorm.dsl.select
import org.ktorm.entity.Entity
import org.ktorm.schema.Table
import org.ktorm.schema.varchar
import org.ktorm.support.postgresql.PostgreSqlDialect
import java.util.*
import kotlin.collections.ArrayList
import kotlin.random.Random

private val logger = KotlinLogging.logger {}

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.main() {
    install(ContentNegotiation) {
        jackson {
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            disable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
            registerModule(JavaTimeModule())
        }
    }

    val appMicrometerRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    install(MicrometerMetrics) {
        registry = appMicrometerRegistry
    }


    setupRouting(appMicrometerRegistry)
    try {
        setupDatabase()
    } catch (e: Exception) {
        logger.error(e.message)
    }

    logger.info("Application started")
}

private fun Application.setupRouting(
    appMicrometerRegistry: PrometheusMeterRegistry
) {
    val html = File("index.html").readText()
    routing {
        get("/health") {
            call.respond(mapOf("OK" to "true"))
        }

        get("/") {
            call.respondText(html, ContentType.Text.Html)
        }

        get("/risky") {
            val crash = Random.nextBoolean()
            if (crash) {
                call.respond("It's a crash! Check your log!")
                throw RuntimeException("Randomly crashed!")
            } else {
                call.respond(mapOf("You got" to "lucky"))
            }
        }

        get("/logthis") {
            val logline = call.request.queryParameters["logline"]
            log.info(logline)
            call.respond(mapOf("Check your log for" to logline))
        }

        // Yes this should be a post, this is not a Ktor refrence app :)
        get("/adduser") {
            val username = call.request.queryParameters["username"]
            call.respond(addUser(username.toString()))

        }

        routing {
            get("/metrics") {
                call.respond(appMicrometerRegistry.scrape())
            }
        }
        routing {
            get("/users") {
                val usernames = getUsernames()
                call.respondText(usernames.toString())
            }
        }

        routing {
            get("/writetopvc") {
                val fileline = call.request.queryParameters["fileline"]
                val result = writeToPvc(fileline.toString())
                call.respond(result)
            }
        }

        routing {
            get("/readfrompvc") {
                val pvcFileContent = readFromPvc().toString()
                call.respondText(pvcFileContent)
            }
        }

        routing {
            file("gopher.png")
            file("OsloSans-Regular.woff")
        }
    }
}

private fun Application.writeToPvc(value: String): String {
    val fileName = try {
        getEnv("PVC_PATH")
    } catch (e: Exception) {
        return pvcConfigError()
    }


    return try {
        val file = File(fileName)
        file.appendText("$value\n\r")
        "Wrote $value to PVC."
    } catch (e: Exception) {
        e.message.toString()
    }
}

private fun Application.pvcConfigError(): String {
    return "PVC is not configured correctly.\n" +
"You need to set PVC_PATH environment variable.\n\n" +
"For more information, see https://okctl.io/help/setup-reference-app/"
}

private fun Application.readFromPvc(): ArrayList<String> {
    val result = arrayListOf<String>()

    val fileName = try {
        getEnv("PVC_PATH")
    } catch (e: Exception) {
        result.add(pvcConfigError())
        return result
    }

    val myfile = File(fileName)

    try {
        val readLines = myfile.readLines()
        for (line in readLines) {
            if (!line.isNullOrEmpty()) {
                result.add(line)
            }
        }


    } catch (e: Exception) {
        result.add(e.message.toString())
    }
    return result
}

interface User : Entity<User> {
    companion object : Entity.Factory<User>()

    val id: String
    val name: String
}

/**
 * The employee table object.
 */
object Users : Table<User>("users") {
    val id = varchar("id").primaryKey().bindTo { it.id }
    val name = varchar("name").bindTo { it.name }
}


private fun Application.setupDatabase() {
    val datasource = getDatasource()
    if (datasource.jdbcUrl != null) {
        val flywayConfig = ClassicConfiguration()
        flywayConfig.setLocations(Location("classpath:/sql/migrations/"))
        flywayConfig.dataSource = datasource
        flywayConfig.setBaselineVersionAsString("1")
        flywayConfig.isBaselineOnMigrate = true
        val flyway = Flyway(flywayConfig)

        log.info("Running flyway migrations...")
        flyway.migrate()
        log.info("Flyway migrations done.")
    }
}

private fun Application.getUsernames(): ArrayList<String> {
    val usernames = arrayListOf<String>()
    val datasource: ComboPooledDataSource
    val database: Database

    try {
        datasource = getDatasource()
        database = connectToDatabase(datasource)
    } catch (e: Exception) {
        return arrayListOf(e.message.toString())
    }

    for (row in database.from(Users).select()) {
        val name: String? = row[Users.name]

        if (!name.isNullOrEmpty()) {
            usernames.add(name)
        }
    }
    datasource.close()
    return usernames
}

private fun Application.addUser(username: String): String {
    val datasource = try {
        getDatasource()
    } catch (e: Exception) {
        return e.message.toString()
    }

    val database = connectToDatabase(datasource)

    database.insert(Users) {
        set(it.id, UUID.randomUUID().toString())
        set(it.name, username)
    }

    datasource.close()

    return "User $username added successfully!"
}


private fun Application.getDatasource(): ComboPooledDataSource {

    val dbEndpoint = try {
        getEnv("DB_ENDPOINT")
    } catch (e: Exception) {
        throw RuntimeException("Database is not configured correctly.\n" +
"You need to set the following environment variables:\n" +
"DB_ENDPOINT, DB_USERNAME, DB_PASSWORD and DB_NAME.\n\n" +
"For more information, see https://okctl.io/help/setup-reference-app/")
    }

    // Database
    val datasource = ComboPooledDataSource()
    log.info("found endpoint " + dbEndpoint)

    val dbUsername = getEnv("DB_USERNAME")
    val dbPassword = getEnv("DB_PASSWORD")
    val dbName = getEnv("DB_NAME")
    val connectString = "jdbc:postgresql://$dbEndpoint/$dbName"


    datasource.driverClass = "org.postgresql.ds.PGSimpleDataSource" //Real driver set in connect string.
    datasource.jdbcUrl = connectString
    datasource.user = dbUsername
    datasource.password = dbPassword

    log.info("Using database: $dbName. Endpoint: $dbEndpoint.")
    return datasource

}

private fun getEnv(env: String): String {
    return System.getenv(env) ?: throw RuntimeException("Could not find environment variable: $env")
}

private fun connectToDatabase(datasource: DataSource): Database {
    val db = Database.connect(
        datasource,
        dialect = PostgreSqlDialect()
    )
    return db
}