package com.example.minimalmusic.util

import java.nio.charset.Charset
import java.text.Normalizer

object TextEncodingUtils {
    private val latin1 = Charset.forName("ISO-8859-1")
    private val utf8 = Charsets.UTF_8

    fun fixCommonMojibake(input: String): String {
        if (input.isBlank()) return "Unknown"

        val repaired = runCatching {
            val bytes = input.toByteArray(latin1)
            val candidate = String(bytes, utf8)
            if (candidate.any { it.code in 0x0400..0x04FF } || candidate.length >= input.length) {
                candidate
            } else {
                input
            }
        }.getOrDefault(input)

        return Normalizer.normalize(repaired.trim(), Normalizer.Form.NFKC)
    }
}
