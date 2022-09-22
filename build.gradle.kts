import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.*

plugins {
    java
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("org.springframework.boot") version "2.7.3"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    checkstyle
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

group = "net.javadiscord"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven(url = "https://m2.dv8tion.net/releases")
    maven(url = "https://jitpack.io")
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.0")

    // DIH4JDA (Interaction Framework) & JDA
    implementation("com.github.DynxstyGIT:DIH4JDA:f564af77e9")
    implementation("net.dv8tion:JDA:5.0.0-alpha.17") {
        exclude(module = "opus-java")
    }

    // Caffeine (Caching Library)
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.1")

    implementation("com.google.code.gson:gson:2.9.0")
    implementation("org.yaml:snakeyaml:1.30")
    implementation("com.google.re2j:re2j:1.6")
    implementation("commons-validator:commons-validator:1.7")

    implementation("ch.qos.logback:logback-classic:1.2.11")
    implementation("com.mashape.unirest:unirest-java:1.4.9")

    // H2 Database
    implementation("com.h2database:h2:2.1.212")
    implementation("com.zaxxer:HikariCP:5.0.1")

    // Webhooks
    implementation("com.github.DynxstyGIT:discord-webhooks:74301a46a0")

    // Lombok Annotations
    compileOnly("org.projectlombok:lombok:1.18.24")
    annotationProcessor("org.projectlombok:lombok:1.18.24")
    testCompileOnly("org.projectlombok:lombok:1.18.24")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.24")

    // Sentry
    implementation("io.sentry:sentry:6.3.0")

    // Spring
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
}

tasks.withType<Jar> {
    manifest {
        attributes["Manifest-Version"] = "1.0"
        attributes["Main-Class"] = "net.javadiscord.javabot.Bot"
    }
}

tasks.withType<JavaCompile>{ options.encoding = "UTF-8" }
tasks.withType<JavaCompile>().configureEach {
    options.forkOptions.jvmArgs = listOf("--add-opens", "jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED")
}
tasks.withType<Test>{ useJUnitPlatform() }

tasks.withType<ShadowJar> {
    isZip64 = true
    // Required for Spring
    mergeServiceFiles()
    append("META-INF/spring.handlers")
    append("META-INF/spring.schemas")
    append("META-INF/spring.tooling")
    transform(PropertiesFileTransformer().apply {
        paths = listOf("META-INF/spring.factories")
        mergeStrategy = "append"
    })
}

checkstyle {
    toolVersion = "9.1"
    configDirectory.set(File("checkstyle"))
}