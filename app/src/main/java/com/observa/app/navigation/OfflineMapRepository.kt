package com.observa.app.navigation

import android.content.Context
import com.observa.app.nav.GeoPoint
import java.io.File

/** A rendered/route-bearing area available offline. Empty when no map pack is provisioned. */
data class OfflineMapArea(val name: String, val file: File, val sizeBytes: Long)

/**
 * Read-only access to the offline map files provisioned on the device. This is the **data layer** for
 * Navigation Mode's optional rendered-map/route layer; it never touches the network. When no pack is
 * installed it simply reports nothing available — compass/bearing guidance still works without it
 * ([NavigationModeController]). Route data is only available when a pack actually contains it.
 */
class OfflineMapRepository(
    private val context: Context,
    private val packs: OfflineMapPackManager = OfflineMapPackManager(context),
) {
    private val packsDir: File get() = File(context.filesDir, OfflineMapPackManager.PACK_DIR)

    fun availableAreas(): List<OfflineMapArea> =
        packs.installedPacks().mapNotNull { name ->
            File(packsDir, name).takeIf { it.isFile }?.let {
                OfflineMapArea(name.removeSuffix(OfflineMapPackManager.MAP_EXT), it, it.length())
            }
        }

    /** True only when a usable offline map area covers the demo/region. No pack → false (honest). */
    val hasRenderableMap: Boolean get() = availableAreas().isNotEmpty()

    /**
     * Whether real route data exists for [from]→[to] offline. No routing engine/route pack is bundled,
     * so this is honest-false until a routable pack is provisioned; callers fall back to bearing
     * guidance and never claim turn-by-turn.
     */
    fun hasRoute(@Suppress("UNUSED_PARAMETER") from: GeoPoint?, @Suppress("UNUSED_PARAMETER") to: GeoPoint?): Boolean = false
}
