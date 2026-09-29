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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.avij.samsungacremote.ui.theme.SamsungACRemoteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val irManager = getSystemService(ConsumerIrManager::class.java)
        val hasEmitter = irManager?.hasIrEmitter() == true
        val frequencyRanges = if (hasEmitter) {
            irManager?.carrierFrequencies?.map { range ->
                getString(R.string.frequency_range, range.minFrequency, range.maxFrequency)
            }
        } else {
            null
        }

        setContent {
            SamsungACRemoteTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    IrStatusScreen(
                        hasEmitter = hasEmitter,
                        frequencyRanges = frequencyRanges,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
private fun IrStatusScreen(
    hasEmitter: Boolean,
    frequencyRanges: List<String>?,
    modifier: Modifier = Modifier
) {
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
            else -> frequencyRanges.forEach { Text(text = it) }
        }
    }
}
