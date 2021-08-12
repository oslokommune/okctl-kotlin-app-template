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

// TODO so, should we continue down this path, or should we uh, hmm
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
            addUser(username.toString())
            call.respond(mapOf("User added" to username))
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
            get("/writetofile") {
                writeToPvc()
                call.respondText("Attemted writing to PVC")
            }

        }
        routing {
            file("gopher.png")
            file("OsloSans-Regular.woff")
        }
    }
}

private fun writeToPvc() {
    val fileName = "template/local/storage/myfile.txt"
    val myfile = File(fileName)

    myfile.printWriter().use { out ->

        out.println("First line")
        out.println("Second line")
    }

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

private fun Application.addUser(username: String) {
    val datasource = getDatasource()
    val database = connectToDatabase(datasource)

    database.insert(Users) {
        set(it.id, UUID.randomUUID().toString())
        set(it.name, username)
    }

    datasource.close()
}


private fun Application.getDatasource(): ComboPooledDataSource {
    // Database
    val datasource = ComboPooledDataSource()

    var found = true
    val dbEndpoint = try {
        getEnv("DB_ENDPOINT")
    } catch (e: Exception) {

        log.info("DB endpoint not found.")
        log.info("Setting found to false!")
        found = false
    }

    if (found) {
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