package net.activitylauncher.plugin.shizuku

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class LauncherActivity : AppCompatActivity(), CoroutineScope {
    private lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    companion object {
        const val ACTION_LAUNCH_ACTIVITY = "activitylauncher.intent.action.LAUNCH_ACTIVITY"
        const val EXTRA_INTENT = "extra_intent"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("LauncherActivity", "onCreate: action=${intent.action}, component=${intent.component}")
        job = Job()

        try {
            when (intent.action) {
                ACTION_LAUNCH_ACTIVITY -> {
                    Log.d("LauncherActivity", "Handling LAUNCH_ACTIVITY")
                    handleLaunchActivity()
                }
                else -> {
                    Log.e("LauncherActivity", "Unknown action: ${intent.action}")
                    finish()
                }
            }
        } catch (e: Exception) {
            Log.e("LauncherActivity", "Error in onCreate", e)
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    private fun handleLaunchActivity() {
        val intentUri = intent.getStringExtra(EXTRA_INTENT)
        if (intentUri == null) {
            Log.e("LauncherActivity", "No EXTRA_INTENT provided")
            finish()
            return
        }

        try {
            val targetIntent = Intent.parseUri(intentUri, Intent.URI_INTENT_SCHEME)
            val component = targetIntent.component
            if (component != null) {
                performShizukuLaunch(component, targetIntent.extras)
            } else {
                Log.e("LauncherActivity", "Parsed intent has no component: $intentUri")
                finish()
            }
        } catch (e: Exception) {
            Log.e("LauncherActivity", "Failed to parse intent URI: $intentUri", e)
            finish()
        }
    }

    private fun performShizukuLaunch(component: ComponentName, extras: Bundle?) {
        if (!ShizukuLauncher.hasPermission()) {
            Log.d("LauncherActivity", "No Shizuku permission, requesting...")
            ShizukuLauncher.requestPermission { granted ->
                Log.d("LauncherActivity", "Shizuku permission result: $granted")
                if (granted) {
                    launch {
                        ShizukuLauncher.launchActivity(this@LauncherActivity, component, extras)
                        finish()
                    }
                } else {
                    Toast.makeText(this, "Shizuku permission denied", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        } else {
            Log.d("LauncherActivity", "Shizuku permission already granted")
            launch {
                ShizukuLauncher.launchActivity(this@LauncherActivity, component, extras)
                finish()
            }
        }
    }
}
