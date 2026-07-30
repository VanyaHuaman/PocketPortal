plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

application {
    applicationName = "pocketportal-connect"
    mainClass.set("dev.pocketportal.connect.MainKt")
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.websockets)
    runtimeOnly(libs.slf4j.nop)
    testImplementation(kotlin("test"))
}
