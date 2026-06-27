package com.observa.app.runtime

import android.util.Log
import com.observa.app.hazard.Detection

/**
 * One model output tensor, decoupled from ExecuTorch types (no `org.pytorch.executorch` import)
 * so that parsing logic is pure JVM code and fully unit-testable.
 */
class TensorOutput(val shape: LongArray, val data: FloatArray)

/**
 * Turns raw model output tensors into [Detection]s. The contract is strict: an implementation MUST
 * return an empty list for any output shape it does not explicitly understand, so the app can never
 * fabricate detections from an unverified tensor.
 */
interface DetectionParser {
    val name: String
    fun parse(outputs: List<TensorOutput>): List<Detection>
}

/**
 * Default parser used until a real model's output contract is documented and verified on device.
 * It NEVER emits a label — it logs the observed output shapes (so the contract can be discovered)
 * and returns empty. Swap in a model-specific parser once the shapes in
 * `assets/models/README.md` are confirmed. This is what keeps "AI detection" honest: no model
 * output is trusted until its format is proven.
 */
class StrictUnknownShapeParser : DetectionParser {
    override val name = "strict-unknown-shape"

    override fun parse(outputs: List<TensorOutput>): List<Detection> {
        val shapes = outputs.joinToString { it.shape.contentToString() }
        Log.i(
            "OBSERVA_MODEL",
            "parser=$name observed output shapes=[$shapes]; refusing to emit labels until the " +
                "output contract is documented and verified.",
        )
        return emptyList()
    }
}
