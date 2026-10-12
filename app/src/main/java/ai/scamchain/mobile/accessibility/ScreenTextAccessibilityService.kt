package ai.scamchain.mobile.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/** Scaffold only: does not continuously collect or persist text. Capture is read only on explicit Analyze action. */
class ScreenTextAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) { /* Intentionally no background harvesting. */ }
    override fun onInterrupt() = Unit

    fun readActiveWindowText(): String = rootInActiveWindow?.let { root ->
        val lines = mutableListOf<String>()
        fun visit(node: android.view.accessibility.AccessibilityNodeInfo?) {
            if (node == null) return
            node.text?.toString()?.takeIf(String::isNotBlank)?.let(lines::add)
            for (i in 0 until node.childCount) visit(node.getChild(i))
        }
        visit(root)
        root.recycle()
        lines.distinct().joinToString("\n")
    }.orEmpty()
}
