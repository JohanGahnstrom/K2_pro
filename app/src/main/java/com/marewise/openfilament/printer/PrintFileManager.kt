package com.marewise.openfilament.printer

import android.content.Context
import android.net.Uri
import com.marewise.openfilament.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okio.BufferedSink
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Local-file and Moonraker transfer layer.
 * Moonraker remains authoritative for storage and print execution.
 */
class PrintFileManager(private val context: Context) {
    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    suspend fun importDocument(uri: Uri): PrintAsset = withContext(Dispatchers.IO) {
        val name = queryName(uri) ?: "import-${System.currentTimeMillis()}.bin"
        val target = uniqueFile(name)
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Unable to open selected document" }
            target.outputStream().use { input.copyTo(it) }
        }
        asset(target, uri.toString())
    }

    suspend fun download(url: String, onProgress: (Float) -> Unit): PrintAsset = withContext(Dispatchers.IO) {
        require(url.startsWith("https://") || url.startsWith("http://")) { "Use an HTTP or HTTPS URL" }
        val request = Request.Builder().url(url).build()
        http.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "Download failed: HTTP ${response.code}" }
            val body = requireNotNull(response.body)
            val suggested = response.header("Content-Disposition")
                ?.substringAfter("filename=", "")?.trim('"', ' ')
                ?.takeIf { it.isNotBlank() }
                ?: response.request.url.pathSegments.lastOrNull()?.takeIf { it.contains('.') }
                ?: "download-${System.currentTimeMillis()}.gcode"
            val target = uniqueFile(suggested)
            val total = body.contentLength()
            body.byteStream().use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var copied = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        copied += read
                        if (total > 0) onProgress((copied.toFloat() / total).coerceIn(0f, 1f))
                    }
                }
            }
            asset(target, url)
        }
    }

    suspend fun upload(
        endpoint: PrinterEndpoint,
        asset: PrintAsset,
        startPrint: Boolean,
        onProgress: (Float) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            require(asset.isPrintable) { "${asset.displayName} must be sliced to G-code before upload" }
            val file = File(asset.localPath)
            require(file.exists()) { "Local file no longer exists" }
            val fileBody = ProgressRequestBody(file, "application/octet-stream".toMediaTypeOrNull(), onProgress)
            val multipart = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("root", "gcodes")
                .addFormDataPart("checksum", asset.sha256 ?: sha256(file))
                .addFormDataPart("print", startPrint.toString())
                .addFormDataPart("file", asset.displayName, fileBody)
                .build()
            val request = Request.Builder()
                .url("http://${endpoint.host}:${endpoint.moonrakerPort}/server/files/upload")
                .post(multipart)
                .build()
            http.newCall(request).execute().use { response ->
                check(response.isSuccessful) { "Upload failed: HTTP ${response.code} ${response.body?.string().orEmpty()}" }
                asset.displayName
            }
        }
    }

    suspend fun start(endpoint: PrinterEndpoint, remoteName: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val url = HttpUrl.Builder().scheme("http").host(endpoint.host).port(endpoint.moonrakerPort)
                .addPathSegments("printer/print/start").addQueryParameter("filename", remoteName).build()
            http.newCall(Request.Builder().url(url).post(RequestBody.create(null, ByteArray(0))).build()).execute().use {
                check(it.isSuccessful) { "Start failed: HTTP ${it.code}" }
            }
        }
    }

    private fun queryName(uri: Uri): String? {
        context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) return it.getString(0)
        }
        return uri.lastPathSegment?.substringAfterLast('/')
    }

    private fun uniqueFile(name: String): File {
        val safe = name.replace(Regex("[^A-Za-z0-9._ -]"), "_").take(160)
        val dir = File(context.filesDir, "print_assets").apply { mkdirs() }
        var candidate = File(dir, safe)
        var n = 1
        while (candidate.exists()) {
            val base = safe.substringBeforeLast('.', safe)
            val ext = safe.substringAfterLast('.', "").let { if (it.isBlank()) "" else ".$it" }
            candidate = File(dir, "$base-$n$ext"); n++
        }
        return candidate
    }

    private fun asset(file: File, source: String): PrintAsset {
        val kind = assetKind(file.name)
        return PrintAsset(
            displayName = file.name,
            localPath = file.absolutePath,
            kind = kind,
            sizeBytes = file.length(),
            source = source,
            sha256 = sha256(file),
            gcode = if (kind == PrintAssetKind.GCODE) GCodeMetadataParser.parse(file) else null
        )
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) { val n = input.read(buffer); if (n < 0) break; digest.update(buffer, 0, n) }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}

private class ProgressRequestBody(
    private val file: File,
    private val mediaType: MediaType?,
    private val progress: (Float) -> Unit
) : RequestBody() {
    override fun contentType(): MediaType? = mediaType
    override fun contentLength(): Long = file.length()
    override fun writeTo(sink: BufferedSink) {
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var sent = 0L
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                sink.write(buffer, 0, read)
                sent += read
                progress((sent.toFloat() / file.length().coerceAtLeast(1L)).coerceIn(0f, 1f))
            }
        }
    }
}
