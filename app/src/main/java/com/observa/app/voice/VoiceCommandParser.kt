package com.observa.app.voice

/**
 * Deterministic offline parser for the OBSERVA command grammar. Pure logic (no Android deps)
 * so it is fully unit-testable. Matching is case-insensitive and tolerant of extra words.
 *
 * Confidence: exact/leading keyword = 1.0; keyword contained elsewhere = 0.7; none = Unknown.
 */
class VoiceCommandParser {

    fun parse(input: String): ParsedCommand {
        val text = input.trim().lowercase()
        if (text.isEmpty()) return ParsedCommand(CommandIntent.Unknown(input), 0f)

        // Parameterized commands first (they contain a destination/target).
        navigateDestination(text)?.let { return ParsedCommand(CommandIntent.NavigateTo(it), 0.9f) }
        findTarget(text)?.let { return ParsedCommand(CommandIntent.Find(it), 0.9f) }

        // Phrase-level commands (multi-word) before single keywords.
        phrase(text, "read text", "read this", "read")?.let { return it.to(CommandIntent.ReadText) }
        phrase(text, "describe scene", "describe", "what do you see")?.let { return it.to(CommandIntent.DescribeScene) }
        phrase(text, "what is ahead", "what's ahead", "ahead")?.let { return it.to(CommandIntent.WhatIsAhead) }
        phrase(text, "where am i", "my location")?.let { return it.to(CommandIntent.WhereAmI) }

        word(text, "start", "begin", "resume")?.let { return it.to(CommandIntent.Start) }
        word(text, "stop", "pause")?.let { return it.to(CommandIntent.Stop) }
        word(text, "help", "commands")?.let { return it.to(CommandIntent.Help) }
        word(text, "repeat", "again", "say again")?.let { return it.to(CommandIntent.Repeat) }
        word(text, "cancel", "never mind", "nevermind")?.let { return it.to(CommandIntent.Cancel) }
        // "unmute" must be checked before "mute": "unmute" contains the substring "mute".
        word(text, "unmute", "speak")?.let { return it.to(CommandIntent.Unmute) }
        word(text, "mute", "quiet", "silence")?.let { return it.to(CommandIntent.Mute) }
        word(text, "yes", "yeah", "correct", "confirm")?.let { return it.to(CommandIntent.Yes) }
        word(text, "no", "nope", "wrong")?.let { return it.to(CommandIntent.No) }

        return ParsedCommand(CommandIntent.Unknown(input), 0f)
    }

    private fun navigateDestination(text: String): String? {
        for (kw in listOf("navigate to ", "take me to ", "go to ", "directions to ")) {
            val i = text.indexOf(kw)
            if (i >= 0) {
                val dest = text.substring(i + kw.length).trim()
                if (dest.isNotEmpty()) return dest
            }
        }
        return null
    }

    private fun findTarget(text: String): String? {
        for (kw in listOf("find ", "locate ", "where is the ", "where is ")) {
            val i = text.indexOf(kw)
            if (i >= 0) {
                val target = text.substring(i + kw.length).trim()
                if (target.isNotEmpty()) return target
            }
        }
        return null
    }

    /** Match any phrase; leading match = 1.0, contained = 0.7. */
    private fun phrase(text: String, vararg phrases: String): Float? = matchScore(text, phrases)
    private fun word(text: String, vararg words: String): Float? = matchScore(text, words)

    private fun matchScore(text: String, keys: Array<out String>): Float? {
        for (k in keys) {
            if (text == k || text.startsWith("$k ")) return 1.0f
        }
        for (k in keys) {
            if (text.contains(k)) return 0.7f
        }
        return null
    }

    private fun Float.to(intent: CommandIntent) = ParsedCommand(intent, this)
}
