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

kotlin {
    jvmToolchain(17)
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
