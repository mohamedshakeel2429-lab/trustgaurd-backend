package ai.scamchain.mobile.overlay

import android.app.Service
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView

/** User-enabled launcher bubble; tapping opens TrustGuard. It does not inspect screen content. */
class OverlayService : Service() {
    private var windowManager: WindowManager? = null
    private var bubble: TextView? = null
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return START_NOT_STICKY }
        if (Build.VERSION.SDK_INT >= 26) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel("trustguard_overlay", "TrustGuard floating button", NotificationManager.IMPORTANCE_LOW))
            val notification = Notification.Builder(this, "trustguard_overlay")
                .setContentTitle("TrustGuard button is on")
                .setContentText("Tap the floating button to open TrustGuard; analysis is never automatic.")
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .build()
            if (Build.VERSION.SDK_INT >= 29) startForeground(42, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            else startForeground(42, notification)
        } else {
            startForeground(42, Notification.Builder(this)
                .setContentTitle("TrustGuard button is on")
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .build())
        }
        if (bubble != null) return START_STICKY
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        bubble = TextView(this).apply {
            text = "TG"
            textSize = 14f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            contentDescription = "Open TrustGuard safety checker"
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.rgb(53, 224, 242)) // Electric cyan
                setStroke((2 * resources.displayMetrics.density).toInt(), Color.parseColor("#080B12"))
            }
            elevation = 8 * resources.displayMetrics.density
            gravity = Gravity.CENTER
            setOnClickListener {
                val launch = packageManager.getLaunchIntentForPackage(packageName)?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (launch != null) startActivity(launch)
            }
        }
        val type = if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
        val size = (56 * resources.displayMetrics.density).toInt()
        val params = WindowManager.LayoutParams(size, size, type, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            y = (48 * resources.displayMetrics.density).toInt()
        }
        windowManager?.addView(bubble, params)
        return START_STICKY
    }
    override fun onDestroy() { bubble?.let { windowManager?.removeView(it) }; bubble = null; super.onDestroy() }
}
