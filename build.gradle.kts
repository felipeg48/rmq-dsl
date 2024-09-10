plugins {
    kotlin("jvm") version "2.0.20"
    kotlin("plugin.serialization") version "2.0.20"
}

group = "dev.felipeg48"
version = "1.0-SNAPSHOT"

val rabbitmqVersion = project.properties["rabbit.client.version"] as String
val kotlinSerializationVersion = project.properties["kotlinx.serialization.version"] as String
val slf4jVersion = project.properties["slf4j.version"] as String
val logbackVersion = project.properties["logback.version"] as String

repositories {
    mavenCentral()
}

dependencies {
    // RabbitMQ amqp-client
    implementation("com.rabbitmq:amqp-client:$rabbitmqVersion")
    // kotlinx.serialization.json
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinSerializationVersion")
    // SLF4J
    testImplementation("ch.qos.logback:logback-classic:$logbackVersion")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}

tasks.withType<Test> {
    testLogging {
        showStandardStreams = true
    }
}
