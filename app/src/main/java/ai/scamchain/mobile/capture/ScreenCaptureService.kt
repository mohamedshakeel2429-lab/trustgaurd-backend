package ai.scamchain.mobile.capture

import android.app.*
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.DisplayMetrics
import android.view.WindowManager
import java.io.File
import java.io.FileOutputStream

/** One user-approved capture, waits for the display to settle, saves a local JPEG preview, then stops projection. */
class ScreenCaptureService : Service() {
    private var projection: MediaProjection? = null
    private var display: VirtualDisplay? = null
    private var reader: ImageReader? = null
    private var frameCount = 0
    private var captureStartedAt = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action != ACTION_CAPTURE) { stopSelf(); return START_NOT_STICKY }
        createChannel()
        val notification = Notification.Builder(this, CHANNEL)
            .setContentTitle("TrustGuard capture")
            .setContentText("Capturing current screen for safety review")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .build()
        if (Build.VERSION.SDK_INT >= 29) startForeground(41, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        else startForeground(41, notification)
        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
        val resultData = if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java) else @Suppress("DEPRECATION") intent.getParcelableExtra(EXTRA_RESULT_DATA)
        if (resultCode != Activity.RESULT_OK || resultData == null) { stopSelf(); return START_NOT_STICKY }
        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projection = manager.getMediaProjection(resultCode, resultData)
        projection?.registerCallback(object : MediaProjection.Callback() { override fun onStop() { cleanup() } }, android.os.Handler(mainLooper))
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION") (getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealMetrics(metrics)
        
        frameCount = 0
        captureStartedAt = SystemClock.elapsedRealtime()
        reader = ImageReader.newInstance(metrics.widthPixels, metrics.heightPixels, PixelFormat.RGBA_8888, 3)
        display = projection?.createVirtualDisplay("TrustGuardPreview", metrics.widthPixels, metrics.heightPixels, metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, reader?.surface, null, null)
            
        reader?.setOnImageAvailableListener({ source ->
            val image = source.acquireLatestImage() ?: return@setOnImageAvailableListener

            // Let the consent sheet and display transitions finish before choosing a frame.
            frameCount++
            if (frameCount < MIN_STABLE_FRAMES || SystemClock.elapsedRealtime() - captureStartedAt < CAPTURE_SETTLE_DELAY_MS) {
                image.close()
                return@setOnImageAvailableListener
            }

            try {
                val plane = image.planes[0]
                val buffer = plane.buffer
                val pixelStride = plane.pixelStride
                val rowStride = plane.rowStride
                val rowPadding = rowStride - pixelStride * metrics.widthPixels
                val padded = Bitmap.createBitmap(metrics.widthPixels + rowPadding / pixelStride, metrics.heightPixels, Bitmap.Config.ARGB_8888)
                padded.copyPixelsFromBuffer(buffer)
                val bitmap = Bitmap.createBitmap(padded, 0, 0, metrics.widthPixels, metrics.heightPixels)
                
                // Clean up previous capture files
                cacheDir.listFiles { _, name -> name.startsWith("capture-") || name == "latest-capture.jpg" }?.forEach { runCatching { it.delete() } }
                
                val captureFile = File(cacheDir, "capture-${System.currentTimeMillis()}.jpg")
                FileOutputStream(captureFile).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
                
                sendBroadcast(Intent(ACTION_PREVIEW_READY).setPackage(packageName).putExtra(EXTRA_PREVIEW_PATH, captureFile.absolutePath))
                bitmap.recycle()
                padded.recycle()
            } finally {
                image.close()
                cleanup()
                stopSelf()
            }
        }, android.os.Handler(mainLooper))
        return START_STICKY
    }

    private fun cleanup() { display?.release(); display = null; reader?.close(); reader = null; val active = projection; projection = null; active?.stop() }
    private fun createChannel() { if (Build.VERSION.SDK_INT >= 26) getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CHANNEL, "Screen capture", NotificationManager.IMPORTANCE_LOW)) }
    override fun onDestroy() { cleanup(); super.onDestroy() }
    companion object {
        const val ACTION_CAPTURE = "ai.scamchain.CAPTURE_ONCE"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        const val ACTION_PREVIEW_READY = "ai.scamchain.PREVIEW_READY"
        const val EXTRA_PREVIEW_PATH = "preview_path"
        private const val CAPTURE_SETTLE_DELAY_MS = 2_000L
        private const val MIN_STABLE_FRAMES = 8
        private const val CHANNEL = "scamchain_capture"
    }
}
