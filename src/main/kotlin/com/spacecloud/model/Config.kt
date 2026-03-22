package com.spacecloud.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Config(
    val ip: String = "192.168.1.100",
    val mac: String = "00:11:22:33:44:55",
    val hour: String = "07",
    val minute: String = "00",
    val enabled: Boolean = false,
    @SerialName("ping_timeout_seconds") val pingTimeoutSeconds: Int = 120,
    @SerialName("ping_interval_seconds") val pingIntervalSeconds: Int = 5,
    @SerialName("max_log_entries") val maxLogEntries: Int = 50
)
