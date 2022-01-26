import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
    val kotlinversion = "1.6.10"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    kotlin("jvm") version kotlinversion
    kotlin("plugin.serialization") version kotlinversion
    application

}
val applicationVersion = "0.0.1-SNAPSHOT"

group = "origo.myproduct"
version = applicationVersion
java.sourceCompatibility = JavaVersion.VERSION_17
java.targetCompatibility = JavaVersion.VERSION_17

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

println("Using java version: " + JavaVersion.current())
if (java.sourceCompatibility > JavaVersion.current()) {
    error("JDK version ${JavaVersion.current()} detected. This project should be compiled with a JDK that supports" +
            " source compatibility for version ${java.sourceCompatibility}")
}

repositories {
    mavenCentral()
    maven { setUrl("https://jitpack.io") }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.2.10")
    implementation("ch.qos.logback:logback-core:1.2.10")
    implementation("org.slf4j:slf4j-api:1.7.35")
    implementation("io.github.microutils:kotlin-logging:2.1.21")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")

    // KTor
    val ktor_version = "1.6.7"
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("io.ktor:ktor-locations:$ktor_version")
    implementation("io.ktor:ktor-gson:$ktor_version")
    implementation("io.ktor:ktor-client-cio:$ktor_version")
    implementation("io.ktor:ktor-client-gson:$ktor_version")
    implementation("io.ktor:ktor-serialization:$ktor_version")
    implementation("io.ktor:ktor-jackson:$ktor_version")
    testImplementation("io.ktor:ktor-server-tests:$ktor_version")

    // Database
    implementation("org.postgresql:postgresql:42.3.1")
    implementation("org.flywaydb:flyway-core:8.4.2")
    implementation("com.mchange:c3p0:0.9.5.5")

    // Database ORM: Ktorm
    val ktorm_version = "3.4.1"
    implementation("org.ktorm:ktorm-core:$ktorm_version")
    implementation( "org.ktorm:ktorm-support-postgresql:${ktorm_version}")

    // Note: 2.13.1 is the relevant jackson-version as of ktor-jackson 1.6.7. Keep an eye on this when upgrading ktor.
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.13.1")

    // Metrics
    implementation ("io.ktor:ktor-metrics:$ktor_version")
    implementation ("io.ktor:ktor-metrics-micrometer:$ktor_version")
    implementation ("io.micrometer:micrometer-registry-prometheus:1.8.2")

}

tasks.test {
	useJUnitPlatform()
	testLogging {
		events("passed", "skipped", "failed")
        exceptionFormat=org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
	}
}

application {
    mainClass.set("origo.myapp.MainKt")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    manifest.attributes["Specification-Version"] = applicationVersion
    manifest.attributes["Implementation-Version"] = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    // Necessary for embedded app servers (among other things) to work from built jar.
    // See: https://stackoverflow.com/questions/48636944/how-to-avoid-a-java-lang-exceptionininitializererror-when-trying-to-run-a-ktor-a
    mergeServiceFiles()
}

