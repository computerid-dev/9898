package com.echochat.cid.util

import kotlin.random.Random

/**
 * Menghasilkan kode ID unik untuk akun tamu, format: XXXX-XXXX
 * Menghindari karakter yang gampang tertukar (0/O, 1/I).
 */
object UidGenerator {

    private const val ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"

    fun generate(): String {
        val part1 = randomChunk(4)
        val part2 = randomChunk(4)
        return "$part1-$part2"
    }

    private fun randomChunk(length: Int): String {
        return buildString {
            repeat(length) {
                append(ALPHABET[Random.nextInt(ALPHABET.length)])
            }
        }
    }
}
