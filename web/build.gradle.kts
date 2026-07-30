plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

val frontendDirectory = rootProject.layout.projectDirectory.dir("frontend")
val frontendDistribution = frontendDirectory.dir("dist")
val skipFrontendBuild = providers.gradleProperty("skipFrontendBuild")
    .map(String::toBoolean)
    .orElse(false)

val installFrontendDependencies by tasks.registering(Exec::class) {
    description = "Installs the pinned frontend dependencies."
    workingDir(frontendDirectory)
    commandLine("npm", "ci")
    inputs.files(
        frontendDirectory.file("package.json"),
        frontendDirectory.file("package-lock.json"),
    )
    outputs.dir(frontendDirectory.dir("node_modules"))
    onlyIf { !skipFrontendBuild.get() }
}

val testFrontend by tasks.registering(Exec::class) {
    description = "Runs the frontend unit and component tests."
    dependsOn(installFrontendDependencies)
    workingDir(frontendDirectory)
    commandLine("npm", "run", "test")
    inputs.dir(frontendDirectory.dir("src"))
    inputs.file(frontendDirectory.file("vite.config.ts"))
    onlyIf { !skipFrontendBuild.get() }
}

val buildFrontend by tasks.registering(Exec::class) {
    description = "Builds the browser dashboard."
    dependsOn(installFrontendDependencies)
    workingDir(frontendDirectory)
    commandLine("npm", "run", "build")
    inputs.dir(frontendDirectory.dir("src"))
    inputs.files(
        frontendDirectory.file("index.html"),
        frontendDirectory.file("tsconfig.json"),
        frontendDirectory.file("tsconfig.app.json"),
        frontendDirectory.file("tsconfig.node.json"),
        frontendDirectory.file("vite.config.ts"),
    )
    outputs.dir(frontendDistribution)
    onlyIf { !skipFrontendBuild.get() }
}

tasks.processResources {
    dependsOn(buildFrontend)
    from(frontendDistribution) {
        into("frontend")
    }
}

tasks.check {
    dependsOn(testFrontend)
}

dependencies {
    implementation(project(":application"))
    implementation(project(":domain"))
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.json)

    testImplementation(kotlin("test"))
    testImplementation(libs.ktor.server.test.host)
}
