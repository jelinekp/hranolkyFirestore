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
    mainClass.set("FirestoreDumpMainKt")
}

// Task to run the upload/migration script
tasks.register<JavaExec>("upload") {
    group = "application"
    description = "Upload dump.json to Firestore with migration"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("FirestoreUploadMainKt")
    args = buildList {
        project.findProperty("input")?.let {
            add("--input")
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
    jvmToolchain(21)
}

// Create a fat JAR for easy running: build/libs/firestore-dump-all.jar
tasks.register<Jar>("fatJar") {
    dependsOn(tasks.named("jar"))
    archiveClassifier.set("all")
    from(sourceSets.main.get().output)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest { attributes["Main-Class"] = "FirestoreDumpMainKt" }
    val runtimeClasspath = configurations.runtimeClasspath.get()
    from(runtimeClasspath.filter { it.name.endsWith(".jar") }.map { zipTree(it) })
}
