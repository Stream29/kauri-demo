@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class, kotlin.js.ExperimentalWasmJsInterop::class)

package dev.kauri.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.ComposeViewport
import dev.kauri.api.DesktopService
import dev.kauri.api.MAX_NOTE_LENGTH
import dev.kauri.api.SystemInfo
import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.rpc.krpc.ktor.client.installKrpc
import kotlinx.rpc.krpc.ktor.client.rpc
import kotlinx.rpc.krpc.serialization.json.json
import kotlinx.rpc.withService
import kotlin.time.TimeSource

private val Background = Color(0xFF101714)
private val Panel = Color(0xFF19231D)
private val Line = Color(0xFF2D4034)
private val Mint = Color(0xFFB9F5CC)
private val Muted = Color(0xFF91A99A)
private val Ink = Color(0xFFE6EEE8)
private val webViewName: String
    get() = when {
        window.navigator.userAgent.contains("Windows") -> "WebView2"
        window.navigator.userAgent.contains("Macintosh") -> "WKWebView"
        else -> "WebKitGTK"
    }

private fun captureCanvasFrame(): Unit = js(
    """queueMicrotask(() => {
        const body = document.body;
        if (!body || body.hasAttribute('data-kauri-image')) return;
        body.setAttribute('data-kauri-capture', 'running');
        let canvas = null;
        const roots = [document];
        while (roots.length && !canvas) {
            const root = roots.pop();
            canvas = root.querySelector('canvas');
            for (const element of root.querySelectorAll('*'))
                if (element.shadowRoot) roots.push(element.shadowRoot);
        }
        if (!canvas) return;
        const sample = document.createElement('canvas');
        sample.width = sample.height = 64;
        const context = sample.getContext('2d');
        context.drawImage(canvas, 0, 0, 64, 64);
        const pixels = context.getImageData(0, 0, 64, 64).data;
        const colors = new Set();
        for (let i = 0; i < pixels.length; i += 4)
            colors.add((pixels[i] << 16) | (pixels[i + 1] << 8) | pixels[i + 2]);
        body.setAttribute('data-kauri-color-count', String(colors.size));
        if (colors.size < 8) {
            body.setAttribute('data-kauri-smoke', 'FAIL: Compose canvas is blank');
            return;
        }
        body.setAttribute('data-kauri-image', canvas.toDataURL('image/png'));
        body.setAttribute('data-kauri-capture', 'captured');
    })"""
)

private fun sendSmokeMessage(status: String, image: String) {
    js(
        """(() => {
            const endpoint = '/test/complete';
            const foreign = location.origin.replace('127.0.0.1', 'localhost');
            window.open(foreign + '/blocked-popup');
            location.href = foreign + '/blocked-navigation';
            setTimeout(() => {
                fetch(endpoint, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'X-Kauri-Test-Capability': location.hash.slice(1)
                    },
                    body: JSON.stringify([status, image])
                }).catch(() => document.body.setAttribute('data-kauri-smoke', 'FAIL: test completion'));
            }, 500);
        })()"""
    )
}

private fun completeStartup(): Unit = js(
    """fetch('/test/complete', {
        method: 'POST',
        headers: {'X-Kauri-Test-Capability': location.hash.slice(1)},
        body: 'ready'
    }).catch(() => document.body.setAttribute('data-kauri-startup', 'failed'))"""
)

private class DesktopState {
    var service: DesktopService? by mutableStateOf(null)
    var info: SystemInfo? by mutableStateOf(null)
    var note by mutableStateOf("")
    var path by mutableStateOf("")
    var status by mutableStateOf("Connecting to native host…")
    var connected by mutableStateOf(false)
    var busy by mutableStateOf(false)
    var sequence by mutableIntStateOf(0)
    var uptime by mutableStateOf(0L)
    var latency by mutableStateOf("—")
    var captureRequested by mutableStateOf(false)
    val events = mutableStateListOf("Compose Wasm mounted in system WebView")

    fun log(message: String) {
        events.add(0, message)
        while (events.size > 6) events.removeAt(events.lastIndex)
    }
}

fun main() {
    document.getElementById("loading")?.remove()
    ComposeViewport(document.body!!) {
        MaterialTheme(
            colorScheme = darkColorScheme(
                primary = Mint, onPrimary = Background, background = Background,
                surface = Panel, onSurface = Ink, outline = Line, onSurfaceVariant = Muted,
            )
        ) { KauriApp() }
    }
}

