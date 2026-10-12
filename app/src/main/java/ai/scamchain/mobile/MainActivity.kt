package ai.scamchain.mobile

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.os.Build
import java.io.File
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.core.content.ContextCompat
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import ai.scamchain.mobile.capture.ScreenCaptureService
import ai.scamchain.mobile.overlay.OverlayService
import ai.scamchain.mobile.ui.ScamChainApp
import ai.scamchain.mobile.ui.theme.ScamChainTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val previewReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val path = intent?.getStringExtra(ScreenCaptureService.EXTRA_PREVIEW_PATH) ?: return
                val file = File(path)
                if (file.exists()) capturePreviewState.value = file.readBytes()
            }
        }
        val filter = IntentFilter(ScreenCaptureService.ACTION_PREVIEW_READY)
        ContextCompat.registerReceiver(this, previewReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        previewReceiverInstance = previewReceiver
        setContent {
            val themePreferences = remember { getSharedPreferences("trustguard_ui", Context.MODE_PRIVATE) }
            var darkTheme by rememberSaveable { mutableStateOf(themePreferences.getBoolean("dark_theme", true)) }
            ScamChainTheme(darkTheme = darkTheme) {
                val captureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                    if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                        startForegroundService(Intent(this, ScreenCaptureService::class.java).apply {
                            action = ScreenCaptureService.ACTION_CAPTURE
                            putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, result.resultCode)
                            putExtra(ScreenCaptureService.EXTRA_RESULT_DATA, result.data)
                        })
                        // Screen capture service writes a preview to app cache and publishes its path locally.
                    }
                }
                ScamChainApp(
                    onRequestCapture = {
                        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                        captureLauncher.launch(manager.createScreenCaptureIntent())
                    },
                    onToggleOverlay = { enable ->
                        if (enable) {
                            if (Settings.canDrawOverlays(this)) {
                                if (Build.VERSION.SDK_INT >= 26) startForegroundService(Intent(this, OverlayService::class.java))
                                else startService(Intent(this, OverlayService::class.java))
                            } else {
                                pendingOverlayActivation = true
                                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                            }
                        } else stopService(Intent(this, OverlayService::class.java))
                    },
                    overlayGranted = { overlayAccess.value },
                    onOpenAccessibilitySettings = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                    accessibilityEnabled = { accessibilityAccess.value },
                    capturePreview = capturePreviewState.value,
                    onCapturePreviewConsumed = { capturePreviewState.value = null },
                    onOpenOfficialPortal = { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://cybercrime.gov.in/"))) },
                    darkTheme = darkTheme,
                    onDarkThemeChange = { enabled ->
                        darkTheme = enabled
                        themePreferences.edit().putBoolean("dark_theme", enabled).apply()
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val granted = Settings.canDrawOverlays(this)
        overlayAccess.value = granted
        accessibilityAccess.value = ai.scamchain.mobile.accessibility.AccessibilityStatus.enabled(this)
        if (pendingOverlayActivation) {
            pendingOverlayActivation = false
            if (granted) {
                if (Build.VERSION.SDK_INT >= 26) startForegroundService(Intent(this, OverlayService::class.java))
                else startService(Intent(this, OverlayService::class.java))
            }
        }
    }

    override fun onDestroy() {
        previewReceiverInstance?.let { runCatching { unregisterReceiver(it) } }
        previewReceiverInstance = null
        super.onDestroy()
    }

    private var previewReceiverInstance: BroadcastReceiver? = null
    private val capturePreviewState = mutableStateOf<ByteArray?>(null)
    private val overlayAccess = mutableStateOf(false)
    private val accessibilityAccess = mutableStateOf(false)
    private var pendingOverlayActivation = false
}
