package com.observa.app.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Routing of parsed commands to actions, incl. the "read text" path and confirmation handling. */
class CommandRouterTest {

    /** Records which action methods fire. */
    private class FakeActions : CommandActions {
        val calls = mutableListOf<String>()
        override fun start() { calls += "start" }
        override fun stop() { calls += "stop" }
        override fun help() { calls += "help" }
        override fun repeat() { calls += "repeat" }
        override fun readText() { calls += "readText" }
        override fun describeScene() { calls += "describeScene" }
        override fun navigateTo(destination: String) { calls += "navigateTo:$destination" }
        override fun find(target: String) { calls += "find:$target" }
        override fun whereAmI() { calls += "whereAmI" }
        override fun whatIsAhead() { calls += "whatIsAhead" }
        override fun cancel() { calls += "cancel" }
        override fun mute() { calls += "mute" }
        override fun unmute() { calls += "unmute" }
        override fun brailleOn() { calls += "brailleOn" }
        override fun brailleOff() { calls += "brailleOff" }
        override fun brailleStatus() { calls += "brailleStatus" }
        override fun confirm(message: String) { calls += "confirm:$message" }
    }

    private fun router(actions: CommandActions) = CommandRouter(VoiceCommandParser(), actions)

    @Test fun readTextRoutesToReadText() {
        val actions = FakeActions()
        router(actions).handleText("read text")
        assertEquals(listOf("readText"), actions.calls)
    }

    @Test fun unmuteRoutesToUnmuteNotMute() {
        val actions = FakeActions()
        router(actions).handleText("unmute")
        assertEquals(listOf("unmute"), actions.calls)
    }

    @Test fun unknownAsksForHelp() {
        val actions = FakeActions()
        router(actions).handleText("xyzzy")
        assertTrue(actions.calls.single().startsWith("confirm:"))
    }

    @Test fun brailleStatusRoutes() {
        val actions = FakeActions()
        router(actions).handleText("braille status")
        assertEquals(listOf("brailleStatus"), actions.calls)
    }
}
