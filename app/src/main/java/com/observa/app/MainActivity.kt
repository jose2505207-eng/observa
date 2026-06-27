package com.observa.app

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.observa.app.service.AmbientAwarenessService
import com.observa.app.service.ServiceBridge
import com.observa.app.ui.ObservaScreen
import com.observa.app.ui.theme.OBSERVATheme

class MainActivity : ComponentActivity() {

    private lateinit var controller: ObservaController

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        controller = ObservaController(applicationContext)
        controller.initVoice()

        // Foreground-service notification actions (Stop / Mute / Repeat) route here.
        ServiceBridge.instance = object : ServiceBridge {
            override fun onStopRequested() { controller.observe(false); controller.setMute(true) }
            override fun onMuteRequested() { controller.setMute(true) }
            override fun onRepeatRequested() { controller.repeatLast() }
        }

        setContent {
            OBSERVATheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    PermissionGate {
                        // Start the foreground service only once the camera permission is granted.
                        LaunchedEffect(Unit) { AmbientAwarenessService.start(applicationContext) }
                        ObservaScreen(controller)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        ServiceBridge.instance = null
        AmbientAwarenessService.stop(applicationContext)
        controller.shutdown()
        super.onDestroy()
    }
}

@Composable
private fun PermissionGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    // Camera is required (gates the app); microphone is optional (enables voice control).
    val permissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result -> granted = result[Manifest.permission.CAMERA] ?: granted }

    LaunchedEffect(Unit) {
        if (!granted) launcher.launch(permissions)
    }

    if (granted) {
        content()
    } else {
        PermissionFallback(onRequest = { launcher.launch(permissions) })
    }
}

@Composable
private fun PermissionFallback(onRequest: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics {
                contentDescription = "OBSERVA needs camera permission for offline scene awareness."
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("OBSERVA needs camera access for offline scene awareness.")
            Button(onClick = onRequest) { Text("Grant camera permission") }
        }
    }
}
