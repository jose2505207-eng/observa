package com.observa.app.runtime

import android.content.Context
import android.util.Log
import java.io.File

/** Truthful QNN/NPU acceleration availability. Never claims acceleration we cannot prove. */
enum class QnnStatus(val label: String) {
    AVAILABLE("QNN libraries present"),
    NOT_PRESENT("QNN absent (CPU fallback)"),
    UNKNOWN("not checked"),
}

/**
 * Detects whether Qualcomm QNN (NPU) native libraries are actually packaged with the app, so the
 * dashboard can state acceleration honestly. This only checks for the libraries' presence — it
 * does not load or invoke them; that happens once real ExecuTorch inference is wired up.
 */
object QnnRuntimeChecker {

    private const val TAG = "OBSERVA_QNN"

    private val candidates = listOf(
        "libQnnHtp.so",
        "libQnnSystem.so",
        "libqnn_executorch_backend.so",
    )

    fun check(context: Context): QnnStatus {
        return try {
            val libDir = context.applicationInfo.nativeLibraryDir
            val found = candidates.filter { File(libDir, it).exists() }
            if (found.isNotEmpty()) {
                Log.i(TAG, "QNN libraries present: $found in $libDir")
                QnnStatus.AVAILABLE
            } else {
                Log.i(TAG, "No QNN libraries in $libDir — CPU fallback (looked for $candidates)")
                QnnStatus.NOT_PRESENT
            }
        } catch (e: Exception) {
            Log.e(TAG, "QNN check failed", e)
            QnnStatus.UNKNOWN
        }
    }
}
