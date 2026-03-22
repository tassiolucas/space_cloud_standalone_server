package com.spacecloud.database

import org.jetbrains.exposed.sql.Table

object ConfigTable : Table("config") {
    val id = integer("id").autoIncrement()
    val ip = varchar("ip", 50)
    val mac = varchar("mac", 50)
    val hour = varchar("hour", 2)
    val minute = varchar("minute", 2)
    val enabled = bool("enabled")
    val pingTimeoutSeconds = integer("ping_timeout_seconds").default(120)
    val pingIntervalSeconds = integer("ping_interval_seconds").default(5)
    val maxLogEntries = integer("max_log_entries").default(50)
    override val primaryKey = PrimaryKey(id)
}

object WakeLogsTable : Table("wake_logs") {
    val id = varchar("id", 100)
    val timestamp = varchar("timestamp", 50)
    val ip = varchar("ip", 50)
    val mac = varchar("mac", 50)
    val successful = bool("successful")
    val retryAttempt = integer("retry_attempt")
    val message = text("message")
    override val primaryKey = PrimaryKey(id)
}
