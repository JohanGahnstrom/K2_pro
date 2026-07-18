package com.marewise.openfilament.ui

import android.view.Choreographer
import android.view.SurfaceView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.filament.utils.ModelViewer
import java.io.File
import java.nio.ByteBuffer

/** GLB/glTF 2.0 preview using Google's Android-native Filament renderer. */
@Composable
fun FilamentModelPreview(localPath: String, modifier: Modifier = Modifier) {
    val state = remember(localPath) { PreviewState(localPath) }
    Box(modifier.fillMaxWidth().height(340.dp)) {
        AndroidView(
            modifier = Modifier.fillMaxWidth().height(340.dp),
            factory = { context ->
                SurfaceView(context).also { surface ->
                    state.attach(surface)
                    surface.setOnTouchListener { _, event -> state.viewer?.onTouchEvent(event) ?: false }
                }
            },
            update = { surface -> if (state.viewer == null) state.attach(surface) }
        )
    }
    DisposableEffect(state) { onDispose { state.destroy() } }
}

private class PreviewState(private val path: String) : Choreographer.FrameCallback {
    var viewer: ModelViewer? = null
    private var active = false

    fun attach(surface: SurfaceView) {
        if (viewer != null) return
        val file = File(path)
        require(file.exists()) { "Preview file does not exist" }
        viewer = ModelViewer(surface).also { modelViewer ->
            val bytes = file.readBytes()
            modelViewer.loadModelGlb(ByteBuffer.wrap(bytes))
            modelViewer.transformToUnitCube()
        }
        active = true
        Choreographer.getInstance().postFrameCallback(this)
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!active) return
        viewer?.render(frameTimeNanos)
        Choreographer.getInstance().postFrameCallback(this)
    }

    fun destroy() {
        active = false
        Choreographer.getInstance().removeFrameCallback(this)
        viewer?.destroyModel()
        viewer = null
    }
}
