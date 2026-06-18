package net.activitylauncher.plugin.shizuku

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import java.util.regex.Pattern

@RunWith(AndroidJUnit4::class)
class ShizukuIntegrationTest {

    companion object {
        private val SHIZUKU_PKGS = listOf("rikka.shizuku", "moe.shizuku.privileged.api")
        private const val PLUGIN_PKG = "net.activitylauncher.plugin.shizuku"

        @BeforeClass
        @JvmStatic
        fun setup() {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val device = UiDevice.getInstance(instrumentation)

            val packages = executeShell("pm list packages")
            val activeShizukuPkg = SHIZUKU_PKGS.find { packages.contains(it) }

            assertNotNull("Shizuku package should be present. Please install Shizuku before running tests.", activeShizukuPkg)
            
            // Try to start Shizuku service if not running
            if (!rikka.shizuku.Shizuku.pingBinder()) {
                val dumpsys = executeShell("dumpsys package $activeShizukuPkg")
                val libDir = dumpsys.lines().find { it.contains("legacyNativeLibraryDir") }?.substringAfter("=")?.trim()
                if (libDir != null) {
                    val abi = android.os.Build.SUPPORTED_ABIS[0]
                    executeShell("$libDir/$abi/libshizuku.so")
                    var attempts = 0
                    while (attempts < 10 && !rikka.shizuku.Shizuku.pingBinder()) {
                        Thread.sleep(1000)
                        attempts++
                    }
                }
            }
            
            assertTrue("Shizuku service should be running", rikka.shizuku.Shizuku.pingBinder())

            // Revoke permission first to ensure we test the grant flow
            executeShell("pm revoke $PLUGIN_PKG moe.shizuku.privileged.api.PERMISSION_GRANTED")

            // Trigger and grant Shizuku permission
            val intent = Intent(LauncherActivity.ACTION_LAUNCH_ACTIVITY).apply {
                setComponent(ComponentName(PLUGIN_PKG, "$PLUGIN_PKG.LauncherActivity"))
                val targetIntent = Intent().apply {
                    component = ComponentName.unflattenFromString("com.android.settings/.Settings")
                }
                putExtra(LauncherActivity.EXTRA_INTENT, targetIntent.toUri(Intent.URI_INTENT_SCHEME))
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            instrumentation.targetContext.startActivity(intent)

            val start = System.currentTimeMillis()
            while (System.currentTimeMillis() - start < 15000) {
                allowIfVisible(device)
                if (rikka.shizuku.Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED) break
                Thread.sleep(1000)
            }
            assertTrue("Permission should be granted", rikka.shizuku.Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED)
            device.pressHome()
        }

        private fun executeShell(command: String): String {
            val pfd = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
            return ParcelFileDescriptor.AutoCloseInputStream(pfd).use { it.readBytes() }.toString(Charsets.UTF_8)
        }

        private fun allowIfVisible(device: UiDevice) {
            val selectors = listOf(
                By.text(Pattern.compile("(?i)Allow.*")),
                By.text(Pattern.compile("(?i)OK")),
                By.res("android:id/button1"),
            )
            for (selector in selectors) {
                device.findObject(selector)?.click()
            }
        }
    }

    @Test
    fun testPublicActivityLaunch() {
        launchAndVerify("com.android.settings/.Settings", "com.android.settings")
    }

    @Test
    fun testPrivateActivityLaunch() {
        launchAndVerify("com.android.settings/.SubSettings", "com.android.settings")
    }

    @Test
    fun testLaunchWithoutPermission() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val pm = context.packageManager

        // Verify the LAUNCH_ACTIVITY permission is defined (throws if missing)
        assertNotNull(
            "LAUNCH_ACTIVITY permission should be defined",
            pm.getPermissionInfo(
                "de.szalkowski.activitylauncher.permission.LAUNCH_ACTIVITY", 0
            )
        )

        // Verify the alias requires the permission
        val info = pm.resolveActivity(
            Intent(LauncherActivity.ACTION_LAUNCH_ACTIVITY).apply {
                component = ComponentName(PLUGIN_PKG, "$PLUGIN_PKG.LaunchActivity")
            }, PackageManager.MATCH_DEFAULT_ONLY
        )
        assertNotNull("LaunchActivity alias should resolve", info)
        assertEquals(
            "Activity-alias should require LAUNCH_ACTIVITY",
            "de.szalkowski.activitylauncher.permission.LAUNCH_ACTIVITY",
            info!!.activityInfo.permission
        )
    }

    private fun launchAndVerify(componentName: String, pkg: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(LauncherActivity.ACTION_LAUNCH_ACTIVITY).apply {
            setPackage(PLUGIN_PKG)
            val targetIntent = Intent().apply {
                component = ComponentName.unflattenFromString(componentName)
            }
            putExtra(LauncherActivity.EXTRA_INTENT, targetIntent.toUri(Intent.URI_INTENT_SCHEME))
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)

        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        assertTrue("$pkg should be visible", device.wait(Until.hasObject(By.pkg(pkg)), 10000))
    }
}
