package com.tt.currentactivity

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process.myUid
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.tt.currentactivity.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val mViewBinding: ActivityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mViewBinding.root)
        mViewBinding.swFloat.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                startWatchingService()
            } else {
                floatingWindow?.hide()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mViewBinding.swFloat.isChecked = false
        if (isCanDrawOverlays()) {
            if (isUsageStatsPermissionEnabled()) {
                mViewBinding.swFloat.isChecked = true
                return
            }

            showUsageAccessPermissionDialog()
            return
        }

        showOverlayPermissionDialog()
    }

    override fun onBackPressed() {
        Intent(Intent.ACTION_MAIN).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            addCategory(Intent.CATEGORY_HOME)
            startActivity(this)
        }
    }

    /**
     * 检查访问应用数据权限
     */
    private fun isUsageStatsPermissionEnabled(): Boolean {
        getSystemService(Context.APP_OPS_SERVICE)?.let {
            val appOps = it as AppOpsManager
            val mode =
                appOps.checkOpNoThrow("android:get_usage_stats", myUid(), packageName)
            return mode == AppOpsManager.MODE_ALLOWED
        }
        return false
    }

    /**
     * 是否有悬浮窗权限
     */
    private fun isCanDrawOverlays(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    private fun startWatchingService() {
        val intent = Intent(this@MainActivity, WatchingService::class.java)
        startService(intent)
    }

    /**
     * 判断AccessibilityService服务是否已经启动
     */
    private fun isAccessibilityEnabled(): Boolean {
        val am = this.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        return am.isEnabled.also { Log.d("MainActivity", "AccessibilityService服务是否已经启动：$it") }
    }

    private fun showAccessibilityPermissionDialog() {
        AlertDialog.Builder(this)
            .setMessage("需要开启无障碍服务")
            .setPositiveButton("去设置") { dialogInterface, _ ->
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                dialogInterface.dismiss()
            }
            .setNegativeButton("取消") { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .create()
            .show()
    }

    private fun showUsageAccessPermissionDialog() {
        AlertDialog.Builder(this)
            .setMessage("需要开启应用数据权限")
            .setPositiveButton("去设置") { dialogInterface, _ ->
                startActivity(
                    Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                        data = Uri.parse("package:" + this@MainActivity.packageName)
                    }
                )
                dialogInterface.dismiss()
            }
            .setNegativeButton("取消") { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .create()
            .show()
    }

    private fun showOverlayPermissionDialog() {
        AlertDialog.Builder(this)
            .setMessage("需要开启悬浮窗权限")
            .setPositiveButton("去设置") { dialogInterface, _ ->
                jumpToOverlaySettingPage()
                dialogInterface.dismiss()
            }
            .setNegativeButton("取消") { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .create()
            .show()
    }

    private fun jumpToOverlaySettingPage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                data = Uri.parse("package:" + this@MainActivity.packageName)
                startActivity(this)
            }
        }
    }

    companion object {

        @SuppressLint("StaticFieldLeak")
        private var floatingWindow: FloatingWindow? = null

        fun windowChange(context: Context, name: String) {
            if (null == floatingWindow) {
                floatingWindow = FloatingWindow(context)
            }
            floatingWindow?.onWindowChange(name)
        }
    }
}
