package com.tt.currentactivity

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent

/**
 * @author tt
 * @date 2022/2/9-17:19
 * @description
 */
class WindowStateChangeService : AccessibilityService() {

    private val floatingWindow: FloatingWindow by lazy { FloatingWindow(this) }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("AccessibilityService", "AccessibilityService has connected")
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        Log.d("AccessibilityService", "onAccessibilityEvent, ${event.eventType}")
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (event.packageName != null && event.className != null) {
                val packageName: String = event.packageName.toString()
                val clsName: String = event.className.toString()
                Log.d("AccessibilityService", "packageName: $packageName, clsName: $clsName")
                val activityInfo = tryGetActivity(ComponentName(packageName, clsName))
                activityInfo?.let {
                    floatingWindow.onWindowChange(packageName, clsName)
                }
            }
        }
    }

    override fun onInterrupt() {
        Log.d("AccessibilityService", "AccessibilityService has onInterrupt")
    }

    private fun tryGetActivity(componentName: ComponentName): ActivityInfo? {
        return try {
            packageManager.getActivityInfo(componentName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }
}
