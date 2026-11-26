package com.icarus.pixelinvaders.render

import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.Inflater
import kotlin.math.abs

/**
 * Minimal PNG decoder for tests (8-bit RGBA, non-interlaced, which is what tools/make_assets.py
 * writes). Android unit tests have no javax.imageio, so we cannot use ImageIO.
 */
class PngImage(val width: Int, val height: Int, private val argb: IntArray) {
    fun argb(x: Int, y: Int): Int = argb[y * width + x]
    fun alpha(x: Int, y: Int): Int = argb(x, y) ushr 24
    fun rgb(x: Int, y: Int): Int = argb(x, y) and 0xFFFFFF
}

object PngReader {
    private val SIGNATURE = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)

    fun read(file: File): PngImage {
        val data = file.readBytes()
        require(data.size > 8 && data.copyOfRange(0, 8).contentEquals(SIGNATURE)) { "not a PNG: $file" }

        var pos = 8
        var width = 0
        var height = 0
        val idat = ByteArrayOutputStream()
        while (pos + 8 <= data.size) {
            val len = u32(data, pos)
            val type = String(data, pos + 4, 4, Charsets.US_ASCII)
            val body = pos + 8
            when (type) {
                "IHDR" -> {
                    width = u32(data, body)
                    height = u32(data, body + 4)
                    require(data[body + 8].toInt() == 8) { "bit depth must be 8" }
                    require(data[body + 9].toInt() == 6) { "colour type must be RGBA" }
                    require(data[body + 12].toInt() == 0) { "interlaced PNG not supported" }
                }
                "IDAT" -> idat.write(data, body, len)
                "IEND" -> break
            }
            pos = body + len + 4 // skip CRC
        }
        require(width > 0 && height > 0) { "missing IHDR" }

        val stride = width * 4
        val raw = ByteArray((stride + 1) * height)
        val inflater = Inflater()
        inflater.setInput(idat.toByteArray())
        var filled = 0
        while (filled < raw.size && !inflater.finished()) {
            val n = inflater.inflate(raw, filled, raw.size - filled)
            if (n == 0 && (inflater.needsInput() || inflater.needsDictionary())) break
            filled += n
        }
        inflater.end()
        require(filled == raw.size) { "truncated image data" }

        val px = ByteArray(stride * height)
        for (y in 0 until height) {
            val filter = raw[y * (stride + 1)].toInt()
            val src = y * (stride + 1) + 1
            val dst = y * stride
            for (i in 0 until stride) {
                val x = raw[src + i].toInt() and 0xFF
                val a = if (i >= 4) px[dst + i - 4].toInt() and 0xFF else 0
                val b = if (y > 0) px[dst - stride + i].toInt() and 0xFF else 0
                val c = if (i >= 4 && y > 0) px[dst - stride + i - 4].toInt() and 0xFF else 0
                val v = when (filter) {
                    0 -> x
                    1 -> x + a
                    2 -> x + b
                    3 -> x + (a + b) / 2
                    4 -> x + paeth(a, b, c)
                    else -> error("bad PNG filter $filter")
                }
                px[dst + i] = v.toByte()
            }
        }

        val argb = IntArray(width * height)
        for (i in argb.indices) {
            val o = i * 4
            argb[i] = ((px[o + 3].toInt() and 0xFF) shl 24) or
                ((px[o].toInt() and 0xFF) shl 16) or
                ((px[o + 1].toInt() and 0xFF) shl 8) or
                (px[o + 2].toInt() and 0xFF)
        }
        return PngImage(width, height, argb)
    }

    private fun u32(d: ByteArray, p: Int): Int =
        ((d[p].toInt() and 0xFF) shl 24) or ((d[p + 1].toInt() and 0xFF) shl 16) or
            ((d[p + 2].toInt() and 0xFF) shl 8) or (d[p + 3].toInt() and 0xFF)

    private fun paeth(a: Int, b: Int, c: Int): Int {
        val p = a + b - c
        val pa = abs(p - a)
        val pb = abs(p - b)
        val pc = abs(p - c)
        return if (pa <= pb && pa <= pc) a else if (pb <= pc) b else c
    }
}
