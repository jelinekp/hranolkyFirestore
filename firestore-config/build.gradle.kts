plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

dependencies {
    implementation(libs.google.cloud.firestore)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(kotlin("test"))
}

application {
    mainClass.set("FirestoreConfigMainKt")
}

// Task to update app config
tasks.register<JavaExec>("updateConfig") {
    group = "application"
    description = "Update AppConfig in Firestore with new version info"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("FirestoreConfigMainKt")
    args = buildList {
        project.findProperty("version")?.let {
            add("--version")
            add(it.toString())
        }
        project.findProperty("versionCode")?.let {
            add("--version-code")
            add(it.toString())
        }
        project.findProperty("releaseNotes")?.let {
            add("--release-notes")
            add(it.toString())
        }
        project.findProperty("credentials")?.let {
            add("--credentials")
            add(it.toString())
        }
        if (project.hasProperty("dryRun")) {
            add("--dry-run")
        }
    }
}

kotlin {
    jvmToolchain(17)
}

// Create a fat JAR for easy running
tasks.register<Jar>("fatJar") {
    dependsOn(tasks.named("jar"))
    archiveClassifier.set("all")
    from(sourceSets.main.get().output)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest { attributes["Main-Class"] = "FirestoreConfigMainKt" }
    val runtimeClasspath = configurations.runtimeClasspath.get()
    from(runtimeClasspath.filter { it.name.endsWith(".jar") }.map { zipTree(it) })
}

