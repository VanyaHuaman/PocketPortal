plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

application {
    mainClass.set("dev.pocketportal.app.MainKt")
}

sourceSets {
    main {
        resources.srcDir(rootProject.layout.projectDirectory.dir("config"))
    }
}

dependencies {
    implementation(project(":application"))
    implementation(project(":infrastructure"))
    implementation(project(":web"))
    implementation(libs.ktor.server.cio)
    implementation(libs.logback.classic)
    testImplementation(kotlin("test"))
}
