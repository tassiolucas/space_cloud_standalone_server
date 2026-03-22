package com.spacecloud.service

import com.spacecloud.model.LogEntry
import com.spacecloud.service.ConfigService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object WakeOnLanService {

    private val logger = LoggerFactory.getLogger(WakeOnLanService::class.java)
    private val timestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    /**
     * Envia o magic packet WOL e verifica se o dispositivo acordou.
     * Deve ser chamado em um contexto de coroutine (suspend).
     */
    suspend fun sendAndVerify(ip: String, mac: String, retryAttempt: Int = 0) {
        val timestamp = LocalDateTime.now().format(timestampFormatter)
        val id = timestamp.replace(":", "-").replace(" ", "_")

        logger.info("Enviando pacote WOL para $ip ($mac)...")

        val packetSent = sendMagicPacket(ip, mac)

        if (!packetSent) {
            val entry = LogEntry(
                id = id,
                timestamp = timestamp,
                ip = ip,
                mac = mac,
                successful = false,
                retryAttempt = retryAttempt,
                message = "Falha ao enviar o pacote WOL"
            )
            LogService.addEntry(entry)
            return
        }

        logger.info("Pacote WOL enviado. Aguardando dispositivo acordar...")

        val config = ConfigService.load()
        var isOnline = false
        var totalWaitSeconds = 0
        val pingIntervalSeconds = config.pingIntervalSeconds
        val maxWaitSeconds = config.pingTimeoutSeconds

        while (totalWaitSeconds < maxWaitSeconds) {
            delay(pingIntervalSeconds * 1000L)
            totalWaitSeconds += pingIntervalSeconds

            if (ping(ip)) {
                isOnline = true
                logger.info("Dispositivo respondeu após ${totalWaitSeconds}s!")
                break
            }
        }

        val finalTimestamp = LocalDateTime.now().format(timestampFormatter)
        val entry = LogEntry(
            id = id,
            timestamp = finalTimestamp,
            ip = ip,
            mac = mac,
            successful = isOnline,
            retryAttempt = retryAttempt,
            message = if (isOnline)
                "Pacote WOL enviado - ✓ - Dispositivo acordou em ${totalWaitSeconds}s"
            else
                "Pacote WOL enviado - ✗ - Dispositivo não respondeu após ${maxWaitSeconds}s"
        )
        LogService.addEntry(entry)
        logger.info("Resultado WOL: ${entry.message}")
    }

    private suspend fun sendMagicPacket(ip: String, mac: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val macBytes = mac.replace(":", "").replace("-", "")
                .chunked(2)
                .map { it.toInt(16).toByte() }
                .toByteArray()

            // Magic packet: 6 bytes de 0xFF + MAC repetido 16 vezes
            val magicPacket = ByteArray(6) { 0xFF.toByte() } + (1..16).flatMap { macBytes.toList() }.toByteArray()

            DatagramSocket().use { socket ->
                socket.setBroadcast(true)

                // Envia para o IP específico
                socket.send(DatagramPacket(magicPacket, magicPacket.size, InetAddress.getByName(ip), 9))

                // Envia para broadcast geral
                socket.send(DatagramPacket(magicPacket, magicPacket.size, InetAddress.getByName("255.255.255.255"), 9))

                // Envia para broadcast da sub-rede
                val ipParts = ip.split(".")
                if (ipParts.size == 4) {
                    val subnetBroadcast = "${ipParts[0]}.${ipParts[1]}.${ipParts[2]}.255"
                    socket.send(DatagramPacket(magicPacket, magicPacket.size, InetAddress.getByName(subnetBroadcast), 9))
                }

                // Portas alternativas comuns de WOL
                for (port in listOf(7, 7812)) {
                    socket.send(DatagramPacket(magicPacket, magicPacket.size, InetAddress.getByName("255.255.255.255"), port))
                }
            }
            true
        } catch (e: Exception) {
            logger.error("Erro ao enviar magic packet: ${e.message}")
            false
        }
    }

    private suspend fun ping(host: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val isWindows = System.getProperty("os.name").lowercase().contains("windows")
            val command = if (isWindows)
                listOf("ping", "-n", "1", "-w", "1000", host)
            else
                listOf("ping", "-c", "1", "-W", "1", host)

            val process = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }
}
