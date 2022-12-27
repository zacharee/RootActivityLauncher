@file:Suppress("unused")

package tk.zwander.rootactivitylauncher.util.launch

import android.app.AppOpsManager
import android.app.IActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.Process
import android.os.UserHandle
import android.util.Log
import androidx.core.content.ContextCompat
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import tk.zwander.rootactivitylauncher.util.isTouchWiz
import tk.zwander.rootactivitylauncher.util.receiver.AdminReceiver

sealed interface ActivityLaunchStrategy : LaunchStrategy {
    object Normal : ActivityLaunchStrategy {
        override suspend fun Context.tryLaunch(args: LaunchArgs): Throwable? {
            return try {
                val i = Intent(args.intent)
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(i)
                null
            } catch (e: SecurityException) {
                Log.e("RootActivityLauncher", "Failure to normally start Activity", e)
                e
            } catch (e: ActivityNotFoundException) {
                Log.e("RootActivityLauncher", "Failure to normally start Activity", e)
                e
            }
        }
    }
    object Iterative : ActivityLaunchStrategy, IterativeLaunchStrategy {
        override fun extraFlags(): Int {
            return Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }
    object SamsungExploit : ActivityLaunchStrategy {
        override suspend fun Context.canRun(): Boolean {
            return Build.VERSION.SDK_INT > Build.VERSION_CODES.P && isTouchWiz
        }

        override suspend fun Context.tryLaunch(args: LaunchArgs): Throwable? {
            return try {
                val wrapperIntent = Intent("com.samsung.server.telecom.USER_SELECT_WIFI_SERVICE_CALL")
                wrapperIntent.putExtra("extra_call_intent", args.intent)

                applicationContext.sendBroadcast(wrapperIntent)
                null
            } catch (e: Exception) {
                Log.e("RootActivityLauncher", "Failure to launch with Samsung exploit", e)
                e
            }
        }
    }
    object ShizukuJava : ActivityLaunchStrategy, ShizukuLaunchStrategy {
        override suspend fun Context.tryLaunch(args: LaunchArgs): Throwable? {
            return try {
                val iam = IActivityManager.Stub.asInterface(
                    ShizukuBinderWrapper(
                    SystemServiceHelper.getSystemService(Context.ACTIVITY_SERVICE))
                )

                iam.startActivity(
                    null, "com.android.shell", args.intent,
                    null, null, null, 0, 0,
                    null, null
                )
                null
            } catch (e: Exception) {
                Log.e("RootActivityLauncher", "Failure to launch through Shizuku binder", e)
                e
            }
        }
    }
    object ShizukuShell : ActivityLaunchStrategy, ShizukuShellLaunchStrategy {
        override fun makeCommand(args: LaunchArgs): String {
            return "am start -n ${args.intent.component.flattenToString()}"
        }
    }
    object KNOX : ActivityLaunchStrategy {
        override suspend fun Context.canRun(): Boolean {
            val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

            return dpm.isAdminActive(ComponentName(this, AdminReceiver::class.java))
        }

        override suspend fun Context.tryLaunch(args: LaunchArgs): Throwable? {
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

                null
            } catch (e: Exception) {
                Log.e("RootActivityLauncher", "Unable to launch through KNOX", e)
                e
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
    object Normal : ServiceLaunchStrategy {
        override suspend fun Context.tryLaunch(args: LaunchArgs): Throwable? {
            return try {
                ContextCompat.startForegroundService(this, args.intent)
                null
            } catch (e: SecurityException) {
                Log.e("RootActivityLauncher", "Failure to normally start Service", e)
                e
            }
        }
    }
    object Iterative : ServiceLaunchStrategy, IterativeLaunchStrategy
    object ShizukuJava : ServiceLaunchStrategy, ShizukuLaunchStrategy {
        override suspend fun Context.tryLaunch(args: LaunchArgs): Throwable? {
            return try {
                val iam = IActivityManager.Stub.asInterface(ShizukuBinderWrapper(
                    SystemServiceHelper.getSystemService(Context.ACTIVITY_SERVICE)))

                iam.startService(
                    null, args.intent, null, false, "com.android.shell",
                    null, UserHandle.USER_CURRENT
                )
                null
            } catch (e: Throwable) {
                Log.e("RootActivityLauncher", "Failure to launch through Shizuku binder.", e)
                e
            }
        }
    }
    object ShizukuShell : ServiceLaunchStrategy, ShizukuShellLaunchStrategy {
        override fun makeCommand(args: LaunchArgs): String {
            return "am startservice ${args.intent.component.flattenToString()}"
        }
    }
    object Root : ServiceLaunchStrategy, RootLaunchStrategy {
        override fun makeCommand(args: LaunchArgs): String {
            return "am startservice ${args.intent.component.flattenToString()}"
        }
    }
}

sealed interface ReceiverLaunchStrategy : LaunchStrategy {
    object Normal : ReceiverLaunchStrategy {
        override suspend fun Context.tryLaunch(args: LaunchArgs): Throwable? {
            return try {
                sendBroadcast(args.intent)
                null
            } catch (e: SecurityException) {
                Log.e("RootActivityLauncher", "Failure to normally send broadcast.", e)
                e
            }
        }
    }
    object Iterative : ReceiverLaunchStrategy, IterativeLaunchStrategy
    object ShizukuJava : ReceiverLaunchStrategy, ShizukuLaunchStrategy {
        override suspend fun Context.tryLaunch(args: LaunchArgs): Throwable? {
            return try {
                val iam = IActivityManager.Stub.asInterface(ShizukuBinderWrapper(
                    SystemServiceHelper.getSystemService(Context.ACTIVITY_SERVICE)))

                iam.broadcastIntent(
                    null, args.intent, null, null, 0, null,
                    null, null, AppOpsManager.OP_NONE, null, false, false,
                    0
                )
                null
            } catch (e: Throwable) {
                Log.e("RootActivityLauncher", "Failure to launch through Shizuku binder.", e)
                e
            }
        }
    }
    object ShizukuShell : ReceiverLaunchStrategy, ShizukuShellLaunchStrategy {
        override fun makeCommand(args: LaunchArgs): String {
            return "am broadcast -n ${args.intent.component.flattenToString()}"
        }
    }
    object Root : ReceiverLaunchStrategy, RootLaunchStrategy {
        override fun makeCommand(args: LaunchArgs): String {
            return "am broadcast -n ${args.intent.component.flattenToString()}"
        }
    }
}