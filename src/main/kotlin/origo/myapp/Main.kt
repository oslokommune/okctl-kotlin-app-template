package origo.myapp

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.mchange.v2.c3p0.ComboPooledDataSource
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.response.*
import io.ktor.routing.*
import mu.KotlinLogging
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.Location
import org.flywaydb.core.api.configuration.ClassicConfiguration
import java.io.File
import javax.sql.DataSource
import org.ktorm.database.Database
import org.ktorm.support.postgresql.PostgreSqlDialect

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

    val html = File("index.html").readText()

    routing {
        get("/health") {
            call.respond(mapOf("OK" to "true"))
        }

        get("/") {
            call.respond(mapOf("hello" to "world"))
        }

        get("/test") {
            call.respondText(html, ContentType.Text.Html)
        }
    }

    setupDatabase()
    logger.info("Application started")
}


private fun Application.setupDatabase() {
    // Database
    // TODO this is a HACK, fix it properly, we have 10 min to allmøte.
    val dbEndpoint = try {
        getEnv("DB_ENDPOINT")
    } catch (e: Exception) {
        return
    }
    val dbUsername = getEnv("DB_USERNAME")
    val dbPassword = getEnv("DB_PASSWORD")
    val dbName = getEnv("DB_NAME")
    val connectString = "jdbc:postgresql://$dbEndpoint/$dbName"

    val datasource = ComboPooledDataSource()
    datasource.driverClass = "org.postgresql.ds.PGSimpleDataSource" //Real driver set in connect string.
    datasource.jdbcUrl = connectString
    datasource.user = dbUsername
    datasource.password = dbPassword

    log.info("Using database: $dbName. Endpoint: $dbEndpoint.")

    val flywayConfig = ClassicConfiguration()
    flywayConfig.setLocations(Location("classpath:/sql/migrations/"))
    flywayConfig.dataSource = datasource
    flywayConfig.isIgnoreMissingMigrations = true
    flywayConfig.setBaselineVersionAsString("1")
    val flyway = Flyway(flywayConfig)

    log.info("Running flyway migrations...")
    flyway.migrate()
    log.info("Flyway migrations done.")

    log.info("Connecting Ktorm framework to database.")
    connectToDatabase(datasource)
    log.info("Connected to database.")
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