package com.observa.app.voice

/** The small, deterministic, safe command grammar OBSERVA understands. */
sealed class CommandIntent {
    object Start : CommandIntent()
    object Stop : CommandIntent()
    object Help : CommandIntent()
    object Repeat : CommandIntent()
    object ReadText : CommandIntent()
    object DescribeScene : CommandIntent()
    data class NavigateTo(val destination: String) : CommandIntent()
    object StartNavigation : CommandIntent()
    object StopNavigation : CommandIntent()
    object StartTranslation : CommandIntent()
    object StopTranslation : CommandIntent()
    data class DownloadLanguage(val language: String) : CommandIntent()
    object DownloadMap : CommandIntent()
    object ReadSigns : CommandIntent()
    data class Find(val target: String) : CommandIntent()
    object WhereAmI : CommandIntent()
    object WhatIsAhead : CommandIntent()
    object Cancel : CommandIntent()
    object Mute : CommandIntent()
    object Unmute : CommandIntent()
    object BrailleOn : CommandIntent()
    object BrailleOff : CommandIntent()
    object BrailleStatus : CommandIntent()
    object Yes : CommandIntent()
    object No : CommandIntent()

    /** Input could not be matched to any command. */
    data class Unknown(val raw: String) : CommandIntent()
}

/** A parse result with a confidence in [0f, 1f]. */
data class ParsedCommand(val intent: CommandIntent, val confidence: Float)
