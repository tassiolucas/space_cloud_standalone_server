package com.spacecloud.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LogEntry(
    val id: String,
    val timestamp: String,
    val ip: String,
    val mac: String,
    val successful: Boolean,
    @SerialName("retry_attempt") val retryAttempt: Int = 0,
    val message: String
)

@Serializable
data class WakeLog(
    @SerialName("total_attempts") val totalAttempts: Int = 0,
    @SerialName("successful_activations") val successfulActivations: Int = 0,
    @SerialName("last_attempt_time") val lastAttemptTime: String? = null,
    @SerialName("last_successful_time") val lastSuccessfulTime: String? = null,
    val logs: List<LogEntry> = emptyList()
)
