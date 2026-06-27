package com.observa.app.runtime

import android.content.Context
import android.util.Log
import java.io.File
import java.util.zip.ZipFile

/**
 * Truthful QNN availability. Detecting the delegate library is NOT the same as acceleration:
 * QNN is only "active" if the *loaded model's* method actually runs on a QNN backend (checked at
 * load time via MethodMetadata.getBackends()). This checker therefore only reports whether the
 * ExecuTorch QNN *delegate* native library is packaged — never that acceleration is happening.
 */
enum class QnnStatus(val label: String) {
    DELEGATE_PRESENT("QNN delegate present (not active)"),
    NOT_PRESENT("QNN absent (CPU)"),
    UNKNOWN("not checked"),
}

object QnnRuntimeChecker {

    private const val TAG = "OBSERVA_QNN"
    private const val DELEGATE = "libqnn_executorch_backend.so"

    fun check(context: Context): QnnStatus {
        return try {
            // On modern Android (extractNativeLibs=false) libs stay inside the APK and are NOT
            // extracted to nativeLibraryDir, so a filesystem-only check would falsely report absent.
            val onDisk = File(context.applicationInfo.nativeLibraryDir, DELEGATE).exists()
            val inApk = apkContainsLib(context, DELEGATE)
            if (onDisk || inApk) {
                Log.i(
                    TAG,
                    "QNN delegate '$DELEGATE' present (onDisk=$onDisk, inApk=$inApk) but NOT active: " +
                        "no QNN-delegated model is loaded.",
                )
                QnnStatus.DELEGATE_PRESENT
            } else {
                Log.i(TAG, "QNN delegate '$DELEGATE' absent — CPU only.")
                QnnStatus.NOT_PRESENT
            }
        } catch (e: Exception) {
            Log.e(TAG, "QNN check failed", e)
            QnnStatus.UNKNOWN
        }
    }

    private fun apkContainsLib(context: Context, libName: String): Boolean {
        val sourceDir = context.applicationInfo.sourceDir ?: return false
        return try {
            ZipFile(sourceDir).use { zip ->
                zip.entries().asSequence().any { it.name.endsWith("/$libName") }
            }
        } catch (_: Exception) {
            false
        }
    }
}
