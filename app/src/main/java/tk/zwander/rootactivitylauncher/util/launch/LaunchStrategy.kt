package tk.zwander.rootactivitylauncher.util.launch

import android.content.Context
import android.util.Log
import eu.chainfire.libsuperuser.Shell
import rikka.shizuku.Shizuku
import tk.zwander.rootactivitylauncher.util.hasShizukuPermission
import java.util.concurrent.TimeUnit

interface LaunchStrategy {
    fun Context.canRun(): Boolean = true
    fun Context.tryLaunch(args: LaunchArgs): Boolean
}

interface CommandLaunchStrategy : LaunchStrategy {
    fun makeCommand(args: LaunchArgs): String
}

interface ShizukuLaunchStrategy : LaunchStrategy {
    override fun Context.canRun(): Boolean {
        return Shizuku.pingBinder() && hasShizukuPermission
    }
}

interface ShizukuShellLaunchStrategy : ShizukuLaunchStrategy, CommandLaunchStrategy {
    override fun Context.tryLaunch(args: LaunchArgs): Boolean {
        return try {
            val command = makeCommand(args)

            Shizuku.newProcess(arrayOf("sh", "-c", command), null, null).run {
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
    override fun Context.canRun(): Boolean {
        return Shell.SU.available()
    }

    override fun Context.tryLaunch(args: LaunchArgs): Boolean {
        return Shell.Pool.SU.run(makeCommand(args)) == 0
    }
}