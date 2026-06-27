package com.observa.app.accessibility

/** Holds the last meaningful spoken message so "Repeat" can replay it. */
class LastMessageStore {
    @Volatile
    var last: String = ""
        private set

    fun set(message: String) { if (message.isNotBlank()) last = message }
}
