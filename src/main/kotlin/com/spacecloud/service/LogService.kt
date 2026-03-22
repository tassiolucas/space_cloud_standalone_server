package com.spacecloud.service

import com.spacecloud.database.WakeLogsTable
import com.spacecloud.service.ConfigService
import com.spacecloud.model.LogEntry
import com.spacecloud.model.WakeLog
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

object LogService {

    fun load(): WakeLog = transaction {
        val maxEntries = ConfigService.load().maxLogEntries
        val entries = WakeLogsTable
            .selectAll()
            .orderBy(WakeLogsTable.timestamp, SortOrder.DESC)
            .limit(maxEntries)
            .map {
                LogEntry(
                    id = it[WakeLogsTable.id],
                    timestamp = it[WakeLogsTable.timestamp],
                    ip = it[WakeLogsTable.ip],
                    mac = it[WakeLogsTable.mac],
                    successful = it[WakeLogsTable.successful],
                    retryAttempt = it[WakeLogsTable.retryAttempt],
                    message = it[WakeLogsTable.message]
                )
            }

        val total = WakeLogsTable.selectAll().count().toInt()
        val successes = entries.count { it.successful }
        val lastAttempt = entries.firstOrNull()?.timestamp
        val lastSuccess = entries.firstOrNull { it.successful }?.timestamp

        WakeLog(
            totalAttempts = total,
            successfulActivations = successes,
            lastAttemptTime = lastAttempt,
            lastSuccessfulTime = lastSuccess,
            logs = entries
        )
    }

    fun addEntry(entry: LogEntry): Unit = transaction {
        WakeLogsTable.insert {
            it[id] = entry.id
            it[timestamp] = entry.timestamp
            it[ip] = entry.ip
            it[mac] = entry.mac
            it[successful] = entry.successful
            it[retryAttempt] = entry.retryAttempt
            it[message] = entry.message
        }

        val maxEntries = ConfigService.load().maxLogEntries
        val total = WakeLogsTable.selectAll().count()
        if (total > maxEntries) {
            val oldest = WakeLogsTable
                .selectAll()
                .orderBy(WakeLogsTable.timestamp, SortOrder.ASC)
                .limit((total - maxEntries).toInt())
                .map { it[WakeLogsTable.id] }

            WakeLogsTable.deleteWhere { WakeLogsTable.id inList oldest }
        }
    }

    fun clear(): Unit = transaction {
        WakeLogsTable.deleteAll()
    }
}
