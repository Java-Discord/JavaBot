plugins {
    java
    id("com.github.johnrengelman.shadow") version "7.1.2"
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
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")

    // JDA & DIH4JDA (Interaction Framework)
    implementation("net.dv8tion:JDA:5.0.0-alpha.12") {
        exclude(module = "opus-java")
    }
    implementation("com.github.DynxstyGIT:DIH4JDA:e83702e9e9")

    implementation("com.google.code.gson:gson:2.9.0")
    implementation("org.yaml:snakeyaml:1.30")
    implementation("com.google.re2j:re2j:1.6")

    implementation("ch.qos.logback:logback-classic:1.2.11")
    implementation("com.mashape.unirest:unirest-java:1.4.9")

    // H2 Database
    implementation("com.h2database:h2:1.4.200")
    implementation("com.zaxxer:HikariCP:5.0.1")

    // Quartz scheduler
    implementation("org.quartz-scheduler:quartz:2.3.2")

    // Lombok Annotations
    compileOnly("org.projectlombok:lombok:1.18.24")
    annotationProcessor("org.projectlombok:lombok:1.18.24")
    testCompileOnly("org.projectlombok:lombok:1.18.24")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.24")

    // Sentry
    implementation("io.sentry:sentry:5.7.3")
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

checkstyle {
    toolVersion = "9.1"
    configDirectory.set(File("checkstyle"))
}