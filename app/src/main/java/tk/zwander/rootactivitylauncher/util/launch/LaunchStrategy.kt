package tk.zwander.rootactivitylauncher.util.launch

import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.IActivityManager
import android.app.IApplicationThread
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.UserHandle
import android.util.Log
import eu.chainfire.libsuperuser.Shell
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import tk.zwander.rootactivitylauncher.util.hasShizukuPermission
import tk.zwander.rootactivitylauncher.util.requestShizukuPermission

interface LaunchStrategy {
    suspend fun Context.canRun(args: LaunchArgs): Boolean = true
    suspend fun Context.tryLaunch(args: LaunchArgs): List<Throwable>
}

interface CommandLaunchStrategy : LaunchStrategy {
    fun makeCommand(args: LaunchArgs): String

    fun makeEscapedCommand(args: LaunchArgs): String {
        val command = makeCommand(args)
        return command
            .replace("$", "\\$")
    }
}

interface ShizukuLaunchStrategy : LaunchStrategy {
    override suspend fun Context.canRun(args: LaunchArgs): Boolean {
        return Shizuku.pingBinder() &&
                (hasShizukuPermission || requestShizukuPermission())
    }

    suspend fun Context.callLaunch(intent: Intent)
}

interface ShizukuActivityLaunchStrategy : ShizukuLaunchStrategy {
    override suspend fun Context.callLaunch(intent: Intent) {
        val iam = IActivityManager.Stub.asInterface(
            ShizukuBinderWrapper(
                SystemServiceHelper.getSystemService(Context.ACTIVITY_SERVICE))
        )

        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            iam.startActivityWithFeature(
                null, "com.android.shell", null, intent,
                null, null, null, 0, 0,
                null, null
            )
        } else {
            @Suppress("DEPRECATION")
            iam.startActivity(
                null, "com.android.shell", intent,
                null, null, null, 0,
                0, null, null
            )
        }

        if (result != ActivityManager.START_SUCCESS) {
            throw Exception("Error starting Activity: $result")
        }
    }
}

interface ShizukuServiceLaunchStrategy : ShizukuLaunchStrategy {
    override suspend fun Context.callLaunch(intent: Intent) {
        val iam = IActivityManager.Stub.asInterface(ShizukuBinderWrapper(
            SystemServiceHelper.getSystemService(Context.ACTIVITY_SERVICE)))

        val cn = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                iam.startService(
                    null, intent, null, false, "com.android.shell",
                    null, UserHandle.USER_CURRENT
                )
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                iam::class.java.getMethod(
                    "startService",
                    IApplicationThread::class.java, Intent::class.java,
                    String::class.java, Boolean::class.java,
                    String::class.java, Int::class.java
                ).invoke(
                    iam,
                    null, intent, null, false, "com.android.shell", UserHandle.USER_CURRENT
                ) as? ComponentName
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                iam::class.java.getMethod(
                    "startService",
                    IApplicationThread::class.java, Intent::class.java,
                    String::class.java, String::class.java, Int::class.java
                ).invoke(
                    iam,
                    null, intent, null, "com.android.shell", UserHandle.USER_CURRENT
                ) as? ComponentName
            }
            else -> {
                iam::class.java.getMethod(
                    "startService",
                    IApplicationThread::class.java, Intent::class.java,
                    String::class.java, Int::class.java
                ).invoke(
                    iam,
                    null, intent, null, UserHandle.USER_CURRENT
                ) as? ComponentName
            }
        }

        when (cn?.packageName) {
            null -> throw Exception("Unable to find service!")
            "!" -> throw Exception("Requires permission ${cn.className}")
            "!!", "?" -> throw Exception(cn.className)
        }
    }
}

