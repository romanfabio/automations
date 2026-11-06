package io.github.romanfabio.automations.ui

/** A displayable piece of a license document. */
sealed interface LicenseBlock {
    data class Heading(val text: String) : LicenseBlock
    data class Bullet(val text: String) : LicenseBlock
    data class Paragraph(val text: String) : LicenseBlock
}

/**
 * Turns the repository's plain-text license documents into blocks a phone can lay out.
 * The files are hard-wrapped at about 80 columns, which would break badly on a narrow screen, so lines of
 * one paragraph are re-joined and the screen wraps them itself. Understands only what those documents use:
 * "#" headings, "- " bullets (one line each) and blank-line separated paragraphs.
 */
fun parseLicenseText(text: String): List<LicenseBlock> =
    text.replace("\r\n", "\n")
        .split(Regex("\n[ \t]*\n"))
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .flatMap { block ->
            val lines = block.lines().map { it.trim() }
            when {
                lines.first().startsWith("#") ->
                    listOf(LicenseBlock.Heading(lines.joinToString(" ") { it.trimStart('#', ' ') }))
                lines.first().startsWith("- ") ->
                    lines.map { LicenseBlock.Bullet(it.removePrefix("- ").trim()) }
                else -> listOf(LicenseBlock.Paragraph(lines.joinToString(" ")))
            }
        }
