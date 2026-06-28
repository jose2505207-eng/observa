package com.observa.app.translation

/** One offline-asset readiness fact for the setup/debug screen. */
data class AssetReadiness(val name: String, val ready: Boolean, val detail: String)

/**
 * Aggregates the offline-asset readiness OBSERVA depends on (detector model, OCR, language packs, map
 * packs, local speech) into one honest "Ready offline" / "Setup required" summary for the
 * setup/debug screen. Pure composition over booleans the callers supply — no Android deps,
 * unit-testable. Never claims ready for an asset that isn't actually present.
 */
class OfflineReadinessChecker(private val probes: () -> List<AssetReadiness>) {

    fun items(): List<AssetReadiness> = probes()

    /** Core safety path (detector) ready = the app is usable offline even if optional packs are missing. */
    fun coreReady(): Boolean = items().firstOrNull { it.name == DETECTOR }?.ready ?: false

    /** Everything (incl. optional maps/languages) provisioned. */
    fun fullyReady(): Boolean = items().all { it.ready }

    fun summaryLine(): String = when {
        !coreReady() -> "Setup required: detector unavailable"
        fullyReady() -> "Ready offline"
        else -> "Core ready offline. Optional setup: " +
            items().filterNot { it.ready }.joinToString(", ") { it.name }
    }

    companion object { const val DETECTOR = "Detector" }
}
