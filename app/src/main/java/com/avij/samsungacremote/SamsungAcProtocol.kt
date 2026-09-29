package com.avij.samsungacremote

internal interface IrProtocol {
    val carrierFrequencyHz: Int
    fun patternFor(powerOn: Boolean): IntArray
}

/**
 * Samsung A/C extended power frames, separate from Samsung TV IR commands.
 *
 * Frame bytes come from IRremoteESP8266's captured and decoded power samples:
 * https://github.com/crankyoldgit/IRremoteESP8266/blob/master/test/ir_Samsung_test.cpp
 * (DecodePowerOnSample and DecodePowerOffSample: Cool, 24°C, Auto fan).
 * Timing and LSB-first section encoding follow sendSamsungAC in:
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

    override fun patternFor(powerOn: Boolean): IntArray {
        val frame = if (powerOn) onFrame else offFrame
        val pattern = ArrayList<Int>(350)
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
