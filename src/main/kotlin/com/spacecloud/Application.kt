package com.spacecloud

import com.spacecloud.database.DatabaseFactory
import com.spacecloud.routes.apiRoutes
import com.spacecloud.service.SchedulerService
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.http.content.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    DatabaseFactory.init()
    val scheduler = SchedulerService()

    install(DefaultHeaders)

    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("error" to (cause.message ?: "Erro interno"))
            )
        }
    }

    routing {
        // Serve a página principal
        get("/") {
            val html = Application::class.java.classLoader
                .getResourceAsStream("static/index.html")
                ?.readBytes()
                ?.toString(Charsets.UTF_8)
                ?: "<h1>index.html não encontrado</h1>"
            call.respondText(html, ContentType.Text.Html)
        }

        // Serve arquivos estáticos (CSS, JS)
        staticResources("/static", "static")

        // Rotas da API
        apiRoutes()
    }

    scheduler.start()
    environment.monitor.subscribe(ApplicationStopped) {
        scheduler.stop()
    }
}
