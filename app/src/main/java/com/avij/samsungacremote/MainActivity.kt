package com.avij.samsungacremote

import android.hardware.ConsumerIrManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.avij.samsungacremote.ui.theme.SamsungACRemoteTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val transmitter = IrTransmitter(getSystemService(ConsumerIrManager::class.java))

        setContent {
            SamsungACRemoteTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    IrStatusScreen(
                        transmitter = transmitter,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
private fun IrStatusScreen(
    transmitter: IrTransmitter,
    modifier: Modifier = Modifier
) {
    var lastSentOn by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var isSending by remember { mutableStateOf(false) }
    var transmissionFailed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val hasEmitter = transmitter.hasEmitter
    val frequencyRanges = transmitter.carrierFrequencies

    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(
                R.string.emitter_available,
                stringResource(if (hasEmitter) R.string.yes else R.string.no)
            )
        )
        Text(text = stringResource(R.string.supported_frequencies))
        when {
            !hasEmitter -> Text(text = stringResource(R.string.no_emitter))
            frequencyRanges == null -> Text(text = stringResource(R.string.frequencies_unavailable))
            frequencyRanges.isEmpty() -> Text(text = stringResource(R.string.no_frequencies_reported))
            else -> frequencyRanges.forEach { range ->
                Text(text = stringResource(R.string.frequency_range, range.minFrequency, range.maxFrequency))
            }
        }

        Text(
            text = stringResource(
                R.string.last_command_sent,
                stringResource(
                    when (lastSentOn) {
                        true -> R.string.power_on_state
                        false -> R.string.power_off_state
                        null -> R.string.no_command_sent
                    }
                )
            )
        )
        Button(
            onClick = {
                val nextPowerOn = lastSentOn != true
                isSending = true
                scope.launch {
                    val sent = withContext(Dispatchers.IO) {
                        transmitter.transmit(nextPowerOn)
                    }
                    if (sent) lastSentOn = nextPowerOn
                    transmissionFailed = !sent
                    isSending = false
                }
            },
            enabled = transmitter.canTransmit && !isSending
        ) {
            Text(text = stringResource(if (lastSentOn == true) R.string.power_off else R.string.power_on))
        }
        if (hasEmitter && !transmitter.canTransmit) {
            Text(text = stringResource(R.string.unsupported_carrier))
        }
        if (transmissionFailed) {
            Text(text = stringResource(R.string.transmission_failed))
        }
    }
}