interface ShizukuReceiverLaunchStrategy : ShizukuLaunchStrategy {
    override suspend fun Context.callLaunch(intent: Intent) {
        val iam = IActivityManager.Stub.asInterface(ShizukuBinderWrapper(
            SystemServiceHelper.getSystemService(Context.ACTIVITY_SERVICE)))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            iam.broadcastIntentWithFeature(
                null, null, intent, null,
                null, 0, null, null, null,
                null, null, AppOpsManager.OP_NONE,
                null, false, false, 0
            )
        } else {
            @Suppress("DEPRECATION")
            iam.broadcastIntent(
                null, intent, null, null,
                0, null, null, null,
                AppOpsManager.OP_NONE, null, false, false, 0
            )
        }
    }
}

//interface ShizukuShellLaunchStrategy : ShizukuLaunchStrategy, CommandLaunchStrategy {
//    override suspend fun Context.tryLaunch(args: LaunchArgs): List<Throwable> {
//        return try {
//            val command = StringBuilder(makeEscapedCommand(args))
//
//            args.addToCommand(command)
//
//            @Suppress("DEPRECATION")
//            Shizuku.newProcess(arrayOf("sh", "-c", command.toString()), null, null).run {
//                waitForTimeout(1000, TimeUnit.MILLISECONDS)
//
//                val inputText = inputStream.bufferedReader().use { it.readText() }
//                val errorText = errorStream.bufferedReader().use { it.readText() }
//
//                Log.e("RootActivityLauncher", "Shizuku Command Output\n${inputText}")
//                Log.e("RootActivityLauncher", "Shizuku Error Output\n${errorText}")
//
//                if (exitValue() == 0 && errorText.isEmpty()) {
//                    listOf()
//                } else {
//                    listOf(Exception(errorText))
//                }
//            }
//        } catch (e: Exception) {
//            Log.e("RootActivityLauncher", "Failure to launch through Shizuku process.", e)
//            listOf(e)
//        }
//    }
//
//    override suspend fun Context.callLaunch(intent: Intent) {
//        throw IllegalAccessException("Not supported!")
//    }
//}

interface RootLaunchStrategy : CommandLaunchStrategy {
    override suspend fun Context.canRun(args: LaunchArgs): Boolean {
        return Shell.SU.available()
    }

    override suspend fun Context.tryLaunch(args: LaunchArgs): List<Throwable> {
        val command = StringBuilder(makeEscapedCommand(args))
        val errorOutput = mutableListOf<String>()

        args.addToCommand(command)

        val result = Shell.Pool.SU.run(command.toString(), null, errorOutput, false)

        return if (result == 0) listOf() else listOf(Exception(errorOutput.joinToString("\n")))
    }
}

interface IterativeLaunchStrategy : LaunchStrategy {
    fun extraFlags(): Int? {
        return null
    }

    override suspend fun Context.canRun(args: LaunchArgs): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && args.filters.isNotEmpty()
    }

    suspend fun Context.performLaunch(args: LaunchArgs, intent: Intent)

    override suspend fun Context.tryLaunch(args: LaunchArgs): List<Throwable> {
        val errors = mutableListOf<Throwable>()

        args.filters.forEach { filter ->
            try {
                val intent = Intent(args.intent)

                extraFlags()?.let { flags ->
                    intent.addFlags(flags)
                }

                intent.categories?.clear()
                intent.action = if (filter.countActions() > 0) filter.getAction(0) else Intent.ACTION_MAIN
                intent.data = if (filter.countDataSchemes() > 0) Uri.parse("${filter.getDataScheme(0)}://yes") else null
                filter.categoriesIterator()?.forEach { intent.addCategory(it) }

                performLaunch(args, intent)
                return listOf()
            } catch (e: Throwable) {
                Log.e("RootActivityLauncher", "Error with alternative filter", e)
                errors.add(e)
            }
        }

        return errors
    }
}

private fun LaunchArgs.addToCommand(command: StringBuilder) {
    command.append(" -a ${intent.action}")

    if (extras.isNotEmpty()) {
        extras.forEach {
            command.append(" --${it.safeType.shellArgName} \"${it.key}\" \"${it.value}\"")
        }
    }

    if (intent.categories?.isNotEmpty() == true) {
        intent.categories.forEach {
            command.append(" -c \"${it}\"")
        }
    }
}
