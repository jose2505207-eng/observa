package com.observa.app.hazard

/**
 * Turns detections into spoken/haptic alerts while preventing spam.
 *
 * Responsibilities:
 *  - map a [Detection] label to a [HazardType] and a concise spoken [message],
 *  - apply a per-(type+direction) cooldown so the same alert does not repeat endlessly,
 *  - keep scene memory so a persisting hazard becomes "Still ahead" rather than a repeat,
 *  - announce "Path clear" once when previously-active hazards disappear.
 *
 * Both the live heuristic path and Demo Mode flow through this single engine, so behavior
 * (and suppression) is identical regardless of where detections originate.
 */
class HazardEngine(
    private val cooldownMs: Long = 6_000L,
    private val stillReminderMs: Long = 4_000L,
) {
    private data class Active(var firstSeenMs: Long, var lastAnnouncedMs: Long, var stillAnnounced: Boolean)

    private val active = HashMap<String, Active>()
    private var hadHazardsLastCycle = false
    private var lastClearMs = 0L

    /** Most recent cooldown/suppression note, for the dashboard. */
    var lastDecision: String = "idle"
        private set

    private fun key(type: HazardType, dir: Direction) = "$type:$dir"

    private fun map(detection: Detection): Hazard? {
        val dir = detection.direction
        return when (detection.label.uppercase()) {
            "OBSTACLE", "OBSTACLE_AHEAD" ->
                Hazard(HazardType.OBSTACLE_AHEAD, dir.orCenter(), Severity.HIGH, "Obstacle ahead", 0)
            "PERSON", "PERSON_APPROACHING" ->
                Hazard(HazardType.PERSON_APPROACHING, dir, Severity.MEDIUM, "Person approaching${dir.spoken()}", 0)
            "DOORWAY", "DOORWAY_LEFT" ->
                Hazard(HazardType.DOORWAY_LEFT, dir, Severity.LOW, "Doorway${dir.spoken()}", 0)
            "STAIRS", "STAIRS_DETECTED" ->
                Hazard(HazardType.STAIRS_DETECTED, dir.orCenter(), Severity.HIGH, "Stairs detected", 0)
            "TEXT", "TEXT_LEFT" ->
                Hazard(HazardType.TEXT_LEFT, dir, Severity.LOW, "Text detected${dir.spoken()}", 0)
            "CLEAR", "CLEAR_PATH" ->
                Hazard(HazardType.CLEAR_PATH, Direction.CENTER, Severity.LOW, "Path clear", 0)
            else -> null
        }
    }

    /**
     * Process this cycle's detections. Returns the hazards that should actually be announced
     * now (after cooldown / scene-memory gating), possibly empty.
     */
    fun process(detections: List<Detection>, nowMs: Long): List<Hazard> {
        val out = ArrayList<Hazard>()
        val seenKeys = HashSet<String>()

        for (det in detections) {
            val mapped = map(det) ?: continue
            if (mapped.type == HazardType.CLEAR_PATH) continue // handled below
            val k = key(mapped.type, mapped.direction)
            seenKeys.add(k)
            val state = active[k]
            if (state == null) {
                active[k] = Active(nowMs, nowMs, stillAnnounced = false)
                lastDecision = "announced ${mapped.type}"
                out.add(mapped.copy(timestampMs = nowMs))
            } else if (nowMs - state.lastAnnouncedMs >= cooldownMs) {
                state.lastAnnouncedMs = nowMs
                state.stillAnnounced = true
                lastDecision = "still ${mapped.type}"
                out.add(mapped.copy(message = "Still " + mapped.message.replaceFirstChar { it.lowercase() }, timestampMs = nowMs))
            } else if (!state.stillAnnounced && nowMs - state.firstSeenMs >= stillReminderMs) {
                state.stillAnnounced = true
                state.lastAnnouncedMs = nowMs
                lastDecision = "still ${mapped.type}"
                out.add(mapped.copy(message = "Still " + mapped.message.replaceFirstChar { it.lowercase() }, timestampMs = nowMs))
            } else {
                lastDecision = "suppressed ${mapped.type}"
            }
        }

        // Drop hazards that are no longer present.
        active.keys.retainAll(seenKeys)

        // Announce "Path clear" once when hazards have just cleared.
        if (seenKeys.isEmpty() && hadHazardsLastCycle && nowMs - lastClearMs >= cooldownMs) {
            lastClearMs = nowMs
            lastDecision = "path clear"
            out.add(Hazard(HazardType.CLEAR_PATH, Direction.CENTER, Severity.LOW, "Path clear", nowMs))
        }
        hadHazardsLastCycle = seenKeys.isNotEmpty()
        return out
    }

    /**
     * Gate a pre-built hazard (used by Demo Mode, whose messages are scripted). Applies the
     * same cooldown so scripted events also can't spam. Returns true if it should be announced.
     */
    fun gate(hazard: Hazard, nowMs: Long): Boolean {
        if (hazard.type == HazardType.CLEAR_PATH) {
            hadHazardsLastCycle = false
            lastDecision = "path clear"
            return true
        }
        val k = key(hazard.type, hazard.direction)
        val state = active[k]
        if (state != null && nowMs - state.lastAnnouncedMs < cooldownMs) {
            lastDecision = "suppressed ${hazard.type}"
            return false
        }
        active[k] = Active(nowMs, nowMs, stillAnnounced = false)
        hadHazardsLastCycle = true
        lastDecision = "announced ${hazard.type}"
        return true
    }

    fun reset() {
        active.clear()
        hadHazardsLastCycle = false
        lastClearMs = 0L
        lastDecision = "idle"
    }

    private fun Direction.orCenter() = if (this == Direction.UNKNOWN) Direction.CENTER else this
    private fun Direction.spoken() = when (this) {
        Direction.LEFT -> " on your left"
        Direction.RIGHT -> " on your right"
        Direction.CENTER -> " ahead"
        Direction.UNKNOWN -> ""
    }
}
