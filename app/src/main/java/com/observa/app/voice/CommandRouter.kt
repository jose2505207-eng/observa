package com.observa.app.voice

/** Actions OBSERVA can perform in response to commands. Implemented by the controller. */
interface CommandActions {
    fun start()
    fun stop()
    fun help()
    fun repeat()
    fun readText()
    fun describeScene()
    fun navigateTo(destination: String)
    fun find(target: String)
    fun whereAmI()
    fun whatIsAhead()
    fun cancel()
    fun mute()
    fun unmute()

    /** Short spoken/braille feedback for parser-level events (unrecognized, confirmation). */
    fun confirm(message: String)
}

/**
 * Routes parsed commands to [CommandActions]. Handles low-confidence confirmation and never
 * traps the user: Stop and Cancel always execute immediately regardless of confidence.
 */
class CommandRouter(
    private val parser: VoiceCommandParser,
    private val actions: CommandActions,
    private val confidenceThreshold: Float = 0.6f,
) {
    private var pending: CommandIntent? = null

    fun handleText(text: String) = handle(parser.parse(text))

    fun handle(parsed: ParsedCommand) {
        val intent = parsed.intent

        // Resolve an outstanding confirmation first.
        pending?.let { p ->
            when (intent) {
                is CommandIntent.Yes -> { pending = null; execute(p); return }
                is CommandIntent.No, is CommandIntent.Cancel -> { pending = null; actions.confirm("Cancelled."); return }
                else -> pending = null // any other command supersedes the pending one
            }
        }

        when (intent) {
            is CommandIntent.Unknown -> actions.confirm("Command not recognized. Say help.")
            // Safety / always-available commands bypass confirmation.
            is CommandIntent.Stop, is CommandIntent.Cancel,
            is CommandIntent.Mute, is CommandIntent.Unmute,
            is CommandIntent.Help, is CommandIntent.Repeat -> execute(intent)
            else -> {
                if (parsed.confidence < confidenceThreshold) {
                    pending = intent
                    actions.confirm("Did you mean ${describe(intent)}? Say yes or no.")
                } else {
                    execute(intent)
                }
            }
        }
    }

    private fun execute(intent: CommandIntent) {
        when (intent) {
            is CommandIntent.Start -> actions.start()
            is CommandIntent.Stop -> actions.stop()
            is CommandIntent.Help -> actions.help()
            is CommandIntent.Repeat -> actions.repeat()
            is CommandIntent.ReadText -> actions.readText()
            is CommandIntent.DescribeScene -> actions.describeScene()
            is CommandIntent.NavigateTo -> actions.navigateTo(intent.destination)
            is CommandIntent.Find -> actions.find(intent.target)
            is CommandIntent.WhereAmI -> actions.whereAmI()
            is CommandIntent.WhatIsAhead -> actions.whatIsAhead()
            is CommandIntent.Cancel -> actions.cancel()
            is CommandIntent.Mute -> actions.mute()
            is CommandIntent.Unmute -> actions.unmute()
            is CommandIntent.Yes, is CommandIntent.No -> actions.confirm("Nothing to confirm.")
            is CommandIntent.Unknown -> actions.confirm("Command not recognized. Say help.")
        }
    }

    private fun describe(intent: CommandIntent): String = when (intent) {
        is CommandIntent.Start -> "start"
        is CommandIntent.ReadText -> "read text"
        is CommandIntent.DescribeScene -> "describe scene"
        is CommandIntent.NavigateTo -> "navigate to ${intent.destination}"
        is CommandIntent.Find -> "find ${intent.target}"
        is CommandIntent.WhereAmI -> "where am I"
        is CommandIntent.WhatIsAhead -> "what is ahead"
        else -> "that command"
    }
}
