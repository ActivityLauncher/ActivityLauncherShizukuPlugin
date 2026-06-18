package net.activitylauncher.plugin.shizuku

import android.content.ComponentName
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class DebugActivity : AppCompatActivity(), CoroutineScope {
    private lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        job = Job()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 120, 48, 48)
            fitsSystemWindows = true
        }

        val componentInput = EditText(this).apply {
            hint = "Component Name (e.g. com.android.settings/.SubSettings)"
            setText("com.android.settings/.SubSettings")
        }
        root.addView(componentInput)

        val launchButton = Button(this).apply {
            text = "Launch with Shizuku"
            setOnClickListener {
                val componentStr = componentInput.text.toString()
                if (componentStr.isBlank()) {
                    Toast.makeText(this@DebugActivity, "Component cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val component = ComponentName.unflattenFromString(componentStr)
                if (component == null) {
                    Toast.makeText(this@DebugActivity, "Invalid component format", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (!ShizukuLauncher.hasPermission()) {
                    ShizukuLauncher.requestPermission { granted ->
                        if (granted) {
                            performLaunch(component)
                        } else {
                            Toast.makeText(this@DebugActivity, "Shizuku permission denied", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    performLaunch(component)
                }
            }
        }
        root.addView(launchButton)

        setContentView(root)
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    private fun performLaunch(component: ComponentName) {
        launch {
            try {
                ShizukuLauncher.launchActivity(this@DebugActivity, component, null)
                Toast.makeText(this@DebugActivity, "Launch attempted", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@DebugActivity, "Launch failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
