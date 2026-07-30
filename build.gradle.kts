plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

allprojects {
    group = "dev.pocketportal"
    version = "0.1.0-SNAPSHOT"
}

subprojects {
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}
