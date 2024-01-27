import org.jetbrains.kotlin.gradle.tasks.*

plugins {
    id("application")
    id("org.jetbrains.kotlin.jvm") version "1.9.20"
    id("org.jetbrains.kotlin.plugin.allopen") version "1.9.20"
    id("org.springframework.boot") version "3.1.5"
    id("it.nicolasfarabegoli.conventional-commits") version "3.1.3"
}

repositories {
    mavenCentral()
}


sourceSets {
    val intTest by creating {
        java.srcDir("src/intTest/kotlin")
        compileClasspath += sourceSets["main"].output + sourceSets["test"].compileClasspath
        runtimeClasspath += sourceSets["main"].output + sourceSets["test"].compileClasspath
    }
}

configurations {
    val intTestImplementation by getting {
        extendsFrom(configurations["implementation"])
    }
    val intTestRuntimeOnly by getting {
        extendsFrom(configurations["runtimeOnly"])
    }
}


val integrationTestTask = tasks.register<Test>("integrationTest") {
    description = "Runs integration tests."
    group = "verification"

    testClassesDirs = sourceSets["intTest"].output.classesDirs
    classpath = sourceSets["intTest"].runtimeClasspath
    shouldRunAfter("test")
    useJUnitPlatform()

    testLogging {
        events("passed")
    }
}

tasks.check {
    dependsOn(integrationTestTask)
}


kotlin.target.compilations.getByName("intTest") {
    associateWith(target.compilations.getByName("test"))
}

dependencies {
    // dependency management
    implementation(platform("org.springframework.boot:spring-boot-dependencies:_"))
    testImplementation(platform("org.jetbrains.kotlin:kotlin-bom:_"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")

    implementation("io.vavr:vavr:_")
    implementation("io.vavr:vavr-jackson:_")


    testImplementation("org.jetbrains.kotlin:kotlin-reflect")
    testImplementation("org.junit.jupiter:junit-jupiter:_")
    testImplementation("org.assertj:assertj-core:_")

    "intTestImplementation"("org.springframework.boot:spring-boot-starter-test")
    "intTestImplementation"("org.testcontainers:testcontainers:_")
    "intTestImplementation"("org.testcontainers:mongodb:_")
    "intTestImplementation"("org.springframework.boot:spring-boot-testcontainers")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(20)
    }
}

kotlin {
    jvmToolchain(20)
}

application {
    mainClass = "debit.card.DebitCardApp"
}


tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("--enable-preview")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    jvmArgs("--enable-preview")
}

tasks.withType<JavaExec>().configureEach {
    jvmArgs("--enable-preview")
}

conventionalCommits {
    warningIfNoGitRoot = true

    successMessage = "Commit message meets Conventional Commit standards..."

    failureMessage = "The commit message does not meet the Conventional Commit standard"
}
