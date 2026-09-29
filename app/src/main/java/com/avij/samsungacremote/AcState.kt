package com.avij.samsungacremote

internal enum class AcMode { AUTO, COOL, DRY, FAN }

internal enum class FanSpeed { AUTO, LOW, MEDIUM, HIGH }

/** The selected remote state. It is not a reading from the air conditioner. */
internal data class AcState(
    val powerOn: Boolean = false,
    val temperatureC: Int = 24,
    val mode: AcMode = AcMode.COOL,
    val fanSpeed: FanSpeed = FanSpeed.AUTO,
    val swingOn: Boolean = false
) {
    init {
        require(temperatureC in 16..30)
        require(mode != AcMode.AUTO || fanSpeed == FanSpeed.AUTO)
    }
}
