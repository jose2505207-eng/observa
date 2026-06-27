package com.observa.app.accessibility

import org.junit.Assert.assertEquals
import org.junit.Test

/** The store backing the "repeat" action. */
class LastMessageStoreTest {

    @Test fun returnsTheLastImportantMessage() {
        val store = LastMessageStore()
        store.set("Obstacle ahead")
        store.set("Path clear")
        assertEquals("Path clear", store.last)
    }

    @Test fun blankMessagesAreIgnored() {
        val store = LastMessageStore()
        store.set("Doorway left")
        store.set("   ")
        assertEquals("Doorway left", store.last)
    }
}
