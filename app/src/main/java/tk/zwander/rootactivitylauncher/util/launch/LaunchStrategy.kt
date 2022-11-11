package tk.zwander.rootactivitylauncher.util.launch

import android.content.Context
import android.util.Log
import eu.chainfire.libsuperuser.Shell
import rikka.shizuku.Shizuku
import tk.zwander.rootactivitylauncher.util.hasShizukuPermission
import tk.zwander.rootactivitylauncher.util.requestShizukuPermission
import java.util.concurrent.TimeUnit

interface LaunchStrategy {
    suspend fun Context.canRun(): Boolean = true
    suspend fun Context.tryLaunch(args: LaunchArgs): Boolean
}

interface CommandLaunchStrategy : LaunchStrategy {
    fun makeCommand(args: LaunchArgs): String
}

interface ShizukuLaunchStrategy : LaunchStrategy {
    override suspend fun Context.canRun(): Boolean {
        return Shizuku.pingBinder() &&
                (hasShizukuPermission || requestShizukuPermission())
    }
}

interface ShizukuShellLaunchStrategy : ShizukuLaunchStrategy, CommandLaunchStrategy {
    override suspend fun Context.tryLaunch(args: LaunchArgs): Boolean {
        return try {
            val command = StringBuilder(makeCommand(args))

            args.addToCommand(command)

            Shizuku.newProcess(arrayOf("sh", "-c", command.toString()), null, null).run {
                waitForTimeout(1000, TimeUnit.MILLISECONDS)

                Log.e("RootActivityLauncher", "Shizuku Command Output\n${inputStream.bufferedReader().use { it.readLines().joinToString("\n") }}")
                Log.e("RootActivityLauncher", "Shizuku Error Output\n${errorStream.bufferedReader().use { it.readLines().joinToString("\n") }}")

                exitValue() == 0
            }
        } catch (e: Exception) {
            Log.e("RootActivityLauncher", "Failure to launch through Shizuku process.", e)
            false
        }
    }
}

interface RootLaunchStrategy : CommandLaunchStrategy {
    override suspend fun Context.canRun(): Boolean {
        return Shell.SU.available()
    }

    override suspend fun Context.tryLaunch(args: LaunchArgs): Boolean {
        val command = StringBuilder(makeCommand(args))

        args.addToCommand(command)

        return Shell.Pool.SU.run(command.toString()) == 0
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
