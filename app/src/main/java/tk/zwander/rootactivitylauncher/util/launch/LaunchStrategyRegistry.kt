@file:Suppress("unused")

package tk.zwander.rootactivitylauncher.util.launch

import android.app.admin.DevicePolicyManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.Process
import android.util.Log
import androidx.core.content.ContextCompat
import rikka.shizuku.SystemServiceHelper
import tk.zwander.rootactivitylauncher.util.isTouchWiz
import tk.zwander.rootactivitylauncher.util.receiver.AdminReceiver

sealed interface ActivityLaunchStrategy : LaunchStrategy {
    data object Normal : ActivityLaunchStrategy {
        override suspend fun Context.tryLaunch(args: LaunchArgs): List<Throwable> {
            return try {
                val i = Intent(args.intent)
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(i)
                listOf()
            } catch (e: SecurityException) {
                Log.e("RootActivityLauncher", "Failure to normally start Activity", e)
                listOf(e)
            } catch (e: ActivityNotFoundException) {
                Log.e("RootActivityLauncher", "Failure to normally start Activity", e)
                listOf(e)
            }
        }
    }
    object Iterative : ActivityLaunchStrategy, IterativeLaunchStrategy {
        override fun extraFlags(): Int {
            return Intent.FLAG_ACTIVITY_NEW_TASK
        }

        override suspend fun Context.performLaunch(args: LaunchArgs, intent: Intent) {
            startActivity(intent)
        }
    }
    data object SamsungExploit : ActivityLaunchStrategy {
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
    object ShizukuJava : ActivityLaunchStrategy, BinderActivityLaunchStrategy, ShizukuLaunchStrategy
    object DhizukuJava : ActivityLaunchStrategy, BinderActivityLaunchStrategy, DhizukuLaunchStrategy
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
                    .invoke(iap, cInfo, cmp.packageName, cmp.className)

                listOf()
            } catch (e: Exception) {
                Log.e("RootActivityLauncher", "Unable to launch through KNOX", e)
                listOf(e)
            }
        }
    }
    object Root : ActivityLaunchStrategy, RootLaunchStrategy {
        override fun makeCommand(args: LaunchArgs): String {
            return "am start -n ${args.intent.component.flattenToString()}"
        }
    }
}

sealed interface ServiceLaunchStrategy : LaunchStrategy {
    data object Normal : ServiceLaunchStrategy {
        override suspend fun Context.tryLaunch(args: LaunchArgs): List<Throwable> {
            return try {
                ContextCompat.startForegroundService(this, args.intent)
                listOf()
            } catch (e: SecurityException) {
                Log.e("RootActivityLauncher", "Failure to normally start Service", e)
                listOf(e)
            }
        }
    }
    object Iterative : ServiceLaunchStrategy, IterativeLaunchStrategy {
        override suspend fun Context.performLaunch(args: LaunchArgs, intent: Intent) {
            startService(intent)
        }
    }
    object ShizukuJava : ServiceLaunchStrategy, BinderServiceLaunchStrategy, ShizukuLaunchStrategy
    object DhizukuJava : ServiceLaunchStrategy, BinderServiceLaunchStrategy, DhizukuLaunchStrategy
    object Root : ServiceLaunchStrategy, RootLaunchStrategy {
        override fun makeCommand(args: LaunchArgs): String {
            return "am startservice ${args.intent.component.flattenToString()}"
        }
    }
}

sealed interface ReceiverLaunchStrategy : LaunchStrategy {
    data object Normal : ReceiverLaunchStrategy {
        override suspend fun Context.tryLaunch(args: LaunchArgs): List<Throwable> {
            return try {
                sendBroadcast(args.intent)
                listOf()
            } catch (e: SecurityException) {
                Log.e("RootActivityLauncher", "Failure to normally send broadcast.", e)
                listOf(e)
            }
        }
    }
    object ShizukuJava : ReceiverLaunchStrategy, BinderReceiverLaunchStrategy, ShizukuLaunchStrategy
    object DhizukuJava : ReceiverLaunchStrategy, BinderReceiverLaunchStrategy, DhizukuLaunchStrategy
    object Root : ReceiverLaunchStrategy, RootLaunchStrategy {
        override fun makeCommand(args: LaunchArgs): String {
            return "am broadcast -n ${args.intent.component.flattenToString()}"
        }
    }
}