@Composable
private fun KauriApp() {
    val state = remember { DesktopState() }
    val smokeMode = remember { window.location.search.contains("smoke=1") }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        val client = HttpClient(Js) { installKrpc { serialization { json() } } }
        try {
            val token = window.location.hash.removePrefix("#")
            check(token.isNotBlank()) { "Launch this UI from the Kauri executable" }
            val rpc = client.rpc("ws://${window.location.host}/rpc/$token")
            val service = rpc.withService<DesktopService>()
            state.service = service
            document.body?.setAttribute("data-kauri-phase", "initial native calls")
            val start = TimeSource.Monotonic.markNow()
            state.info = withTimeout(20_000) { service.systemInfo() }
            state.latency = "${start.elapsedNow().inWholeMilliseconds} ms"
            val note = service.readNote()
            state.note = note.text
            state.path = note.path
            state.connected = true
            state.status = "Connected · typed kRPC"
            state.log("systemInfo() → native platform APIs")
            state.log("readNote() → app-owned native file")
            if (window.location.search.contains("startup=1")) completeStartup()
            if (smokeMode) {
                runSmoke(service, client, state)
            }
            service.pulses().collect {
                state.sequence = it.sequence
                state.uptime = it.uptimeSeconds
            }
        } catch (e: TimeoutCancellationException) {
            state.connected = false
            state.status = "Native request or Compose render timed out"
            state.log(state.status)
            document.body?.setAttribute("data-kauri-smoke", "FAIL: ${state.status}")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            state.connected = false
            state.status = "Connection failed: ${e.message}"
            state.log(state.status)
            document.body?.setAttribute("data-kauri-smoke", "FAIL: ${e.message}")
        } finally {
            client.close()
        }
    }

    fun action(block: suspend (DesktopService) -> Unit) {
        val service = state.service ?: return
        scope.launch {
            state.busy = true
            try { block(service) }
            catch (e: CancellationException) { throw e }
            catch (e: Throwable) { state.status = "Request failed: ${e.message}" }
            finally { state.busy = false }
        }
    }

    Row(Modifier.fillMaxSize().background(Background).drawWithContent {
        drawContent()
        if (smokeMode) {
            document.body?.setAttribute("data-kauri-painted", "true")
            if (state.captureRequested) captureCanvasFrame()
        }
    }) {
        Column(
            Modifier.width(204.dp).fillMaxHeight().background(Color(0xFF141D17)).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text("k /", color = Mint, fontSize = 40.sp, fontWeight = FontWeight.Light)
            Text("KAURI", color = Ink, fontSize = 22.sp, letterSpacing = 4.sp, fontWeight = FontWeight.Bold)
            Text("Native roots.\nWasm canopy.", color = Muted, fontSize = 14.sp, lineHeight = 23.sp)
            HorizontalDivider(color = Line)
            Text("●  Playground", color = Mint, fontSize = 14.sp)
            Text("01   Native host\n\n02   Typed bridge\n\n03   Wasm view", color = Muted, fontSize = 12.sp)
            Spacer(Modifier.weight(1f))
            Text("EXPERIMENT 001", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp)
            Text("${state.info?.architecture ?: "Native"} · $webViewName\nOne executable", color = Ink, fontSize = 12.sp, lineHeight = 21.sp)
        }
        Column(
            Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("THE KOTLIN DESKTOP EXPERIMENT", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Hello, native world.", color = Ink, fontSize = 34.sp, fontWeight = FontWeight.SemiBold)
                }
                BadgeLabel(if (state.connected) "●  CONNECTED" else "○  CONNECTING")
            }
            Text(
                "Compose on the surface. Kotlin/Native underneath. One shared contract.",
                color = Muted, fontSize = 14.sp,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Metric("BACKEND", "Kotlin/Native", "No JVM runtime", Modifier.weight(1f))
                Metric("FRONTEND", "Compose Wasm", "System $webViewName", Modifier.weight(1f))
                Metric("BRIDGE", "kRPC", "Typed calls + Flow", Modifier.weight(1f))
            }
            SurfaceCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        SectionTitle("01", "Your machine, via native APIs")
                        Text("Native platform APIs · no JavaScript system shim", color = Muted, fontSize = 12.sp)
                    }
                    OutlinedButton(
                        enabled = state.connected && !state.busy,
                        onClick = {
                            action {
                                val start = TimeSource.Monotonic.markNow()
                                state.info = it.systemInfo()
                                state.latency = "${start.elapsedNow().inWholeMilliseconds} ms"
                                state.log("systemInfo() completed in ${state.latency}")
                            }
                        },
                    ) { Text("Refresh") }
                }
                Spacer(Modifier.height(16.dp))
                val info = state.info
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                    Detail("USER", info?.user ?: "—", Modifier.weight(1f))
                    Detail("PLATFORM", info?.architecture ?: "—", Modifier.weight(1f))
                    Detail("CPU / MEMORY", info?.let { "${it.processors} cores / ${it.memoryBytes / 1_073_741_824} GiB" } ?: "—", Modifier.weight(1f))
                    Detail("ROUND TRIP", state.latency, Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                Text(info?.operatingSystem ?: "Waiting for the native host…", color = Muted, fontSize = 12.sp)
            }
            SurfaceCard {
                SectionTitle("02", "A note that lives outside the browser")
                Text("A typed suspend call writes UTF-8 atomically through native APIs.", color = Muted, fontSize = 12.sp)
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = state.note,
                    onValueChange = { if (it.length <= MAX_NOTE_LENGTH) state.note = it },
                    modifier = Modifier.fillMaxWidth().height(112.dp),
                    placeholder = { Text("Write something. Kotlin will take it from here.") },
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        enabled = state.connected && !state.busy,
                        onClick = {
                            action {
                                val saved = it.saveNote(state.note)
                                state.path = saved.path
                                state.status = "Saved atomically by Kotlin/Native"
                                state.log("saveNote(${saved.text.length} chars) → native filesystem")
                            }
                        },
                    ) { Text(if (state.busy) "Working…" else "Save to native") }
                    Spacer(Modifier.width(12.dp))
                    TextButton(
                        enabled = state.connected && !state.busy,
                        onClick = {
                            action {
                                val note = it.readNote()
                                state.note = note.text
                                state.path = note.path
                                state.status = "Reloaded from the native file"
                                state.log("readNote() → persisted contents")
                            }
                        },
                    ) { Text("Reload") }
                    Spacer(Modifier.weight(1f))
                    Text("${state.note.length} / $MAX_NOTE_LENGTH", color = Muted, fontSize = 11.sp)
                }
                Text(state.path.ifBlank { "Application Support / Kauri / note.txt" }, color = Muted, fontSize = 10.sp)
            }
            SurfaceCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        SectionTitle("03", "A living, typed connection")
                        Text("Native Flow<Pulse> → WebSocket → Compose state", color = Muted, fontSize = 12.sp)
                    }
                    Text("#${state.sequence.toString().padStart(4, '0')}", color = Mint, fontSize = 28.sp, fontFamily = FontFamily.Monospace)
                }
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = Line)
                Spacer(Modifier.height(10.dp))
                state.events.take(3).forEach {
                    Text("›  $it", color = Muted, fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 22.sp)
                }
            }
            Text(state.status, color = if (state.connected) Mint else Muted, fontSize = 11.sp)
            Text("Built as an experiment, not a benchmark. Smallness is measured, never assumed.", color = Muted, fontSize = 10.sp)
        }
    }
}

