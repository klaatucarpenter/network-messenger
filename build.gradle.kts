plugins {
    id("java")
    id("application")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

application {
    // Default entry point for `./gradlew run`
    mainClass.set("chat.server.ChatServer")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.mockito:mockito-junit-jupiter:5.14.2")
    testImplementation("org.mockito:mockito-core:5.14.2")
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<JavaExec>("runServer") {
    group = "application"
    description = "Runs the chat server"
    mainClass.set("chat.server.ChatServer")
    classpath = sourceSets["main"].runtimeClasspath
    args = listOf("5000")
}
