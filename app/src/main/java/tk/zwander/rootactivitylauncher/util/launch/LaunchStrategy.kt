package tk.zwander.rootactivitylauncher.util.launch

import android.content.Context
import android.content.Intent
import android.util.Log
import eu.chainfire.libsuperuser.Shell
import rikka.shizuku.Shizuku
import tk.zwander.rootactivitylauncher.util.hasShizukuPermission
import tk.zwander.rootactivitylauncher.util.requestShizukuPermission
import java.util.concurrent.TimeUnit

interface LaunchStrategy {
    suspend fun Context.canRun(): Boolean = true
    suspend fun Context.tryLaunch(args: LaunchArgs): Throwable?
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
    override suspend fun Context.tryLaunch(args: LaunchArgs): Throwable? {
        return try {
            val command = StringBuilder(makeCommand(args))

            args.addToCommand(command)

            Shizuku.newProcess(arrayOf("sh", "-c", command.toString()), null, null).run {
                waitForTimeout(1000, TimeUnit.MILLISECONDS)

                Log.e("RootActivityLauncher", "Shizuku Command Output\n${inputStream.bufferedReader().use { it.readText() }}")
                Log.e("RootActivityLauncher", "Shizuku Error Output\n${errorStream.bufferedReader().use { it.readText() }}")

                if (exitValue() == 0) {
                    null
                } else {
                    Exception(errorStream.bufferedReader().use { it.readText() })
                }
            }
        } catch (e: Exception) {
            Log.e("RootActivityLauncher", "Failure to launch through Shizuku process.", e)
            e
        }
    }
}

interface RootLaunchStrategy : CommandLaunchStrategy {
    override suspend fun Context.canRun(): Boolean {
        return Shell.SU.available()
    }

    override suspend fun Context.tryLaunch(args: LaunchArgs): Throwable? {
        val command = StringBuilder(makeCommand(args))
        val errorOutput = mutableListOf<String>()

        args.addToCommand(command)

        val result = Shell.Pool.SU.run(command.toString(), null, errorOutput, false)

        return if (result == 0) null else Exception(errorOutput.joinToString("\n"))
    }
}

interface IterativeLaunchStrategy : LaunchStrategy {
    fun extraFlags(): Int? {
        return null
    }

    override suspend fun Context.tryLaunch(args: LaunchArgs): Throwable? {
        var latestError: Throwable? = null

        args.filters.forEach { filter ->
            try {
                val intent = Intent(args.intent)

                extraFlags()?.let { flags ->
                    intent.addFlags(flags)
                }

                intent.categories?.clear()
                intent.action = if (filter.countActions() > 0) filter.getAction(0) else Intent.ACTION_MAIN
                filter.categoriesIterator().forEach { intent.addCategory(it) }

                startActivity(intent)
                return null
            } catch (e: Throwable) {
                Log.e("RootActivityLauncher", "Error with alternative filter", e)
                latestError = e
            }
        }

        return latestError
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
