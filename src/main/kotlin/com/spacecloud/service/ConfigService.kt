package com.spacecloud.service

import com.spacecloud.database.ConfigTable
import com.spacecloud.model.Config
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

object ConfigService {

    fun load(): Config = transaction {
        ConfigTable.selectAll().limit(1).single().let {
            Config(
                ip = it[ConfigTable.ip],
                mac = it[ConfigTable.mac],
                hour = it[ConfigTable.hour],
                minute = it[ConfigTable.minute],
                enabled = it[ConfigTable.enabled],
                pingTimeoutSeconds = it[ConfigTable.pingTimeoutSeconds],
                pingIntervalSeconds = it[ConfigTable.pingIntervalSeconds],
                maxLogEntries = it[ConfigTable.maxLogEntries]
            )
        }
    }

    fun save(config: Config): Unit = transaction {
        ConfigTable.update({ ConfigTable.id eq 1 }) {
            it[ip] = config.ip
            it[mac] = config.mac
            it[hour] = config.hour
            it[minute] = config.minute
            it[enabled] = config.enabled
            it[pingTimeoutSeconds] = config.pingTimeoutSeconds
            it[pingIntervalSeconds] = config.pingIntervalSeconds
            it[maxLogEntries] = config.maxLogEntries
        }
    }
}
