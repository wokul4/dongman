package com.animehub.util

object FileNameParser {

    private val episodePatterns = listOf(
        Regex("""[第]?(\d+)[集話话話]"""),
        Regex("""[Ss](\d+)[Ee](\d+)"""),
        Regex("""[Ee]p(?:isode)?[\s.]*(\d+)"""),
        Regex("""(\d+)\s*[-–—]\s*\d+"""),
        Regex("""^\d+""")
    )

    fun parseEpisodeNumber(fileName: String): Float? {
        val nameWithoutExt = fileName.substringBeforeLast(".")

        for (pattern in episodePatterns) {
            val match = pattern.find(nameWithoutExt)
            if (match != null) {
                val groups = match.groupValues
                return when {
                    // S01E03 -> use the episode part (second group)
                    groups.size >= 3 && pattern.pattern.contains("[Ee]") -> {
                        groups[2].toFloatOrNull()
                    }
                    // Pattern has capturing groups -> use first capture
                    groups.size >= 2 -> groups[1].toFloatOrNull()
                    // No capture groups -> use full match
                    else -> groups[0].toFloatOrNull()
                }
            }
        }
        return null
    }

    fun extractTitle(fileName: String): String {
        return fileName.substringBeforeLast(".")
    }
}
