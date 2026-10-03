package com.makemission.makeflow.core.ml

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CharWbVectorizerTest {

    @Test
    fun `charWbNgrams extracts all 2-4-grams from a single word`() {
        val ngrams = charWbNgrams("uber").toSet()
        val expected = setOf(
            " u", "ub", "be", "er", "r ",
            " ub", "ube", "ber", "er ",
            " ube", "uber", "ber "
        )
        assertEquals(expected, ngrams)
    }

    @Test
    fun `charWbNgrams extracts n-grams within word boundaries only`() {
        val ngrams = charWbNgrams("uber trip").toSet()
        assertTrue(ngrams.contains("ub"))
        assertTrue(ngrams.contains("tr"))
        assertTrue(ngrams.contains("ip"))
        assertTrue("cross-word n-gram should NOT appear",
            ngrams.none { it == "r t" || it == "er t" || it == "er tr" })
    }

    @Test
    fun `charWbNgrams lowercases input`() {
        val ngrams = charWbNgrams("UBER").toSet()
        val expected = setOf(
            " u", "ub", "be", "er", "r ",
            " ub", "ube", "ber", "er ",
            " ube", "uber", "ber "
        )
        assertEquals(expected, ngrams)
    }

    @Test
    fun `charWbNgrams handles empty string`() {
        val ngrams = charWbNgrams("")
        assertTrue(ngrams.isEmpty())
    }

    @Test
    fun `charWbNgrams handles short word with ngram_range 2-4`() {
        val ngrams = charWbNgrams("a").toSet()
        assertEquals(setOf(" a", "a ", " a "), ngrams)
    }

    @Test
    fun `vectorize produces correct feature vector for known input`() {
        val vocab = mapOf(
            " u" to 0, "ub" to 1, "be" to 2, "er" to 3, "r " to 4,
            " ub" to 5, "ube" to 6, "ber" to 7, "er " to 8,
            " ube" to 9, "uber" to 10, "ber " to 11
        )
        val vec = vectorize("uber", vocab, 20)
        assertEquals(20, vec.size)
        for (i in 0..11) assertEquals(1.0f, vec[i], 0.0f)
        for (i in 12 until 20) assertEquals(0.0f, vec[i], 0.0f)
    }

    @Test
    fun `vectorize handles unknown n-grams gracefully`() {
        val vocab = mapOf(" u" to 0, "ub" to 1)
        val vec = vectorize("xyz", vocab, 10)
        assertEquals(0.0f, vec[0], 0.0f)
        assertEquals(0.0f, vec[1], 0.0f)
    }

    @Test
    fun `vectorize output matches known vocabulary for uber trip`() {
        val vocab = mapOf(
            " u" to 0, "ub" to 1, "be" to 2, "r " to 4,
            " t" to 5, "tr" to 6, "ri" to 7, "ip" to 8, "p " to 9
        )
        val vec = vectorize("uber trip", vocab, 12)
        assertEquals(1.0f, vec[0], 0.0f)
        assertEquals(1.0f, vec[1], 0.0f)
        assertEquals(1.0f, vec[2], 0.0f)
        assertEquals(0.0f, vec[3], 0.0f)
        assertEquals(1.0f, vec[4], 0.0f)
        assertEquals(1.0f, vec[5], 0.0f)
        assertEquals(1.0f, vec[6], 0.0f)
        assertEquals(1.0f, vec[7], 0.0f)
        assertEquals(1.0f, vec[8], 0.0f)
        assertEquals(1.0f, vec[9], 0.0f)
        assertEquals(0.0f, vec[10], 0.0f)
        assertEquals(0.0f, vec[11], 0.0f)
    }
}
