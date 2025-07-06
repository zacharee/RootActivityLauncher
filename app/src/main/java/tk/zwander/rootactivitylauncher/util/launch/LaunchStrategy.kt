package tk.zwander.rootactivitylauncher.util.launch

import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.IActivityManager
import android.app.IApplicationThread
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.net.toUri
import com.rosan.dhizuku.api.Dhizuku
import eu.chainfire.libsuperuser.Shell
import rikka.shizuku.Shizuku
import rikka.shizuku.SystemServiceHelper
import tk.zwander.rootactivitylauncher.util.BinderWrapper
import tk.zwander.rootactivitylauncher.util.DhizukuBinderWrapper
import tk.zwander.rootactivitylauncher.util.DhizukuUtils
import tk.zwander.rootactivitylauncher.util.hasShizukuPermission
import tk.zwander.rootactivitylauncher.util.requestShizukuPermission

interface LaunchStrategy {
    val priority: Int
        get() = 0

    val labelRes: Int
    val descRes: Int

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

interface BinderWrapperLaunchStrategy : LaunchStrategy, BinderWrapper {
    suspend fun Context.callLaunch(intent: Intent)

    override suspend fun Context.tryLaunch(args: LaunchArgs): List<Throwable> {
        return try {
            callLaunch(args.intent)
            listOf()
        } catch (e: Throwable) {
            Log.e("RootActivityLauncher", "Failure to launch through ${this::class.java.name}", e)
            listOf(e)
        }
    }
}

interface ShizukuLaunchStrategy : BinderWrapperLaunchStrategy,
    tk.zwander.rootactivitylauncher.util.ShizukuBinderWrapper {
    override suspend fun Context.canRun(args: LaunchArgs): Boolean {
        return Shizuku.pingBinder() &&
                (hasShizukuPermission || requestShizukuPermission())
    }
}

interface DhizukuLaunchStrategy : BinderWrapperLaunchStrategy, DhizukuBinderWrapper {
    override suspend fun Context.canRun(args: LaunchArgs): Boolean {
        return Dhizuku.init(this) &&
                (Dhizuku.isPermissionGranted() || DhizukuUtils.requestDhizukuPermission())
    }
}

interface BinderActivityLaunchStrategy : BinderWrapperLaunchStrategy {
    override val priority: Int
        get() = 2

    override suspend fun Context.callLaunch(intent: Intent) {
        val iam = IActivityManager.Stub.asInterface(wrapBinder(
            SystemServiceHelper.getSystemService(Context.ACTIVITY_SERVICE)
        ))

        val (_, pkg) = getUidAndPackage()

        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            iam.startActivityWithFeature(
                null, pkg, null, intent,
                null, null, null, 0, 0,
                null, null
            )
        } else {
            @Suppress("DEPRECATION")
            iam.startActivity(
                null, pkg, intent,
                null, null, null, 0,
                0, null, null
            )
        }

        if (result != ActivityManager.START_SUCCESS) {
            throw Exception("Error starting Activity: $result")
        }
    }
}

interface BinderServiceLaunchStrategy : BinderWrapperLaunchStrategy {
    override val priority: Int
        get() = 2

    override suspend fun Context.callLaunch(intent: Intent) {
        val iam = IActivityManager.Stub.asInterface(wrapBinder(
            SystemServiceHelper.getSystemService(Context.ACTIVITY_SERVICE)))

        val (uid, pkg) = getUidAndPackage()

        val cn = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                iam.startService(
                    null, intent, null, false, pkg,
                    null, uid,
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
                    null, intent, null, false, pkg, uid,
                ) as? ComponentName
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                iam::class.java.getMethod(
                    "startService",
                    IApplicationThread::class.java, Intent::class.java,
                    String::class.java, String::class.java, Int::class.java
                ).invoke(
                    iam,
                    null, intent, null, pkg, uid,
                ) as? ComponentName
            }
            else -> {
                iam::class.java.getMethod(
                    "startService",
                    IApplicationThread::class.java, Intent::class.java,
                    String::class.java, Int::class.java
                ).invoke(
                    iam,
                    null, intent, null, uid,
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

interface BinderReceiverLaunchStrategy : BinderWrapperLaunchStrategy {
    override val priority: Int
        get() = 2

    override suspend fun Context.callLaunch(intent: Intent) {
        val iam = IActivityManager.Stub.asInterface(wrapBinder(
            SystemServiceHelper.getSystemService(Context.ACTIVITY_SERVICE)))

        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
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

        if (result != ActivityManager.START_SUCCESS) {
            throw Exception("Error starting Receiver: $result")
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
                intent.data = if (filter.countDataSchemes() > 0) "${filter.getDataScheme(0)}://yes".toUri() else null
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
