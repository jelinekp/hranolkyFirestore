package eu.jelinek.hranolky.model

data class AppUpdateInfo(
    val versionCode: Int,
    val version: String,
    val downloadUrl: String?,
    val releaseNotes: String
)

