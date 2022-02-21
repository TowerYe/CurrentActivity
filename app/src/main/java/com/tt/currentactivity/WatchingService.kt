package com.tt.currentactivity

import android.annotation.TargetApi
import android.app.ActivityManager
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.*
import android.util.Log
import java.util.*

class WatchingService :
    Service(),
    IFloatingWindowState {

    private val mActivityManager: ActivityManager by lazy {
        getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    }

    private var timer: Timer? = null

    private val mHandler by lazy { Handler(Looper.getMainLooper()) }

    private var lastName = ""

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        Log.d("WatchingService", "Watching Service onCreate")
        super.onCreate()
        FloatingWindow.callback = this
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.d("WatchingService", "Watching Service start")
        if (null == timer) {
            timer = Timer().apply {
                scheduleAtFixedRate(RefreshTask(), 0, 500)
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    override fun onTaskRemoved(rootIntent: Intent) {
        Log.d("WatchingService", "Watching Service onTaskRemoved")
        val restartServiceIntent = Intent(applicationContext, this.javaClass).apply {
            setPackage(packageName)
        }
        val restartServicePendingIntent = PendingIntent.getService(
            applicationContext, 1, restartServiceIntent,
            PendingIntent.FLAG_ONE_SHOT
        )
        val alarmService =
            applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmService[AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 500] =
            restartServicePendingIntent
        super.onTaskRemoved(rootIntent)
    }

    internal inner class RefreshTask : TimerTask() {
        override fun run() {
            val name = getCurrentActivityName()
            if (lastName == name) {
                return
            }
            lastName = name
            Log.d("WatchingService", "top running app is : $name")
            mHandler.post {
                MainActivity.windowChange(this@WatchingService, name)
            }
        }
    }

    private fun getCurrentActivityName(): String {
        var topActivityName = ""
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            val usageStatsManager =
                getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val now = System.currentTimeMillis()
            val events = usageStatsManager.queryEvents(now - 600000, now)
            while (events.hasNextEvent()) {
                val event = UsageEvents.Event()
                events.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                    topActivityName = "${event.packageName}\n${event.className}"
                }
            }
        } else {
            val forGroundActivity = mActivityManager.getRunningTasks(1)
            topActivityName =
                forGroundActivity[0].topActivity!!.packageName + "\n" + forGroundActivity[0].topActivity!!.className
        }
        return topActivityName
    }

    override fun windowHide(isHide: Boolean) {
        if (isHide) {
            lastName = ""
            timer?.cancel()
            timer = null
        }
    }
}
