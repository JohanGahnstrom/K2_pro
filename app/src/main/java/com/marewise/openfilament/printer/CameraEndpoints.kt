package com.marewise.openfilament.printer

import com.marewise.openfilament.domain.CameraKind

/** Refactored from ha_creality_ws camera mode selection; no HA/go2rtc dependency. */
object CameraEndpoints {
    data class Candidate(val kind: CameraKind, val url: String)
    fun candidates(host:String, detected:CameraKind): List<Candidate> = when(detected) {
        CameraKind.WEBRTC -> listOf(Candidate(CameraKind.WEBRTC,"http://$host:8000"), Candidate(CameraKind.WEBRTC,"webrtc://$host:8000"))
        CameraKind.MJPEG -> listOf(Candidate(CameraKind.MJPEG,"http://$host:8080/?action=stream"),Candidate(CameraKind.MJPEG,"http://$host:4408/webcam/?action=stream"))
        CameraKind.NONE -> emptyList()
    }
    fun previewCandidates(host:String)=listOf("https://$host/downloads/original/current_print_image.png","http://$host/downloads/original/current_print_image.png")
}
