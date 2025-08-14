package com.efedonmez.nothingmatrixmusicdisc.util

import java.text.Normalizer

object TextSanitizer {

    private val specificMap: Map<Char, Char> = mapOf(
        'ç' to 'c', 'Ç' to 'C',
        'ğ' to 'g', 'Ğ' to 'G',
        'ı' to 'i', 'İ' to 'I',
        'ö' to 'o', 'Ö' to 'O',
        'ş' to 's', 'Ş' to 'S',
        'ü' to 'u', 'Ü' to 'U',
        'â' to 'a', 'Â' to 'A',
        'î' to 'i', 'Î' to 'I',
        'û' to 'u', 'Û' to 'U'
    )

    /**
     * Glyph Matrix için metni normalize eder.
     * - Türkçe karakterleri ASCII karşılıklarına dönüştürür (ö→o, ç→c, ğ→g, ı→i, ş→s, ü→u ...)
     * - Kalan aksanları/diakritikleri kaldırır.
     */
    fun sanitizeForGlyph(input: String): String {
        if (input.isEmpty()) return input
        val mapped = StringBuilder(input.length)
        for (ch in input) {
            val repl = specificMap[ch]
            if (repl != null) {
                mapped.append(repl)
            } else {
                mapped.append(ch)
            }
        }
        val normalized = Normalizer.normalize(mapped.toString(), Normalizer.Form.NFD)
        val stripped = normalized.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        return stripped
    }
}


