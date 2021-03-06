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

    // DIH4JDA (Interaction Framework) (includes JDA (jda5.0.0-alpha.17))
    implementation("com.github.DenuxPlays:DIH4JDA:7ac2c9c77c")
    implementation("org.reflections:reflections:0.10.2")

    implementation("com.google.code.gson:gson:2.9.0")
    implementation("org.yaml:snakeyaml:1.30")
    implementation("com.google.re2j:re2j:1.6")
    implementation("commons-validator:commons-validator:1.7")

    implementation("ch.qos.logback:logback-classic:1.2.11")
    implementation("com.mashape.unirest:unirest-java:1.4.9")

    // H2 Database
    implementation("com.h2database:h2:2.1.212")
    implementation("com.zaxxer:HikariCP:5.0.1")

    // Quartz Scheduler
    implementation("org.quartz-scheduler:quartz:2.3.2")
    
    // Webhooks
    implementation("com.github.DynxstyGIT:discord-webhooks:74301a46a0")

    // Lombok Annotations
    compileOnly("org.projectlombok:lombok:1.18.24")
    annotationProcessor("org.projectlombok:lombok:1.18.24")
    testCompileOnly("org.projectlombok:lombok:1.18.24")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.24")

    // Sentry
    implementation("io.sentry:sentry:6.3.0")
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