package com.observa.app.service

/** Honest, plain-language permission rationale. Pure copy; unit-testable. */
object PermissionEducation {

    val camera = "Camera is used only on this device to understand your surroundings. " +
        "Images are processed locally and never uploaded."

    val microphone = "Microphone is used only when you give a voice command. " +
        "Audio is not recorded in the background or uploaded."

    val location = "Location is used only for offline navigation guidance. " +
        "Your location is not uploaded."

    val network = "OBSERVA needs no network. It has no internet permission and works in airplane mode."

    fun all(): List<String> = listOf(camera, microphone, location, network)
}
