package com.avij.samsungacremote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun RemoteScreen(
    state: AcState,
    canTransmit: Boolean,
    isSending: Boolean,
    transmissionFailed: Boolean,
    transmitter: IrTransmitter,
    onStateChange: (AcState) -> Unit,
    modifier: Modifier = Modifier
) {
    val controlsEnabled = canTransmit && !isSending
    val swingDescription = stringResource(R.string.swing)
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.app_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = stringResource(R.string.remote_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = stringResource(R.string.power), style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = stringResource(if (state.powerOn) R.string.power_on_state else R.string.power_off_state),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = { onStateChange(state.copy(powerOn = !state.powerOn)) },
                    enabled = controlsEnabled
                ) {
                    Text(text = stringResource(if (state.powerOn) R.string.power_off else R.string.power_on))
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.set_temperature),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TemperatureButton(
                        label = "−",
                        description = stringResource(R.string.decrease_temperature),
                        enabled = controlsEnabled && state.temperatureC > 16,
                        onClick = { onStateChange(state.copy(temperatureC = state.temperatureC - 1)) }
                    )
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.temperature_value, state.temperatureC),
                            fontSize = 72.sp,
                            lineHeight = 80.sp,
                            fontWeight = FontWeight.Light,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    TemperatureButton(
                        label = "+",
                        description = stringResource(R.string.increase_temperature),
                        enabled = controlsEnabled && state.temperatureC < 30,
                        onClick = { onStateChange(state.copy(temperatureC = state.temperatureC + 1)) }
                    )
                }
                Text(
                    text = stringResource(
                        R.string.mode_and_fan_summary,
                        stringResource(modeLabel(state.mode)),
                        stringResource(fanLabel(state.fanSpeed))
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = stringResource(R.string.mode), style = MaterialTheme.typography.titleMedium)
                ChoiceGrid(
                    options = AcMode.entries.toList(),
                    selected = state.mode,
                    enabled = controlsEnabled,
                    labelRes = ::modeLabel,
                    onSelect = { mode ->
                        onStateChange(
                            state.copy(
                                mode = mode,
                                fanSpeed = if (mode == AcMode.AUTO) FanSpeed.AUTO else state.fanSpeed
                            )
                        )
                    }
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = stringResource(R.string.fan_speed), style = MaterialTheme.typography.titleMedium)
                ChoiceGrid(
                    options = FanSpeed.entries.toList(),
                    selected = state.fanSpeed,
                    enabled = controlsEnabled,
                    labelRes = ::fanLabel,
                    optionEnabled = { state.mode != AcMode.AUTO || it == FanSpeed.AUTO },
                    onSelect = { fan -> onStateChange(state.copy(fanSpeed = fan)) }
                )
                if (state.mode == AcMode.AUTO) {
                    Text(
                        text = stringResource(R.string.auto_mode_fan_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = stringResource(R.string.swing), style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = stringResource(R.string.vertical_swing),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = state.swingOn,
                    onCheckedChange = { onStateChange(state.copy(swingOn = it)) },
                    enabled = controlsEnabled,
                    modifier = Modifier.semantics { contentDescription = swingDescription }
                )
            }
        }

        if (isSending) Text(text = stringResource(R.string.sending))
        if (transmissionFailed) {
            Text(
                text = stringResource(R.string.transmission_failed),
                color = MaterialTheme.colorScheme.error
            )
        }
        if (!canTransmit) {
            Text(
                text = stringResource(R.string.ir_unavailable),
                color = MaterialTheme.colorScheme.error
            )
        }
        IrDiagnostics(transmitter)
    }
}

@Composable
private fun TemperatureButton(
    label: String,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.size(56.dp).semantics { contentDescription = description },
        contentPadding = PaddingValues(0.dp),
        enabled = enabled
    ) {
        Text(text = label, fontSize = 28.sp)
    }
}

@Composable
private fun <T> ChoiceGrid(
    options: List<T>,
    selected: T,
    enabled: Boolean,
    labelRes: (T) -> Int,
    onSelect: (T) -> Unit,
    optionEnabled: (T) -> Boolean = { true }
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        options.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                pair.forEach { option ->
                    FilterChip(
                        selected = selected == option,
                        onClick = { onSelect(option) },
                        label = { Text(stringResource(labelRes(option))) },
                        modifier = Modifier.weight(1f),
                        enabled = enabled && optionEnabled(option)
                    )
                }
            }
        }
    }
}

private fun modeLabel(mode: AcMode): Int = when (mode) {
    AcMode.AUTO -> R.string.mode_auto
    AcMode.COOL -> R.string.mode_cool
    AcMode.DRY -> R.string.mode_dry
    AcMode.FAN -> R.string.mode_fan
}

private fun fanLabel(fan: FanSpeed): Int = when (fan) {
    FanSpeed.AUTO -> R.string.fan_auto
    FanSpeed.LOW -> R.string.fan_low
    FanSpeed.MEDIUM -> R.string.fan_medium
    FanSpeed.HIGH -> R.string.fan_high
}

@Composable
private fun IrDiagnostics(transmitter: IrTransmitter) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    TextButton(onClick = { expanded = !expanded }) {
        Text(stringResource(if (expanded) R.string.hide_ir_details else R.string.show_ir_details))
    }
    if (expanded) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(
                        R.string.emitter_available,
                        stringResource(if (transmitter.hasEmitter) R.string.yes else R.string.no)
                    )
                )
                Text(text = stringResource(R.string.supported_frequencies))
                val ranges = transmitter.carrierFrequencies
                when {
                    !transmitter.hasEmitter -> Text(stringResource(R.string.no_emitter))
                    ranges == null -> Text(stringResource(R.string.frequencies_unavailable))
                    ranges.isEmpty() -> Text(stringResource(R.string.no_frequencies_reported))
                    else -> ranges.forEach { range ->
                        Text(stringResource(R.string.frequency_range, range.minFrequency, range.maxFrequency))
                    }
                }
                Text(text = stringResource(R.string.carrier_in_use))
            }
        }
    }
}
