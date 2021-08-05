package origo.myapp

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.mchange.v2.c3p0.ComboPooledDataSource
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
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
import org.ktorm.dsl.select
import org.ktorm.entity.Entity
import org.ktorm.schema.Table
import org.ktorm.schema.varchar
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

    val appMicrometerRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    install(MicrometerMetrics) {
        registry = appMicrometerRegistry
    }


    setupRouting(appMicrometerRegistry)
    setupDatabase()
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
            call.respond(mapOf("hello" to "world"))
        }

        get("/test") {
            call.respondText(html, ContentType.Text.Html)
            logger.info("Test page called.")
        }
        routing {
            get("/metrics") {
                call.respond(appMicrometerRegistry.scrape())
            }
        }

        // TODO OK cool, we have users in the database, next step is to figure out how to fetch them and do the thing

        routing {
            get("/users") {
                val usernames = getUsernames()
                call.respondText(usernames.toString())
            }
        }
    }
}


interface User : Entity<User> {
    companion object : Entity.Factory<User>()
    val id: String
    var name: String
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

    val flywayConfig = ClassicConfiguration()
    flywayConfig.setLocations(Location("classpath:/sql/migrations/"))
    flywayConfig.dataSource = datasource
    flywayConfig.isIgnoreMissingMigrations = true
    flywayConfig.setBaselineVersionAsString("1")
    flywayConfig.isBaselineOnMigrate = true
    val flyway = Flyway(flywayConfig)

    log.info("Running flyway migrations...")
    log.info("wait for it")
    flyway.migrate()
    log.info("aaan it's crashed")
    log.info("Flyway migrations done.")
}

private fun Application.getUsernames(): ArrayList<String> {
    val usernames = arrayListOf<String>()

    val datasource = getDatasource()
    val database = connectToDatabase(datasource)


    for (row in database.from(Users).select()) {
        val id: String? = row[Users.id]
        val name: String? = row[Users.name]

        if (!name.isNullOrEmpty()) {
            usernames.add(name)
        }
    }
    datasource.close()
    return usernames
}

private fun Application.getDatasource(): ComboPooledDataSource {
    // Database
    val datasource = ComboPooledDataSource()
    val dbEndpoint = getEnv("DB_ENDPOINT")

    if (dbEndpoint.length == 0) {
        log.info("DB endpoint not found!")
    } else {
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
    }
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