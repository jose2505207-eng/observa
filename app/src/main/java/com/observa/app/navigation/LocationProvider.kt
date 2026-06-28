package com.observa.app.navigation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import com.observa.app.nav.GeoPoint
import com.observa.app.nav.GpsAccuracy

/**
 * Real device location for GPS Orientation Lite. Uses the Android [LocationManager] GPS/network
 * providers directly — **no Google Play Services, no INTERNET**. GPS is satellite-based and works in
 * Airplane Mode, so this preserves OBSERVA's offline guarantee.
 *
 * Honest about quality: [accuracy] reports [GpsAccuracy.NONE] until a fix arrives (or if permission
 * is missing), [GpsAccuracy.LOW] for coarse/old fixes, [GpsAccuracy.GOOD] for a recent accurate fix.
 */
class LocationProvider(private val context: Context) : LocationListener, LocationSource {

    private val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    @Volatile private var last: Location? = null

    override val hasPermission: Boolean
        get() = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /** Begin listening. Safe to call without permission (it just stays NONE). */
    override fun start() {
        val mgr = lm
        if (!hasPermission || mgr == null) {
            Log.i(TAG, "location unavailable (permission=$hasPermission, manager=${mgr != null})")
            return
        }
        try {
            // Seed with the best last-known fix so guidance can start immediately.
            val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            last = providers.mapNotNull { p -> runCatching { mgr.getLastKnownLocation(p) }.getOrNull() }
                .maxByOrNull { it.time }
            providers.forEach { p ->
                if (mgr.isProviderEnabled(p)) {
                    mgr.requestLocationUpdates(p, 1_000L, 1f, this)
                }
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "requestLocationUpdates denied: ${e.message}")
        }
    }

    override fun stop() {
        runCatching { lm?.removeUpdates(this) }
    }

    /** Current point, or null if no fix yet. */
    override fun current(): GeoPoint? = last?.let { GeoPoint(it.latitude, it.longitude) }

    /** Honest fix quality for the guidance "confidence" state. */
    override val accuracy: GpsAccuracy
        get() {
            val l = last ?: return GpsAccuracy.NONE
            val ageMs = System.currentTimeMillis() - l.time
            return when {
                !l.hasAccuracy() || l.accuracy > 50f || ageMs > 30_000L -> GpsAccuracy.LOW
                else -> GpsAccuracy.GOOD
            }
        }

    override fun onLocationChanged(location: Location) { last = location }
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
    @Deprecated("Deprecated in API 29; required by the interface on older APIs.")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

    private companion object { const val TAG = "OBSERVA_ORIENT" }
}
