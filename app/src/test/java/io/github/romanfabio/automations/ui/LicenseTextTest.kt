package io.github.romanfabio.automations.ui

import io.github.romanfabio.automations.ui.LicenseBlock.Bullet
import io.github.romanfabio.automations.ui.LicenseBlock.Heading
import io.github.romanfabio.automations.ui.LicenseBlock.Paragraph
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LicenseTextTest {
    @Test
    fun rejoinsHardWrappedParagraphs() {
        val text = "First line of a\n  wrapped paragraph.\n\nSecond paragraph."
        assertEquals(
            listOf(Paragraph("First line of a wrapped paragraph."), Paragraph("Second paragraph.")),
            parseLicenseText(text),
        )
    }

    @Test
    fun recognisesHeadingsAndBullets() {
        val text = "# Title\n\n## Section\n\n- one\n- two: Apache-2.0\n\nTail."
        assertEquals(
            listOf(Heading("Title"), Heading("Section"), Bullet("one"), Bullet("two: Apache-2.0"), Paragraph("Tail.")),
            parseLicenseText(text),
        )
    }

    @Test
    fun toleratesWindowsLineEndingsAndBlankLinesWithSpaces() {
        val text = "# A\r\n\r\nline one\r\nline two\r\n  \r\nnext"
        assertEquals(
            listOf(Heading("A"), Paragraph("line one line two"), Paragraph("next")),
            parseLicenseText(text),
        )
    }

    @Test
    fun emptyInputGivesNoBlocks() {
        assertTrue(parseLicenseText("  \n\n \n").isEmpty())
    }

    /** The real documents shown on the About screen must parse into something sensible. */
    @Test
    fun repositoryLicenseDocumentsParse() {
        val root = generateSequence(File("").absoluteFile) { it.parentFile }.first { File(it, "LICENSE").isFile }
        val mit = parseLicenseText(File(root, "LICENSE").readText())
        val thirdParty = parseLicenseText(File(root, "THIRD_PARTY_LICENSES.md").readText())
        val apache = parseLicenseText(File(root, "licenses/APACHE-2.0.txt").readText())

        assertTrue(mit.any { it is Paragraph && "Permission is hereby granted" in it.text })
        assertTrue(thirdParty.any { it is Heading && "Shipped inside the app" in it.text })
        assertTrue(thirdParty.count { it is Bullet } >= 10)
        assertTrue(apache.any { it is Paragraph && "END OF TERMS AND CONDITIONS" in it.text })
        // nothing should still contain a hard line break after re-joining
        assertTrue((mit + thirdParty + apache).none { it is Paragraph && '\n' in it.text })
    }
}
