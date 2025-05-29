@file:Suppress("unused")

package tk.zwander.rootactivitylauncher.util.launch

import android.app.SearchManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.IPackageManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.Process
import android.os.UserHandle
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import kotlinx.coroutines.delay
import rikka.shizuku.Shizuku
import rikka.shizuku.SystemServiceHelper
import tk.zwander.rootactivitylauncher.util.hasShizukuPermission
import tk.zwander.rootactivitylauncher.util.isTouchWiz
import tk.zwander.rootactivitylauncher.util.receiver.AdminReceiver
import tk.zwander.rootactivitylauncher.util.requestShizukuPermission

sealed interface ActivityLaunchStrategy : LaunchStrategy {
    data object Normal : ActivityLaunchStrategy {
        override val priority: Int = 100

        override suspend fun Context.tryLaunch(args: LaunchArgs): List<Throwable> {
            return try {
                val i = Intent(args.intent)
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(i)
                listOf()
            } catch (e: Throwable) {
                Log.e("RootActivityLauncher", "Failure to normally start Activity", e)
                listOf(e)
            }
        }
    }
    data object Iterative : ActivityLaunchStrategy, IterativeLaunchStrategy {
        override val priority: Int = 10

        override fun extraFlags(): Int {
            return Intent.FLAG_ACTIVITY_NEW_TASK
        }

        override suspend fun Context.performLaunch(args: LaunchArgs, intent: Intent) {
            startActivity(intent)
        }
    }
    data object SamsungExploit : ActivityLaunchStrategy {
        override val priority: Int = 1

        override suspend fun Context.canRun(args: LaunchArgs): Boolean {
            return Build.VERSION.SDK_INT > Build.VERSION_CODES.P && isTouchWiz
        }

        override suspend fun Context.tryLaunch(args: LaunchArgs): List<Throwable> {
            return try {
                val wrapperIntent = Intent("com.samsung.server.telecom.USER_SELECT_WIFI_SERVICE_CALL")
                wrapperIntent.putExtra("extra_call_intent", args.intent)

                applicationContext.sendBroadcast(wrapperIntent)
                listOf()
            } catch (e: Exception) {
                Log.e("RootActivityLauncher", "Failure to launch with Samsung exploit", e)
                listOf(e)
            }
        }
    }
    data object ShizukuJava : ActivityLaunchStrategy, BinderActivityLaunchStrategy, ShizukuLaunchStrategy
    data object DhizukuJava : ActivityLaunchStrategy, BinderActivityLaunchStrategy, DhizukuLaunchStrategy
    data object AssistantJava : ActivityLaunchStrategy, ShizukuLaunchStrategy {
        override val priority: Int = 0

        private fun Context.hasWss(): Boolean = checkCallingOrSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED

        override suspend fun Context.canRun(args: LaunchArgs): Boolean {
            return hasWss() || (Shizuku.pingBinder() && (hasShizukuPermission || requestShizukuPermission()))
        }

        override suspend fun Context.callLaunch(intent: Intent) {
            if (!hasWss()) {
                val pm = IPackageManager.Stub.asInterface(wrapBinder(SystemServiceHelper.getSystemService("package")))

                pm.grantRuntimePermission(packageName, android.Manifest.permission.WRITE_SECURE_SETTINGS, UserHandle.USER_SYSTEM)
            }

            val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager

            val currentAssistant = Settings.Secure.getString(contentResolver, Settings.Secure.ASSISTANT)
            val replacedAssistant = intent.component?.flattenToString()

            try {
                Settings.Secure.putString(contentResolver, Settings.Secure.ASSISTANT, replacedAssistant)
                searchManager.launchAssist(intent.extras ?: bundleOf())
                delay(500)
            } finally {
                try {
                    Settings.Secure.putString(contentResolver, Settings.Secure.ASSISTANT, currentAssistant)
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        }
    }
    data object KNOX : ActivityLaunchStrategy {
        override suspend fun Context.canRun(args: LaunchArgs): Boolean {
            val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

            return dpm.isAdminActive(ComponentName(this, AdminReceiver::class.java))
        }

        override suspend fun Context.tryLaunch(args: LaunchArgs): List<Throwable> {
            return try {
                val cInfoClass = Class.forName("com.samsung.android.knox.ContextInfo")
                val cInfo = cInfoClass.getDeclaredConstructor(Int::class.java)
                    .newInstance(Process.myUid())

                val iapClass = Class.forName("com.samsung.android.knox.application.IApplicationPolicy\$Stub")
                val iap = iapClass.getDeclaredMethod("asInterface", IBinder::class.java)
                    .invoke(null, (SystemServiceHelper.getSystemService("application_policy")))

                val cmp = args.intent.component

                iapClass.getMethod("startApp", cInfoClass, String::class.java, String::class.java)
                    .invoke(iap, cInfo, cmp?.packageName, cmp?.className)

                listOf()
            } catch (e: Exception) {
                Log.e("RootActivityLauncher", "Unable to launch through KNOX", e)
                listOf(e)
            }
        }
    }
    data object Root : ActivityLaunchStrategy, RootLaunchStrategy {
        override val priority: Int = 1

        override fun makeCommand(args: LaunchArgs): String {
            return "am start -n ${args.intent.component?.flattenToString()}"
        }
    }
}

sealed interface ServiceLaunchStrategy : LaunchStrategy {
    data object Normal : ServiceLaunchStrategy {
        override val priority: Int = 100

        override suspend fun Context.tryLaunch(args: LaunchArgs): List<Throwable> {
            return try {
                ContextCompat.startForegroundService(this, args.intent)
                listOf()
            } catch (e: Throwable) {
                Log.e("RootActivityLauncher", "Failure to normally start Service", e)
                listOf(e)
            }
        }
    }
    data object Iterative : ServiceLaunchStrategy, IterativeLaunchStrategy {
        override val priority: Int = 20

        override suspend fun Context.performLaunch(args: LaunchArgs, intent: Intent) {
            startService(intent)
        }
    }
    data object ShizukuJava : ServiceLaunchStrategy, BinderServiceLaunchStrategy, ShizukuLaunchStrategy
    data object DhizukuJava : ServiceLaunchStrategy, BinderServiceLaunchStrategy, DhizukuLaunchStrategy
    data object Root : ServiceLaunchStrategy, RootLaunchStrategy {
        override val priority: Int = 1

        override fun makeCommand(args: LaunchArgs): String {
            return "am startservice ${args.intent.component?.flattenToString()}"
        }
    }
}

sealed interface ReceiverLaunchStrategy : LaunchStrategy {
    data object Normal : ReceiverLaunchStrategy {
        override val priority: Int = 100

        override suspend fun Context.tryLaunch(args: LaunchArgs): List<Throwable> {
            return try {
                sendBroadcast(args.intent)
                listOf()
            } catch (e: Throwable) {
                Log.e("RootActivityLauncher", "Failure to normally send broadcast.", e)
                listOf(e)
            }
        }
    }
    data object ShizukuJava : ReceiverLaunchStrategy, BinderReceiverLaunchStrategy, ShizukuLaunchStrategy
    data object DhizukuJava : ReceiverLaunchStrategy, BinderReceiverLaunchStrategy, DhizukuLaunchStrategy
    data object Root : ReceiverLaunchStrategy, RootLaunchStrategy {
        override val priority: Int = 1

        override fun makeCommand(args: LaunchArgs): String {
            return "am broadcast -n ${args.intent.component?.flattenToString()}"
        }
    }
}