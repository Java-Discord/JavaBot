import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.*

plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("org.springframework.boot") version "3.5.5"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.graalvm.buildtools.native") version "0.11.0"
    checkstyle
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

group = "net.discordjug"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven(url = "https://m2.dv8tion.net/releases")
    maven(url = "https://jitpack.io")
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
    compileOnly("org.jetbrains:annotations:26.0.2")

    // DIH4JDA (Command Framework) & JDA
    implementation("com.github.jasonlessenich:DIH4JDA:1.7.0")
    //implementation("xyz.dynxsty:dih4jda:1.7.0")
    implementation("net.dv8tion:JDA:6.2.0") {
        exclude(module = "opus-java")
    }

    // Caffeine (Caching Library)
    implementation("com.github.ben-manes.caffeine:caffeine:3.2.2")

    implementation("com.google.code.gson:gson:2.13.1")
    implementation("org.yaml:snakeyaml:2.4")
    implementation("com.google.re2j:re2j:1.8")
    implementation("commons-validator:commons-validator:1.10.0")

    implementation("com.mashape.unirest:unirest-java:1.4.9")

    // H2 Database
    implementation("com.h2database:h2:2.3.232")
    implementation("com.zaxxer:HikariCP")

    // Webhooks
    implementation("com.github.DynxstyGIT:discord-webhooks:74301a46a0")

    // Lombok Annotations
    compileOnly("org.projectlombok:lombok:1.18.38")
    annotationProcessor("org.projectlombok:lombok:1.18.38")
    testCompileOnly("org.projectlombok:lombok:1.18.38")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.38")

    // Sentry
    implementation("io.sentry:sentry:8.20.0")

    // Spring
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    
    //required for registering native hints
    implementation("org.jetbrains.kotlin:kotlin-reflect")
}

configurations {
    all {
        exclude(group = "commons-logging", module = "commons-logging")
    }
}

tasks.withType<Jar> {
    manifest {
        attributes["Manifest-Version"] = "1.0"
        attributes["Main-Class"] = "net.discordjug.javabot.Bot"
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

tasks.withType<Checkstyle>() {
    exclude("**/generated/**")
}

tasks.checkstyleAot {
	isEnabled = false
}
tasks.processTestAot {
	isEnabled = false
}

graalvmNative {
	binaries {
		named("main") {
			if (hasProperty("prod")) {
				buildArgs.add("-O3")
			} else {
				quickBuild.set(true)
			}
		}
	}
}
