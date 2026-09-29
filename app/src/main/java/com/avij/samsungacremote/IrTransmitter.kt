package com.avij.samsungacremote

import android.hardware.ConsumerIrManager
import android.util.Log

internal class IrTransmitter(
    private val manager: ConsumerIrManager?,
    private val protocol: IrProtocol = SamsungAcProtocol
) {
    val hasEmitter: Boolean = manager?.hasIrEmitter() == true
    val carrierFrequencies: List<ConsumerIrManager.CarrierFrequencyRange>? =
        if (hasEmitter) manager?.carrierFrequencies?.toList() else null

    val canTransmit: Boolean = carrierFrequencies?.any { range ->
        protocol.carrierFrequencyHz in range.minFrequency..range.maxFrequency
    } == true

    fun transmit(powerOn: Boolean): Boolean {
        val state = if (powerOn) "ON" else "OFF"
        val carrier = protocol.carrierFrequencyHz
        Log.i(TAG, "Samsung AC $state requested at $carrier Hz")

        if (!canTransmit || manager == null) {
            Log.w(TAG, "Samsung AC $state not sent: no emitter or unsupported $carrier Hz carrier")
            return false
        }

        return try {
            val pattern = protocol.patternFor(powerOn)
            manager.transmit(carrier, pattern)
            Log.i(TAG, "Samsung AC $state transmitted at $carrier Hz")
            true
        } catch (error: Exception) {
            Log.e(TAG, "Samsung AC $state transmission failed at $carrier Hz", error)
            false
        }
    }

    private companion object {
        const val TAG = "IrTransmitter"
    }
}
