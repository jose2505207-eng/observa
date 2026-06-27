package com.observa.app.cue

import com.observa.app.accessibility.CueDirection
import com.observa.app.cue.HapticCuePlayer.HapticPattern
import org.junit.Assert.assertEquals
import org.junit.Test

/** Pure tests for spatial pan mapping and haptic pattern selection. */
class CueMappingTest {

    @Test fun panMapsDirectionToStereo() {
        assertEquals(-1f, SpatialCueEngine.panOf(CueDirection.LEFT))
        assertEquals(1f, SpatialCueEngine.panOf(CueDirection.RIGHT))
        assertEquals(0f, SpatialCueEngine.panOf(CueDirection.CENTER))
        assertEquals(0f, SpatialCueEngine.panOf(CueDirection.NONE))
    }

    @Test fun urgentAlwaysSelectsStopPattern() {
        assertEquals(HapticPattern.STOP, HapticCuePlayer.selectPattern(CueDirection.LEFT, urgent = true))
        assertEquals(HapticPattern.STOP, HapticCuePlayer.selectPattern(CueDirection.CENTER, urgent = true))
    }

    @Test fun nonUrgentSelectsDirectionalPattern() {
        assertEquals(HapticPattern.LEFT, HapticCuePlayer.selectPattern(CueDirection.LEFT, urgent = false))
        assertEquals(HapticPattern.RIGHT, HapticCuePlayer.selectPattern(CueDirection.RIGHT, urgent = false))
        assertEquals(HapticPattern.FORWARD, HapticCuePlayer.selectPattern(CueDirection.CENTER, urgent = false))
        assertEquals(HapticPattern.FORWARD, HapticCuePlayer.selectPattern(CueDirection.NONE, urgent = false))
    }
}
