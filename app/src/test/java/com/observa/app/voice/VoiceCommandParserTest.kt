package com.observa.app.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit tests for the deterministic offline command grammar. Pure logic, no Android deps. */
class VoiceCommandParserTest {

    private val parser = VoiceCommandParser()

    @Test fun startObserving() {
        val p = parser.parse("start observing")
        assertTrue(p.intent is CommandIntent.Start)
        assertEquals(1.0f, p.confidence, 0.0001f)
    }

    @Test fun stopObserving() {
        assertTrue(parser.parse("stop observing").intent is CommandIntent.Stop)
    }

    @Test fun describeScene() {
        assertTrue(parser.parse("describe scene").intent is CommandIntent.DescribeScene)
    }

    @Test fun whatIsAhead() {
        assertTrue(parser.parse("what is ahead").intent is CommandIntent.WhatIsAhead)
    }

    @Test fun readText() {
        assertTrue(parser.parse("read text").intent is CommandIntent.ReadText)
    }

    @Test fun repeat() {
        assertTrue(parser.parse("repeat").intent is CommandIntent.Repeat)
        assertTrue(parser.parse("say again").intent is CommandIntent.Repeat)
    }

    @Test fun help() {
        assertTrue(parser.parse("help").intent is CommandIntent.Help)
    }

    @Test fun mute() {
        assertTrue(parser.parse("mute").intent is CommandIntent.Mute)
    }

    /** Regression: "unmute" contains the substring "mute" and must NOT parse as Mute. */
    @Test fun unmuteIsNotMute() {
        assertTrue(parser.parse("unmute").intent is CommandIntent.Unmute)
        assertTrue(parser.parse("speak").intent is CommandIntent.Unmute)
    }

    @Test fun caseInsensitiveAndTolerant() {
        assertTrue(parser.parse("  START OBSERVING please ").intent is CommandIntent.Start)
    }

    @Test fun unknownCommandFallsBack() {
        val p = parser.parse("banana helicopter")
        assertTrue(p.intent is CommandIntent.Unknown)
        assertEquals(0f, p.confidence, 0.0001f)
    }

    @Test fun emptyInputIsUnknown() {
        assertTrue(parser.parse("   ").intent is CommandIntent.Unknown)
    }

    @Test fun navigateExtractsDestination() {
        val p = parser.parse("navigate to the kitchen")
        val intent = p.intent
        assertTrue(intent is CommandIntent.NavigateTo)
        assertEquals("the kitchen", (intent as CommandIntent.NavigateTo).destination)
    }

    @Test fun findExtractsTarget() {
        val p = parser.parse("find my keys")
        val intent = p.intent
        assertTrue(intent is CommandIntent.Find)
        assertEquals("my keys", (intent as CommandIntent.Find).target)
    }
}
