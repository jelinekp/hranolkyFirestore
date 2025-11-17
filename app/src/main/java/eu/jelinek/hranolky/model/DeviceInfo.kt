package eu.jelinek.hranolky.model

data class DeviceInfo(
    val deviceName: String? = null,
    val isInventoryCheckPermitted: Boolean = false,
    val appVersion: String? = null,
    val lastSeen: com.google.firebase.Timestamp? = null
)

