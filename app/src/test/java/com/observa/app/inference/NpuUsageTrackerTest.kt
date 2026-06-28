package com.observa.app.inference

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NpuUsageTrackerTest {

    @Test
    fun stats_computeCurrentAvgMinMax() {
        val t = NpuUsageTracker()
        t.backendLabel = "QNN/NPU"; t.npuActive = true
        t.record(2f, nowMs = 1000)
        t.record(4f, nowMs = 1100)
        t.record(3f, nowMs = 1200)
        val s = t.snapshot(nowMs = 1200)
        assertEquals(3f, s.currentMs, 0.001f)
        assertEquals(3f, s.avgMs, 0.001f)   // (2+4+3)/3
        assertEquals(2f, s.minMs, 0.001f)
        assertEquals(4f, s.maxMs, 0.001f)
        assertEquals(3L, s.total)
        assertEquals(listOf(2f, 4f, 3f), s.samples)
        assertTrue(s.npuActive)
        assertTrue(s.summaryLine().contains("NPU"))
    }

    @Test
    fun ratePerSec_countsLastSecondOnly() {
        val t = NpuUsageTracker()
        t.record(2f, nowMs = 100)    // older than 1s before 'now'
        t.record(2f, nowMs = 1500)
        t.record(2f, nowMs = 1800)
        t.record(2f, nowMs = 2000)
        // window [1000, 2000] → samples at 1500,1800,2000 = 3
        assertEquals(3, t.snapshot(nowMs = 2000).ratePerSec)
    }

    @Test
    fun ringBuffer_capsAtCapacity_keepsNewest() {
        val t = NpuUsageTracker(capacity = 3)
        listOf(1f, 2f, 3f, 4f, 5f).forEachIndexed { i, v -> t.record(v, nowMs = i.toLong()) }
        val s = t.snapshot(nowMs = 10)
        assertEquals(listOf(3f, 4f, 5f), s.samples) // oldest dropped
        assertEquals(5L, s.total)                   // total still counts all
    }

    @Test
    fun empty_isHonest() {
        val t = NpuUsageTracker().apply { backendLabel = "XNNPACK CPU" }
        val s = t.snapshot()
        assertTrue(s.samples.isEmpty())
        assertFalse(s.npuActive)
        assertTrue(s.summaryLine().contains("no inferences yet"))
    }
}
