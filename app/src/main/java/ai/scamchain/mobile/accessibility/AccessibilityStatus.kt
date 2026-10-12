package ai.scamchain.mobile.accessibility

import android.content.Context
import android.provider.Settings

object AccessibilityStatus {
    fun enabled(context: Context): Boolean = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        ?.contains("${context.packageName}/${ScreenTextAccessibilityService::class.java.name}", ignoreCase = true) == true
}
