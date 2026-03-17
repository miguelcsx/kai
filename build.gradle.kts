import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.plugins.JavaApplication
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
    kotlin("jvm") version "1.9.24" apply false
    application apply false
}

val kotlinVersion = "1.9.24"
val tomljVersion = "1.1.1"

allprojects {
    group = "kai"
    version = "0.1.0"

    repositories {
        mavenCentral()
    }
}

subprojects {
    pluginManager.apply("org.jetbrains.kotlin.jvm")

    extensions.configure<KotlinJvmProjectExtension> {
        jvmToolchain(21)
    }

    dependencies {
        "implementation"(kotlin("stdlib", kotlinVersion))
        "testImplementation"(kotlin("test", kotlinVersion))
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions.jvmTarget = "21"
        kotlinOptions.freeCompilerArgs += "-Xjsr305=strict"
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        testLogging.exceptionFormat = TestExceptionFormat.FULL
    }
}

project(":core:kai-domain")

project(":core:kai-plugin-api") {
    dependencies {
        "implementation"(project(":core:kai-domain"))
    }
}

project(":storage:kai-storage-api") {
    dependencies {
        "implementation"(project(":core:kai-domain"))
    }
}

project(":core:kai-config") {
    dependencies {
        "implementation"(project(":core:kai-domain"))
        "implementation"("org.tomlj:tomlj:$tomljVersion")
    }
}

project(":core:kai-campaign-engine") {
    dependencies {
        "implementation"(project(":core:kai-domain"))
        "implementation"(project(":core:kai-plugin-api"))
        "implementation"(project(":storage:kai-storage-api"))
    }
}

project(":storage:kai-storage-fs") {
    dependencies {
        "implementation"(project(":core:kai-domain"))
        "implementation"(project(":storage:kai-storage-api"))
    }
}

listOf(
    ":plugins:strategy:kai-strategy-random",
    ":plugins:strategy:kai-strategy-mutation",
    ":plugins:executor:kai-executor-cli",
    ":plugins:oracle:kai-oracle-crash",
    ":plugins:oracle:kai-oracle-differential",
    ":plugins:reducer:kai-reducer-delta",
    ":plugins:scheduler:kai-scheduler-roundrobin",
    ":test-support:kai-test-fixtures",
).forEach { path ->
    project(path) {
        dependencies {
            "implementation"(project(":core:kai-domain"))
            "implementation"(project(":core:kai-plugin-api"))
        }
    }
}

project(":app:kai-cli") {
    pluginManager.apply("application")

    dependencies {
        "implementation"(project(":core:kai-domain"))
        "implementation"(project(":core:kai-plugin-api"))
        "implementation"(project(":core:kai-campaign-engine"))
        "implementation"(project(":core:kai-config"))
        "implementation"(project(":storage:kai-storage-api"))
        "implementation"(project(":storage:kai-storage-fs"))
        "implementation"(project(":plugins:strategy:kai-strategy-random"))
        "implementation"(project(":plugins:strategy:kai-strategy-mutation"))
        "implementation"(project(":plugins:executor:kai-executor-cli"))
        "implementation"(project(":plugins:oracle:kai-oracle-crash"))
        "implementation"(project(":plugins:oracle:kai-oracle-differential"))
        "implementation"(project(":plugins:reducer:kai-reducer-delta"))
        "implementation"(project(":plugins:scheduler:kai-scheduler-roundrobin"))
    }

    extensions.configure<JavaApplication> {
        mainClass.set("kai.cli.MainKt")
    }

    tasks.withType<Jar>().configureEach {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
}
