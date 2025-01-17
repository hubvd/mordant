plugins {
    kotlin("jvm")
    alias(libs.plugins.graalvm.nativeimage)
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.test {
    useJUnitPlatform()
}

dependencies {
    implementation(project(":mordant"))
    testImplementation(kotlin("test"))
}

graalvmNative {
    binaries {
        named("test") {
            quickBuild.set(true)
            buildArgs(
                // https://github.com/oracle/graal/issues/6957
                "--initialize-at-build-time=kotlin.annotation.AnnotationTarget,kotlin.annotation.AnnotationRetention",
            )
        }
    }
}
