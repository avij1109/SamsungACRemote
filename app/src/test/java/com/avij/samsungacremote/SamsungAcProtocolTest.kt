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
            decode(SamsungAcProtocol.patternFor(AcState(powerOn = true), powerChanged = true))
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
            decode(SamsungAcProtocol.patternFor(AcState(), powerChanged = true))
        )
    }

    @Test
    fun supportedSettingsEncodeFieldsAndValidChecksums() {
        for (powerOn in listOf(false, true)) {
            for (mode in AcMode.entries) {
                for (fan in FanSpeed.entries) {
                    if (mode == AcMode.AUTO && fan != FanSpeed.AUTO) continue
                    for (temperature in 16..30) {
                        for (swingOn in listOf(false, true)) {
                            val state = AcState(powerOn, temperature, mode, fan, swingOn)
                            val frame = decode(SamsungAcProtocol.patternFor(state, powerChanged = true))
                            assertChecksums(frame)
                            assertEquals(temperature - 16, frame[18] shr 4)
                            assertEquals(if (swingOn) 2 else 7, (frame[16] shr 4) and 7)
                            assertEquals(mode.ordinal, (frame[19] shr 4) and 7)
                            val expectedFan = if (mode == AcMode.AUTO) 6 else when (fan) {
                                FanSpeed.AUTO -> 0
                                FanSpeed.LOW -> 2
                                FanSpeed.MEDIUM -> 4
                                FanSpeed.HIGH -> 5
                            }
                            assertEquals(expectedFan, (frame[19] shr 1) and 7)
                            assertEquals(if (powerOn) 3 else 0, (frame[6] shr 4) and 3)
                            assertEquals(if (powerOn) 3 else 0, (frame[20] shr 4) and 3)
                        }
                    }
                }
            }
        }
    }

    @Test
    fun settingChangeUsesNormalTwoSectionFrame() {
        val state = AcState(powerOn = true, temperatureC = 25, fanSpeed = FanSpeed.MEDIUM)
        val extended = SamsungAcProtocol.frameFor(state, powerChanged = true)
        val normal = decode(SamsungAcProtocol.patternFor(state, powerChanged = false))
        assertArrayEquals(extended.copyOfRange(0, 7) + extended.copyOfRange(14, 21), normal)
        assertChecksums(normal)
    }

    @Test
    fun unchangedCool24AutoMatchesCapturedNormalFrame() {
        assertArrayEquals(
            intArrayOf(
                0x02, 0x92, 0x0F, 0x00, 0x00, 0x00, 0xF0,
                0x01, 0xE2, 0xFE, 0x71, 0x80, 0x11, 0xF0
            ),
            decode(SamsungAcProtocol.patternFor(AcState(powerOn = true), powerChanged = false))
        )
    }

    private fun assertChecksums(frame: IntArray) {
        for (start in frame.indices step 7) {
            val section = frame.copyOfRange(start, start + 7)
            val count = Integer.bitCount(section[0]) +
                Integer.bitCount(section[1] and 0x0F) +
                Integer.bitCount(section[2] and 0xF0) +
                section.drop(3).sumOf { Integer.bitCount(it) }
            val expected = count xor 0xFF
            val actual = ((section[2] and 0x0F) shl 4) or (section[1] shr 4)
            assertEquals(expected, actual)
        }
    }

    private fun decode(pattern: IntArray): IntArray {
        assertEquals(38_000, SamsungAcProtocol.carrierFrequencyHz)
        val sections = (pattern.size - 2) / 116
        assertTrue(sections == 2 || sections == 3)
        assertEquals(2 + sections * 116, pattern.size)
        assertTrue(pattern.sum() < 2_000_000)
        assertEquals(690, pattern[0])
        assertEquals(17_844, pattern[1])

        var position = 2
        val frame = IntArray(sections * 7)
        repeat(sections) { section ->
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
