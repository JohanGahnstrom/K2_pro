package com.marewise.openfilament.printer

import com.marewise.openfilament.domain.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/**
 * Direct Android refactor of protocol behaviour used by 3dg1luk43/ha_creality_ws.
 * Home Assistant entities/coordinator/services were intentionally removed.
 * The app connects straight to the printer and emits a domain snapshot.
 */
class CrealityDirectClient(val endpoint: PrinterEndpoint) {
    private val json = Json { ignoreUnknownKeys = true }
    private val http = OkHttpClient.Builder().connectTimeout(4, TimeUnit.SECONDS).readTimeout(6, TimeUnit.SECONDS).build()
    private val _state = MutableStateFlow(PrinterSnapshot(connection = ConnectionState.DISCONNECTED))
    val state: StateFlow<PrinterSnapshot> = _state
    private var ws: WebSocket? = null

    fun connect() {
        _state.value = _state.value.copy(connection = ConnectionState.CONNECTING, lastError = null)
        val candidates = listOf(
            "ws://${endpoint.host}:${endpoint.crealityWsPort}/",
            "ws://${endpoint.host}/websocket",
            "ws://${endpoint.host}:${endpoint.moonrakerPort}/websocket"
        )
        connectCandidate(candidates, 0)
    }

    private fun connectCandidate(urls: List<String>, index: Int) {
        if (index >= urls.size) { refreshViaMoonraker(); return }
        ws = http.newWebSocket(Request.Builder().url(urls[index]).build(), object: WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _state.value = _state.value.copy(connection = ConnectionState.CONNECTED)
                // Creality push endpoint may begin streaming automatically. Moonraker needs explicit subscriptions.
                webSocket.send("""{"jsonrpc":"2.0","method":"printer.objects.subscribe","params":{"objects":{"print_stats":null,"extruder":null,"heater_bed":null,"temperature_sensor chamber_temp":null,"box":null}},"id":1}""")
            }
            override fun onMessage(webSocket: WebSocket, text: String) { parseFrame(text) }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) { connectCandidate(urls, index+1) }
        })
    }

    fun disconnect() { ws?.close(1000, "user"); ws = null; _state.value = _state.value.copy(connection = ConnectionState.DISCONNECTED) }

    fun sendCommand(command: PrinterCommand) {
        val method = when (command) {
            PrinterCommand.PAUSE -> "printer.print.pause"
            PrinterCommand.RESUME -> "printer.print.resume"
            PrinterCommand.CANCEL -> "printer.print.cancel"
            PrinterCommand.HOME -> null
            PrinterCommand.EMERGENCY_STOP -> "printer.emergency_stop"
        }
        if (method != null) {
            rpc(method)
        } else {
            postGcode("G28")
        }
    }

    private fun rpc(method: String) {
        val body = """{"jsonrpc":"2.0","method":"$method","id":${System.currentTimeMillis()}}"""
        val socket = ws
        if (socket != null && socket.send(body)) return
        val url = "http://${endpoint.host}:${endpoint.moonrakerPort}/${method.replace('.', '/')}"
        http.newCall(Request.Builder().url(url).post("".toRequestBody(null)).build()).enqueue(commandCallback())
    }

    private fun postGcode(script: String) {
        val payload = """{"script":"${script.replace("\", "\\").replace(""", "\"")}"}"""
        val url = "http://${endpoint.host}:${endpoint.moonrakerPort}/printer/gcode/script"
        http.newCall(Request.Builder().url(url).post(payload.toRequestBody("application/json".toMediaType())).build()).enqueue(commandCallback())
    }

    private fun commandCallback() = object : Callback {
        override fun onFailure(call: Call, e: java.io.IOException) {
            _state.value = _state.value.copy(lastError = e.message)
        }
        override fun onResponse(call: Call, response: Response) {
            response.use {
                if (!it.isSuccessful) _state.value = _state.value.copy(lastError = "Command failed: HTTP ${it.code}")
                else refreshViaMoonraker()
            }
        }
    }

    fun refreshViaMoonraker() {
        val url = "http://${endpoint.host}:${endpoint.moonrakerPort}/printer/objects/query?print_stats&extruder&heater_bed&temperature_sensor%20chamber_temp&box"
        http.newCall(Request.Builder().url(url).build()).enqueue(object: Callback {
            override fun onFailure(call: Call, e: java.io.IOException) { _state.value = _state.value.copy(connection=ConnectionState.ERROR,lastError=e.message) }
            override fun onResponse(call: Call, response: Response) { response.use { parseFrame(it.body?.string().orEmpty()) } }
        })
    }

    private fun parseFrame(text: String) = runCatching {
        val root = json.parseToJsonElement(text).jsonObject
        val status = root["result"]?.jsonObject?.get("status")?.jsonObject
            ?: root["params"]?.jsonArray?.firstOrNull()?.jsonObject
            ?: root["status"]?.jsonObject
            ?: root
        val print = status["print_stats"]?.jsonObject ?: status
        val extruder = status["extruder"]?.jsonObject
        val bed = status["heater_bed"]?.jsonObject
        val chamber = status["temperature_sensor chamber_temp"]?.jsonObject
        val box = status["box"]?.jsonObject ?: status["boxInfo"]?.jsonObject
        val old = _state.value
        _state.value = old.copy(
            connection = ConnectionState.CONNECTED,
            model = str(status,"model") ?: str(status,"modelVersion") ?: old.model,
            status = str(print,"state") ?: str(status,"state") ?: old.status,
            progress = num(status,"progress") ?: num(print,"progress") ?: old.progress,
            nozzle = temp(extruder, old.nozzle), bed = temp(bed,old.bed), chamber = temp(chamber,old.chamber),
            fileName = str(print,"filename") ?: str(status,"printFileName") ?: old.fileName,
            autoRefill = bool(box,"auto_refill") ?: old.autoRefill,
            slots = parseSlots(box).ifEmpty { old.slots },
            cameraKind = detectCamera(status, old.model),
            printDurationSeconds = num(print,"print_duration") ?: old.printDurationSeconds,
            filamentUsedMm = num(print,"filament_used") ?: old.filamentUsedMm,
            capabilities = old.capabilities.copy(
                moonraker = true,
                boxObject = box != null,
                pauseResume = true,
                cancel = true,
                camera = detectCamera(status, old.model),
                chamberTelemetry = chamber != null
            ),
            lastUpdatedEpochMs = System.currentTimeMillis(),
            lastError = null
        )
    }.onFailure { _state.value = _state.value.copy(lastError = it.message) }

    private fun parseSlots(box: JsonObject?): List<CfsSlot> {
        if (box == null) return emptyList()
        val out = mutableListOf<CfsSlot>()
        fun add(boxNo:Int, slotNo:Int, o:JsonObject) { out += CfsSlot(boxNo,slotNo,str(o,"material_type")?.removePrefix("1"),str(o,"color_value")?.removePrefix("0"),num(o,"remain_len"),num(o,"temperature"),num(o,"dry_and_humidity"),str(o,"state"),bool(o,"active")?:false) }
        box.forEach { (k,v) ->
            if (k.matches(Regex("T[1-4]")) && v is JsonArray) v.forEachIndexed { i,e -> e.jsonObject.let { add(k.drop(1).toInt(),i+1,it) } }
            if (k.matches(Regex("box[_-]?\d+",RegexOption.IGNORE_CASE)) && v is JsonObject) v.forEach { (sk,sv) -> if (sv is JsonObject) add(k.filter(Char::isDigit).toIntOrNull()?:1, sk.filter(Char::isDigit).toIntOrNull()?:1,sv) }
        }
        return out
    }

    private fun detectCamera(o:JsonObject, model:String?): CameraKind {
        val m=(str(o,"model")?:model).orEmpty().uppercase(); val explicit=str(o,"cameraType").orEmpty().lowercase()
        return when { explicit.contains("webrtc") || m.startsWith("K2") -> CameraKind.WEBRTC; explicit.contains("mjpeg") || m.startsWith("K1") || m.contains("ENDER") -> CameraKind.MJPEG; else -> CameraKind.NONE }
    }
    private fun temp(o:JsonObject?, old:Temperature)= if(o==null) old else Temperature(num(o,"temperature")?:old.actual,num(o,"target")?:old.target)
    private fun str(o:JsonObject?,k:String)=o?.get(k)?.jsonPrimitive?.contentOrNull
    private fun num(o:JsonObject?,k:String)=o?.get(k)?.jsonPrimitive?.doubleOrNull
    private fun bool(o:JsonObject?,k:String)=o?.get(k)?.jsonPrimitive?.booleanOrNull
}
