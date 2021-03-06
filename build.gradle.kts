import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.20"
    application
}

group = "net.entelijan"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jdom:jdom2:2.0.6.1")
    implementation("org.junit.jupiter:junit-jupiter:5.8.2")
    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.4")
    implementation("io.ktor:ktor-server-netty:1.6.8")
    implementation("ch.qos.logback:logback-classic:1.2.11")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "16"
}

application {
    mainClass.set("exop.MainKt")
}

tasks.withType<Test> {
    this.testLogging {
        this.showStandardStreams = true
    }
}