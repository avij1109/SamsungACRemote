package com.avij.samsungacremote

import android.content.Context

/** Stores the last selected state; loading it never sends IR. */
internal class AcStateStore(context: Context) {
    private val preferences = context.getSharedPreferences("ac_state", Context.MODE_PRIVATE)

    fun load(): AcState {
        val mode = enumValueOrDefault(preferences.getString("mode", null), AcMode.COOL)
        val fan = enumValueOrDefault(preferences.getString("fan", null), FanSpeed.AUTO)
        return AcState(
            powerOn = preferences.getBoolean("power", false),
            temperatureC = preferences.getInt("temperature", 24).coerceIn(16, 30),
            mode = mode,
            fanSpeed = if (mode == AcMode.AUTO) FanSpeed.AUTO else fan,
            swingOn = preferences.getBoolean("swing", false)
        )
    }

    fun save(state: AcState) {
        preferences.edit()
            .putBoolean("power", state.powerOn)
            .putInt("temperature", state.temperatureC)
            .putString("mode", state.mode.name)
            .putString("fan", state.fanSpeed.name)
            .putBoolean("swing", state.swingOn)
            .apply()
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == value } ?: default
}
