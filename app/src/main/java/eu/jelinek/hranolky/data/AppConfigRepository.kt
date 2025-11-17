package eu.jelinek.hranolky.data

import eu.jelinek.hranolky.model.AppUpdateInfo

interface AppConfigRepository {
    suspend fun getLatestAppVersion(): AppUpdateInfo?
}

