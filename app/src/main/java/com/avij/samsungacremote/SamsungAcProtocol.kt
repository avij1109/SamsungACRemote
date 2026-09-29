package com.avij.samsungacremote

internal interface IrProtocol {
    val carrierFrequencyHz: Int
    fun patternFor(state: AcState, powerChanged: Boolean): IntArray
}

/**
 * Samsung A/C extended power frames, separate from Samsung TV IR commands.
 *
 * Frame bytes come from IRremoteESP8266's captured and decoded power samples:
 * https://github.com/crankyoldgit/IRremoteESP8266/blob/master/test/ir_Samsung_test.cpp
 * (DecodePowerOnSample and DecodePowerOffSample: Cool, 24°C, Auto fan).
 * Field positions, values, checksums, timing, and LSB-first section encoding follow:
 * https://github.com/crankyoldgit/IRremoteESP8266/blob/master/src/ir_Samsung.h
 * https://github.com/crankyoldgit/IRremoteESP8266/blob/master/src/ir_Samsung.cpp
 * IRremoteESP8266 is licensed under LGPL-2.1.
 */
internal object SamsungAcProtocol : IrProtocol {
    override val carrierFrequencyHz = 38_000

    private const val SECTION_LENGTH = 7
    private const val HEADER_MARK = 690
    private const val HEADER_SPACE = 17_844
    private const val SECTION_MARK = 3_086
    private const val SECTION_SPACE = 8_864
    private const val BIT_MARK = 586
    private const val ONE_SPACE = 1_432
    private const val ZERO_SPACE = 436
    private const val SECTION_GAP = 2_886

    private val onFrame = intArrayOf(
        0x02, 0x92, 0x0F, 0x00, 0x00, 0x00, 0xF0,
        0x01, 0xD2, 0x0F, 0x00, 0x00, 0x00, 0x00,
        0x01, 0xE2, 0xFE, 0x71, 0x80, 0x11, 0xF0
    )

    private val offFrame = intArrayOf(
        0x02, 0xB2, 0x0F, 0x00, 0x00, 0x00, 0xC0,
        0x01, 0xD2, 0x0F, 0x00, 0x00, 0x00, 0x00,
        0x01, 0x02, 0xFF, 0x71, 0x80, 0x11, 0xC0
    )

    /** Power changes use three sections; setting changes use the normal two. */
    internal fun frameFor(state: AcState, powerChanged: Boolean): IntArray {
        val frame = (if (state.powerOn) onFrame else offFrame).clone()
        // The first and middle sections retain the captured power-on/off values.
        // The final section carries vertical swing (byte 16), temperature
        // (byte 18), and fan/mode (byte 19). These are standard bytes 9, 11, 12.
        frame[16] = (frame[16] and 0x8F) or ((if (state.swingOn) 2 else 7) shl 4)
        frame[18] = (frame[18] and 0x0F) or ((state.temperatureC - 16) shl 4)
        val modeBits = when (state.mode) {
            AcMode.AUTO -> 0
            AcMode.COOL -> 1
            AcMode.DRY -> 2
            AcMode.FAN -> 3
        }
        val fanBits = when {
            state.mode == AcMode.AUTO -> 6 // Special Auto fan value for Auto mode.
            else -> when (state.fanSpeed) {
                FanSpeed.AUTO -> 0
                FanSpeed.LOW -> 2
                FanSpeed.MEDIUM -> 4
                FanSpeed.HIGH -> 5
            }
        }
        frame[19] = (frame[19] and 0x81) or (fanBits shl 1) or (modeBits shl 4)
        updateChecksum(frame, 14)

        return if (powerChanged) frame else frame.copyOfRange(0, 7) + frame.copyOfRange(14, 21)
    }

    private fun updateChecksum(frame: IntArray, start: Int) {
        val sum = Integer.bitCount(frame[start]) +
            Integer.bitCount(frame[start + 1] and 0x0F) +
            Integer.bitCount(frame[start + 2] and 0xF0) +
            (start + 3..start + 6).sumOf { Integer.bitCount(frame[it]) }
        val checksum = sum xor 0xFF
        frame[start + 1] = (frame[start + 1] and 0x0F) or ((checksum and 0x0F) shl 4)
        frame[start + 2] = (frame[start + 2] and 0xF0) or (checksum shr 4)
    }

    override fun patternFor(state: AcState, powerChanged: Boolean): IntArray {
        val frame = frameFor(state, powerChanged)
        val pattern = ArrayList<Int>(2 + (frame.size / SECTION_LENGTH) * 116)
        pattern.add(HEADER_MARK)
        pattern.add(HEADER_SPACE)

        for (sectionStart in frame.indices step SECTION_LENGTH) {
            pattern.add(SECTION_MARK)
            pattern.add(SECTION_SPACE)
            for (index in sectionStart until sectionStart + SECTION_LENGTH) {
                val byte = frame[index]
                for (bit in 0..7) {
                    pattern.add(BIT_MARK)
                    pattern.add(if ((byte and (1 shl bit)) != 0) ONE_SPACE else ZERO_SPACE)
                }
            }
            pattern.add(BIT_MARK)
            pattern.add(SECTION_GAP)
        }
        return pattern.toIntArray()
    }
}
