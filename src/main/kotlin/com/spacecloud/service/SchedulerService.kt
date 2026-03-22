package com.spacecloud.service

import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class SchedulerService {

    private val logger = LoggerFactory.getLogger(SchedulerService::class.java)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var schedulerJob: Job? = null

    fun start() {
        schedulerJob = scope.launch {
            logger.info("Scheduler iniciado.")
            while (isActive) {
                try {
                    checkAndTrigger()
                } catch (e: Exception) {
                    logger.error("Erro no scheduler: ${e.message}")
                }
                // Aguarda até o próximo minuto exato
                val now = LocalDateTime.now()
                val secondsUntilNextMinute = 60 - now.second
                delay(secondsUntilNextMinute * 1000L)
            }
        }
    }

    fun stop() {
        logger.info("Parando scheduler...")
        scope.cancel()
    }

    private suspend fun checkAndTrigger() {
        val config = ConfigService.load()
        if (!config.enabled) return

        val now = LocalDateTime.now()
        val configHour = config.hour.toIntOrNull() ?: return
        val configMinute = config.minute.toIntOrNull() ?: return

        if (now.hour == configHour && now.minute == configMinute) {
            logger.info("Horário agendado atingido (${config.hour}:${config.minute}). Disparando WOL...")
            WakeOnLanService.sendAndVerify(config.ip, config.mac)
        }
    }
}
