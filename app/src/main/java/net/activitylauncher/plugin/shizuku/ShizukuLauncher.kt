package net.activitylauncher.plugin.shizuku

import android.app.SearchManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.view.KeyEvent
import android.util.Log
import kotlinx.coroutines.delay
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

object ShizukuLauncher {
    private const val REQUEST_CODE = 1001

    fun hasPermission(): Boolean {
        val ping = Shizuku.pingBinder()
        Log.d("ShizukuLauncher", "hasPermission: pingBinder=$ping")
        if (!ping) return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val res = Shizuku.checkSelfPermission()
            Log.d("ShizukuLauncher", "hasPermission: checkSelfPermission=$res")
            res == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun requestPermission(requestCode: Int = REQUEST_CODE, callback: (Boolean) -> Unit) {
        Log.d("ShizukuLauncher", "requestPermission: requestCode=$requestCode")
        if (!Shizuku.pingBinder()) {
            Log.e("ShizukuLauncher", "requestPermission: Shizuku binder is not alive!")
            callback(false)
            return
        }
        val listener = object : Shizuku.OnRequestPermissionResultListener {
            override fun onRequestPermissionResult(rc: Int, grantResult: Int) {
                Log.d("ShizukuLauncher", "onRequestPermissionResult: rc=$rc, result=$grantResult")
                if (rc == requestCode) {
                    callback(grantResult == PackageManager.PERMISSION_GRANTED)
                    Shizuku.removeRequestPermissionResultListener(this)
                }
            }
        }
        Shizuku.addRequestPermissionResultListener(listener)
        Log.d("ShizukuLauncher", "Calling Shizuku.requestPermission")
        try {
            Shizuku.requestPermission(requestCode)
        } catch (e: Exception) {
            Log.e("ShizukuLauncher", "Failed to call Shizuku.requestPermission", e)
            callback(false)
        }
    }

    suspend fun launchActivity(context: Context, component: ComponentName, extras: Bundle?) {
        Log.d("ShizukuLauncher", "launchActivity starting: $component")
        if (!Shizuku.pingBinder()) {
            Log.e("ShizukuLauncher", "Shizuku binder is not alive!")
            throw IllegalStateException("Shizuku is not running")
        }
        try {
            Log.d("ShizukuLauncher", "Trying assistant method")
            launchWithAssistantMethod(context, component, extras)
        } catch (e: Exception) {
            Log.w("ShizukuLauncher", "Assistant method failed, falling back to raw Shizuku", e)
            launchWithRawShizuku(component, extras)
        }
    }

    private suspend fun launchWithAssistantMethod(context: Context, component: ComponentName, extras: Bundle?) {
        Log.d("ShizukuLauncher", "launchWithAssistantMethod: $component")
        if (context.checkCallingOrSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_GRANTED) {
            Log.d("ShizukuLauncher", "Requesting WRITE_SECURE_SETTINGS")
            grantWriteSecureSettings(context.packageName)
        }

        // Settings.Secure.ASSISTANT is a hidden constant string "assistant"
        val assistantKey = "assistant"
        val originalAssistant = Settings.Secure.getString(context.contentResolver, assistantKey)
        val targetAssistant = component.flattenToString()
        Log.d("ShizukuLauncher", "Original assistant: $originalAssistant, target: $targetAssistant")

        try {
            val setRes = Settings.Secure.putString(context.contentResolver, assistantKey, targetAssistant)
            Log.d("ShizukuLauncher", "Set assistant result: $setRes")

            try {
                val searchManager = context.getSystemService(Context.SEARCH_SERVICE) as SearchManager
                Log.d("ShizukuLauncher", "Invoking launchAssist")
                try {
                    HiddenApiBypass.invoke(
                        SearchManager::class.java,
                        searchManager,
                        "launchAssist",
                        extras ?: Bundle()
                    )
                    Log.d("ShizukuLauncher", "launchAssist invoked")
                } catch (e: Exception) {
                    Log.d("ShizukuLauncher", "launchAssist failed, trying startAssist: $e")
                    HiddenApiBypass.invoke(
                        SearchManager::class.java,
                        searchManager,
                        "startAssist",
                        extras ?: Bundle()
                    )
                    Log.d("ShizukuLauncher", "startAssist invoked")
                }
            } catch (e: Exception) {
                Log.d("ShizukuLauncher", "Reflection failed, trying assist key injection: $e")
                injectAssistKey()
            }
            delay(500)
        } finally {
            Log.d("ShizukuLauncher", "Restoring original assistant: $originalAssistant")
            Settings.Secure.putString(context.contentResolver, assistantKey, originalAssistant)
        }
    }

    private fun grantWriteSecureSettings(packageName: String) {
        try {
            val binder = SystemServiceHelper.getSystemService("package")
            val wrapped = ShizukuBinderWrapper(binder)
            val iPackageManager = HiddenApiBypass.invoke(
                Class.forName("android.content.pm.IPackageManager\$Stub"),
                null,
                "asInterface",
                wrapped
            )
            
            val res = HiddenApiBypass.invoke(
                iPackageManager!!.javaClass,
                iPackageManager,
                "grantRuntimePermission",
                packageName,
                android.Manifest.permission.WRITE_SECURE_SETTINGS,
                0 // UserHandle.USER_SYSTEM usually 0
            )
            Log.d("ShizukuLauncher", "grantRuntimePermission result: $res")
        } catch (e: Exception) {
            Log.e("ShizukuLauncher", "Failed to grant WRITE_SECURE_SETTINGS", e)
        }
    }

    private fun injectAssistKey() {
        try {
            val binder = SystemServiceHelper.getSystemService(Context.INPUT_SERVICE)
            val wrapped = ShizukuBinderWrapper(binder)
            val iInputManager = HiddenApiBypass.invoke(
                Class.forName("android.hardware.input.IInputManager\$Stub"),
                null,
                "asInterface",
                wrapped
            )

            val now = SystemClock.uptimeMillis()
            val downEvent = KeyEvent(now, now, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ASSIST, 0)
            val upEvent = KeyEvent(now, now, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ASSIST, 0)

            val iInputManagerClass = Class.forName("android.hardware.input.IInputManager")
            HiddenApiBypass.invoke(iInputManagerClass, iInputManager, "injectInputEvent", downEvent, 0)
            HiddenApiBypass.invoke(iInputManagerClass, iInputManager, "injectInputEvent", upEvent, 0)
        } catch (e: Exception) {
            Log.e("ShizukuLauncher", "Failed to inject assist key", e)
        }
    }

    private fun launchWithRawShizuku(component: ComponentName, extras: Bundle?) {
        val componentStr = component.flattenToString()
        val command = StringBuilder("am start -n $componentStr")
        
        extras?.keySet()?.forEach { key ->
            val value = extras.get(key)
            if (value != null) {
                when (value) {
                    is String -> command.append(" --es \"$key\" \"$value\"")
                    is Int -> command.append(" --ei \"$key\" $value")
                    is Boolean -> command.append(" --ez \"$key\" $value")
                    is Long -> command.append(" --el \"$key\" $value")
                }
            }
        }

        val fullCommand = command.toString()
        Log.d("ShizukuLauncher", "launchWithRawShizuku command: $fullCommand")

        try {
            // Shizuku.newProcess is public but sometimes restricted by compiler
            val process = Shizuku::class.java.getMethod("newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
                .invoke(null, arrayOf("sh", "-c", fullCommand), null, null) as rikka.shizuku.ShizukuRemoteProcess
            
            Log.d("ShizukuLauncher", "Launch command sent, waiting for process exit")
            val exitCode = process.waitFor()
            Log.d("ShizukuLauncher", "Launch command process exited with: $exitCode")
        } catch (e: Exception) {
            Log.e("ShizukuLauncher", "Failed to launch via Shizuku.newProcess", e)
        }
    }
}
