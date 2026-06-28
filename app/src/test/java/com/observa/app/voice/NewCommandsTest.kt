package com.observa.app.voice

import com.observa.app.translation.LanguageCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NewCommandsTest {

    private val parser = VoiceCommandParser()

    @Test fun parsesStartTranslation() {
        assertTrue(parser.parse("start translation").intent is CommandIntent.StartTranslation)
        assertTrue(parser.parse("translate").intent is CommandIntent.StartTranslation)
    }

    @Test fun parsesStartNavigation() {
        assertTrue(parser.parse("start navigation").intent is CommandIntent.StartNavigation)
        assertTrue(parser.parse("navigate").intent is CommandIntent.StartNavigation)
    }

    @Test fun parsesReadSignsAndDownloadMap() {
        assertTrue(parser.parse("read signs").intent is CommandIntent.ReadSigns)
        assertTrue(parser.parse("download map").intent is CommandIntent.DownloadMap)
    }

    @Test fun parsesDownloadLanguageWithName() {
        val i = parser.parse("download french").intent
        assertTrue(i is CommandIntent.DownloadLanguage)
        assertEquals("french", (i as CommandIntent.DownloadLanguage).language)
    }

    @Test fun downloadMapNotMisreadAsLanguage() {
        // "download map" must be the map command, not DownloadLanguage("map").
        assertTrue(parser.parse("download map").intent is CommandIntent.DownloadMap)
    }

    @Test fun navigateToStillParameterised() {
        val i = parser.parse("navigate to the park").intent
        assertTrue(i is CommandIntent.NavigateTo)
        assertEquals("the park", (i as CommandIntent.NavigateTo).destination)
    }

    // --- LanguageCatalog ---
    @Test fun languageCatalog_resolvesNamesAndCodes() {
        assertEquals("fr", LanguageCatalog.codeFor("French"))
        assertEquals("ja", LanguageCatalog.codeFor("japanese"))
        assertEquals("es", LanguageCatalog.codeFor("es"))   // already a code
        assertNull(LanguageCatalog.codeFor("klingon"))
        assertEquals("French", LanguageCatalog.nameFor("fr"))
    }
}
