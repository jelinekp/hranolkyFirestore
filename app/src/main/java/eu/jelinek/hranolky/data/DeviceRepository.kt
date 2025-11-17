package eu.jelinek.hranolky.data

import eu.jelinek.hranolky.model.DeviceInfo

interface DeviceRepository {
    suspend fun updateDeviceInfo(deviceId: String, appVersion: String)
    suspend fun getDeviceInfo(deviceId: String): DeviceInfo?
}

