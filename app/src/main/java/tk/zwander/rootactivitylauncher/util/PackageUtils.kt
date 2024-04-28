package tk.zwander.rootactivitylauncher.util

import android.content.ComponentName
import android.content.Context
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import tk.zwander.rootactivitylauncher.data.model.AppModel
import java.io.File

// Newer Android versions say these APIs are NonNull, but older versions can return null.

fun PackageManager.getInstalledPackagesCompat(flags: Int = 0): List<PackageInfo> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getInstalledPackages(PackageManager.PackageInfoFlags.of(flags.toLong()))
    } else {
        getInstalledPackages(flags)
    } ?: listOf()
}

fun PackageManager.getPackageInfoCompat(pkg: String, flags: Int = 0): PackageInfo? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(flags.toLong()))
        } else {
            getPackageInfo(pkg, flags)
        }
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }
}

fun Context.extractApk(result: Uri, info: AppModel): List<Throwable> {
    val errors = ArrayList<Throwable>()

    val dir = DocumentFile.fromTreeUri(this, result)

    if (dir == null) {
        errors.add(Exception("Unable to get file reference for $result."))
    }

    val baseDir = File(info.info.sourceDir)

    val splits = info.info.splitSourceDirs?.mapIndexed { index, s ->
        val splitApk = File(s)
        val splitName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            info.info.splitNames[index]
        } else splitApk.nameWithoutExtension

        splitName to s
    }

    val baseFile = dir?.createFile(
        "application/vnd.android.package-archive",
        info.info.packageName
    )

    if (baseFile == null) {
        errors.add(Exception("Unable to create file ${info.info.packageName}."))
    } else {
        try {
            contentResolver.openOutputStream(baseFile.uri).use { writer ->
                baseDir.inputStream().use { reader ->
                    reader.copyTo(writer!!)
                }
            }
        } catch (e: Exception) {
            Log.e("RootActivityLauncher", "Extraction failed", e)
            errors.add(e)
        }
    }

    splits?.forEach { split ->
        val name = split.first
        val path = File(split.second)

        val file = dir?.createFile(
            "application/vnd.android.package-archive",
            "${info.info.packageName}_$name"
        )

        if (file == null) {
            errors.add(Exception("Unable to create file ${info.info.packageName}_$name"))
        } else {
            contentResolver.openOutputStream(file.uri).use { writer ->
                try {
                    path.inputStream().use { reader ->
                        reader.copyTo(writer!!)
                    }
                } catch (e: Exception) {
                    Log.e("RootActivityLauncher", "Extraction failed", e)
                    errors.add(e)
                }
            }
        }
    }

    return errors
}

fun PackageManager.getAllIntentFiltersCompat(packageName: String): List<IntentFilter> {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getAllIntentFilters(packageName)
        } else {
            listOf()
        }
    } catch (e: Throwable) {
        listOf()
    }
}

fun PackageManager.getActivityInfoCompat(
    componentName: ComponentName,
    flags: Int = 0
): ActivityInfo {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getActivityInfo(componentName, PackageManager.ComponentInfoFlags.of(flags.toLong()))
    } else {
        getActivityInfo(componentName, flags)
    }
}

fun PackageManager.getServiceInfoCompat(componentName: ComponentName, flags: Int = 0): ServiceInfo {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getServiceInfo(componentName, PackageManager.ComponentInfoFlags.of(flags.toLong()))
    } else {
        getServiceInfo(componentName, flags)
    }
}

fun PackageManager.getReceiverInfoCompat(
    componentName: ComponentName,
    flags: Int = 0
): ActivityInfo {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getReceiverInfo(componentName, PackageManager.ComponentInfoFlags.of(flags.toLong()))
    } else {
        getReceiverInfo(componentName, flags)
    }
}
