package com.spacecloud.routes

import com.spacecloud.model.Config
import com.spacecloud.service.ConfigService
import com.spacecloud.service.LogService
import com.spacecloud.service.WakeOnLanService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("ApiRoutes")
private val startTimeMillis = System.currentTimeMillis()

fun Route.apiRoutes() {

    route("/api") {

        get("/uptime") {
            call.respond(buildJsonObject {
                put("start_time_millis", startTimeMillis)
                put("uptime_millis", System.currentTimeMillis() - startTimeMillis)
            })
        }

        get("/config") {
            call.respond(ConfigService.load())
        }

        post("/config") {
            try {
                val body = call.receiveText()
                val jsonObj = Json.parseToJsonElement(body).jsonObject

                val updated = ConfigService.load().let { current ->
                    Config(
                        ip = jsonObj["ip"]?.jsonPrimitive?.contentOrNull ?: current.ip,
                        mac = jsonObj["mac"]?.jsonPrimitive?.contentOrNull ?: current.mac,
                        hour = jsonObj["hour"]?.jsonPrimitive?.contentOrNull ?: current.hour,
                        minute = jsonObj["minute"]?.jsonPrimitive?.contentOrNull ?: current.minute,
                        enabled = jsonObj["enabled"]?.jsonPrimitive?.booleanOrNull ?: current.enabled,
                        pingTimeoutSeconds = jsonObj["ping_timeout_seconds"]?.jsonPrimitive?.intOrNull ?: current.pingTimeoutSeconds,
                        pingIntervalSeconds = jsonObj["ping_interval_seconds"]?.jsonPrimitive?.intOrNull ?: current.pingIntervalSeconds,
                        maxLogEntries = jsonObj["max_log_entries"]?.jsonPrimitive?.intOrNull ?: current.maxLogEntries
                    )
                }
                ConfigService.save(updated)

                call.respond(buildJsonObject {
                    put("success", true)
                    put("config", Json.encodeToJsonElement(updated))
                })
            } catch (e: Exception) {
                logger.error("Erro ao atualizar config: ${e.message}")
                call.respond(HttpStatusCode.BadRequest, buildJsonObject {
                    put("success", false)
                    put("message", "Erro ao atualizar configuração: ${e.message}")
                })
            }
        }

        post("/test-wol") {
            val config = ConfigService.load()

            // Dispara o WOL em background e retorna imediatamente
            call.application.launch {
                WakeOnLanService.sendAndVerify(config.ip, config.mac)
            }

            call.respond(buildJsonObject {
                put("success", true)
                put("message", "Pacote WOL enviado! Verificando se o servidor acordou... Acompanhe nos logs.")
            })
        }

        get("/logs") {
            call.respond(LogService.load())
        }

        post("/clear-logs") {
            LogService.clear()
            call.respond(buildJsonObject {
                put("success", true)
                put("message", "Logs limpos com sucesso")
            })
        }
    }
}
