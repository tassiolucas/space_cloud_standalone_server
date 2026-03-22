package com.spacecloud.database

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.io.File
import java.sql.DriverManager

object DatabaseFactory {

    private val logger = LoggerFactory.getLogger(DatabaseFactory::class.java)

    fun init() {
        val dbDir = File("data")
        dbDir.mkdirs()

        val jdbcUrl = "jdbc:sqlite:${dbDir.absolutePath}/space_cloud.db"

        // PRAGMAs de configuração precisam rodar fora de transação
        DriverManager.getConnection(jdbcUrl).use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute("PRAGMA journal_mode=WAL;")
                stmt.execute("PRAGMA synchronous=NORMAL;")
                stmt.execute("PRAGMA foreign_keys=ON;")
            }
        }

        Database.connect(jdbcUrl, driver = "org.sqlite.JDBC")

        transaction {
            SchemaUtils.createMissingTablesAndColumns(ConfigTable, WakeLogsTable)

            if (ConfigTable.selectAll().count() == 0L) {
                ConfigTable.insert {
                    it[ip] = "192.168.1.100"
                    it[mac] = "00:11:22:33:44:55"
                    it[hour] = "07"
                    it[minute] = "00"
                    it[enabled] = false
                    it[pingTimeoutSeconds] = 120
                    it[pingIntervalSeconds] = 5
                    it[maxLogEntries] = 50
                }
                logger.info("Configuração padrão inserida no banco de dados.")
            }
        }

        logger.info("Banco de dados SQLite inicializado em ${dbDir.absolutePath}/space_cloud.db")
    }
}
