package dev.kauri.host

import dev.kauri.api.DesktopService
import dev.kauri.framework.WebViewWindow
import dev.kauri.framework.openEmbeddedResource
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.*
import kotlinx.io.readString
import kotlinx.rpc.krpc.ktor.server.*
import kotlinx.rpc.krpc.serialization.json.json
import kotlinx.serialization.json.*
import kotlin.concurrent.atomics.*
import kotlin.io.encoding.Base64
import kotlin.uuid.Uuid

internal expect fun appDataDirectory(): String
internal expect fun newTestDirectory(): String
internal expect fun makeAppDirectory(path: String)
internal expect fun removeAppDirectory(path: String)
internal expect fun writeAppBytes(path: String, bytes: ByteArray)

internal fun safeResourcePath(path: String): Boolean =
    path.isNotEmpty() && path.split('/').all {
        it.matches(Regex("[A-Za-z0-9_@.-]+")) && it != "." && it != ".."
    }

@OptIn(ExperimentalAtomicApi::class)
fun main(args: Array<String>) {
    if ("--licenses" in args) {
        checkNotNull(openEmbeddedResource("THIRD_PARTY_NOTICES.txt")).use { println(it.readString()) }
        return
    }
    fun option(name: String) = args.indexOf(name).takeIf { it >= 0 }?.let {
        require(it + 1 < args.size) { "Missing argument for $name" }
        args[it + 1]
    }
    val smoke = "--smoke" in args
    val startup = "--startup-check" in args
    val serve = "--serve" in args
    require(listOf(smoke, startup, serve).count { it } <= 1) { "Choose one test mode" }
    val resultDirectory = option("--result-dir")
    val urlFile = option("--url-file")
    require(!smoke || resultDirectory != null) { "Smoke needs a result directory" }
    require(!serve || urlFile != null) { "Server check needs a private URL file" }
    val temporary = smoke || startup || serve
    val directory = when {
        serve -> "${checkNotNull(urlFile).substringBeforeLast('/', ".")}/native-data"
        temporary -> newTestDirectory()
        else -> appDataDirectory()
    }
    val service: DesktopService = NativeService(directory)
    val capability = Uuid.random().toHexString() + Uuid.random().toHexString()
    val servingOrigin = AtomicReference("")
    val window = AtomicReference<WebViewWindow?>(null)
    val completed = AtomicBoolean(false)
    var timeout: Job? = null
    val background = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val server = embeddedServer(CIO, host = "127.0.0.1", port = 0) {
        install(Krpc) { serialization { json() } }
        intercept(ApplicationCallPipeline.Plugins) {
            val origin = servingOrigin.load()
            val requestOrigin = call.request.headers[HttpHeaders.Origin]
            val path = call.request.path()
            if (call.request.headers[HttpHeaders.Host] != origin.removePrefix("http://") ||
                (requestOrigin != null && requestOrigin != origin) ||
                (path.startsWith("/rpc/") && (path != "/rpc/$capability" || requestOrigin != origin)) ||
                (path.startsWith("/test/") && (requestOrigin != origin ||
                    call.request.headers["X-Kauri-Test-Capability"] != capability))
            ) {
                call.respond(HttpStatusCode.Forbidden)
                finish()
                return@intercept
            }
            call.response.header(HttpHeaders.CacheControl, "no-store")
            call.response.header("X-Content-Type-Options", "nosniff")
            call.response.header("Referrer-Policy", "no-referrer")
        }
        routing {
            rpc("/rpc/$capability") { registerService<DesktopService> { service } }
            get("/health") { call.respondText("Kauri Native") }
            if (smoke || startup) {
                post("/test/complete") {
                    try {
                        if (smoke) {
                            val values = Json.parseToJsonElement(call.receiveText()).jsonArray
                            val status = values[0].jsonPrimitive.content
                            val image = values[1].jsonPrimitive.content
                            require(status.startsWith("PASS:") && image.startsWith("data:image/png;base64,"))
                            makeAppDirectory(checkNotNull(resultDirectory))
                            writeAppBytes("$resultDirectory/window.png", Base64.decode(image.substringAfter(',')))
                            writeAppBytes("$resultDirectory/result.txt", status.encodeToByteArray())
                            println("KAURI_SMOKE $status")
                        }
                        completed.store(true)
                        call.respondText("ok")
                    } finally {
                        window.load()?.requestClose()
                    }
                }
            }
            get("/{path...}") {
                val path = call.parameters.getAll("path")?.joinToString("/").orEmpty().ifEmpty { "index.html" }
                if (!safeResourcePath(path)) {
                    call.respond(HttpStatusCode.NotFound)
                    return@get
                }
                val source = openEmbeddedResource(path)
                if (source == null) call.respond(HttpStatusCode.NotFound)
                else {
                    val type = when (path.substringAfterLast('.')) {
                        "html" -> ContentType.Text.Html
                        "js", "mjs" -> ContentType.Application.JavaScript
                        "wasm" -> ContentType("application", "wasm")
                        "css" -> ContentType.Text.CSS
                        "json", "map" -> ContentType.Application.Json
                        "ttf" -> ContentType("font", "ttf")
                        "woff2" -> ContentType("font", "woff2")
                        else -> ContentType.Application.OctetStream
                    }
                    source.use { call.respondSource(it, type) }
                }
            }
        }
    }
    try {
        makeAppDirectory(directory)
        server.start(wait = false)
        val origin = runBlocking { "http://127.0.0.1:${server.engine.resolvedConnectors().first().port}" }
        servingOrigin.store(origin)
        if (serve) {
            writeAppBytes(checkNotNull(urlFile), "$origin/#$capability".encodeToByteArray())
            runBlocking { awaitCancellation() }
        }
        val query = if (smoke) "?smoke=1" else if (startup) "?startup=1" else ""
        val view = WebViewWindow(
            url = "$origin/$query#$capability",
            title = "Kauri — Kotlin, all the way down",
        )
        window.store(view)
        if (temporary) timeout = background.launch {
            delay(if (smoke) 120_000 else 30_000)
            view.requestClose()
        }
        view.run()
        check(!temporary || completed.load()) { "UI test did not complete" }
    } finally {
        timeout?.cancel()
        background.cancel()
        try { server.stop(100, 2_000) }
        finally { if (temporary) removeAppDirectory(directory) }
    }
    if (startup) println("KAURI_STARTUP PASS: non-smoke app path; isolated browser/notes; caller closed window and server")
    if (smoke) println("KAURI_CLEANUP PASS: window returned, caller stopped server and removed test data")
}
