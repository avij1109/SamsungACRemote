package com.avij.samsungacremote

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SamsungAcProtocolTest {
    @Test
    fun powerOnUsesCapturedCool24AutoFrame() {
        assertArrayEquals(
            intArrayOf(
                0x02, 0x92, 0x0F, 0x00, 0x00, 0x00, 0xF0,
                0x01, 0xD2, 0x0F, 0x00, 0x00, 0x00, 0x00,
                0x01, 0xE2, 0xFE, 0x71, 0x80, 0x11, 0xF0
            ),
            decode(SamsungAcProtocol.patternFor(powerOn = true))
        )
    }

    @Test
    fun powerOffUsesCapturedCool24AutoFrame() {
        assertArrayEquals(
            intArrayOf(
                0x02, 0xB2, 0x0F, 0x00, 0x00, 0x00, 0xC0,
                0x01, 0xD2, 0x0F, 0x00, 0x00, 0x00, 0x00,
                0x01, 0x02, 0xFF, 0x71, 0x80, 0x11, 0xC0
            ),
            decode(SamsungAcProtocol.patternFor(powerOn = false))
        )
    }

    private fun decode(pattern: IntArray): IntArray {
        assertEquals(38_000, SamsungAcProtocol.carrierFrequencyHz)
        assertEquals(350, pattern.size)
        assertTrue(pattern.sum() < 2_000_000)
        assertEquals(690, pattern[0])
        assertEquals(17_844, pattern[1])

        var position = 2
        val frame = IntArray(21)
        repeat(3) { section ->
            assertEquals(3_086, pattern[position++])
            assertEquals(8_864, pattern[position++])
            repeat(7) { byteIndex ->
                var value = 0
                repeat(8) { bit ->
                    assertEquals(586, pattern[position++])
                    when (pattern[position++]) {
                        1_432 -> value = value or (1 shl bit)
                        436 -> Unit
                        else -> throw AssertionError("Unexpected bit space")
                    }
                }
                frame[section * 7 + byteIndex] = value
            }
            assertEquals(586, pattern[position++])
            assertEquals(2_886, pattern[position++])
        }
        assertEquals(pattern.size, position)
        return frame
    }
}
