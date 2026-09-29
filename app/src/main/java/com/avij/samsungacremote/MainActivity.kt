package com.avij.samsungacremote

import android.hardware.ConsumerIrManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.avij.samsungacremote.ui.theme.SamsungACRemoteTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private lateinit var transmitter: IrTransmitter
    private lateinit var stateStore: AcStateStore
    private var selectedState by mutableStateOf(AcState())
    private var isSending by mutableStateOf(false)
    private var transmissionFailed by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        stateStore = AcStateStore(this)
        selectedState = stateStore.load() // Restore the selection without transmitting.
        transmitter = IrTransmitter(getSystemService(ConsumerIrManager::class.java))

        setContent {
            SamsungACRemoteTheme {
                Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
                    RemoteScreen(
                        state = selectedState,
                        canTransmit = transmitter.canTransmit,
                        isSending = isSending,
                        transmissionFailed = transmissionFailed,
                        transmitter = transmitter,
                        onStateChange = ::sendState,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    private fun sendState(next: AcState) {
        if (isSending || next == selectedState || !transmitter.canTransmit) return
        val previous = selectedState
        isSending = true
        transmissionFailed = false
        lifecycleScope.launch {
            val sent = withContext(Dispatchers.IO) {
                transmitter.transmit(previous, next).also { success ->
                    if (success) stateStore.save(next)
                }
            }
            if (sent) selectedState = next else transmissionFailed = true
            isSending = false
        }
    }
}