private suspend fun runSmoke(service: DesktopService, client: HttpClient, state: DesktopState) {
    withTimeout(30_000) {
        document.body?.setAttribute("data-kauri-phase", "native persistence and validation")
        check(service.systemInfo().runtime == "Kotlin/Native")
        val text = "One binary. Two Kotlin targets. 🌿"
        check(service.saveNote(text).text == text)
        check(service.readNote().text == text)
        var rejected = false
        try { service.saveNote("x".repeat(MAX_NOTE_LENGTH + 1)) }
        catch (e: CancellationException) { throw e }
        catch (_: Throwable) { rejected = true }
        check(rejected) { "Oversized note was accepted" }
        check(service.readNote().text == text) { "Rejected write mutated the note" }
        document.body?.setAttribute("data-kauri-phase", "native Flow")
        val pulses = service.pulses().take(3).toList()
        check(pulses.map { it.sequence } == listOf(1, 2, 3))
        check(client.get("${window.location.origin}/rpc/invalid").status == HttpStatusCode.Forbidden)
        document.body?.setAttribute("data-kauri-phase", "canvas capture")
        document.body?.setAttribute("data-kauri-capture", "requested")
        state.captureRequested = true
        state.note = text
        state.sequence = pulses.last().sequence
        state.log("Smoke: Native info, Unicode persistence, validation, Flow, authorization PASS")
        while (document.body?.getAttribute("data-kauri-painted") != "true") delay(100)
        while (document.body?.getAttribute("data-kauri-image") == null) delay(100)
        withFrameNanos { }
        delay(1_000)
        sendSmokeMessage(
            "PASS: Compose painted (${document.body?.getAttribute("data-kauri-color-count")} sampled colors); Native systemInfo; Unicode note round-trip; rejected oversize; Flow 1,2,3; unauthorized RPC 403",
            document.body?.getAttribute("data-kauri-image") ?: "",
        )
    }
}

@Composable
private fun SurfaceCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(16.dp))
            .border(1.dp, Line, RoundedCornerShape(16.dp)).padding(20.dp),
        content = content,
    )
}

@Composable
private fun Metric(label: String, value: String, detail: String, modifier: Modifier) {
    Column(modifier.background(Panel, RoundedCornerShape(12.dp)).border(1.dp, Line, RoundedCornerShape(12.dp)).padding(16.dp)) {
        Text(label, color = Muted, fontSize = 9.sp, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(8.dp))
        Text(value, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        Text(detail, color = Muted, fontSize = 11.sp)
    }
}

@Composable
private fun SectionTitle(number: String, title: String) {
    Text("$number   /   $title", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun Detail(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(label, color = Muted, fontSize = 9.sp, letterSpacing = 1.sp)
        Spacer(Modifier.height(6.dp))
        Text(value, color = Ink, fontSize = 14.sp)
    }
}

@Composable
private fun BadgeLabel(text: String) {
    Text(
        text, color = Mint, fontSize = 10.sp, letterSpacing = 1.sp,
        modifier = Modifier.background(Color(0xFF223D2B), RoundedCornerShape(50)).padding(horizontal = 14.dp, vertical = 9.dp),
    )
}
