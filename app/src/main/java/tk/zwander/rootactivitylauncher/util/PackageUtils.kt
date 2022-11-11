package tk.zwander.rootactivitylauncher.util

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build

// Newer Android versions say these APIs are NonNull, but older versions can return null.

fun PackageManager.getInstalledPackagesCompat(flags: Int = 0): List<PackageInfo> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getInstalledPackages(PackageManager.PackageInfoFlags.of(flags.toLong()))
    } else {
        @Suppress("DEPRECATION")
        getInstalledPackages(flags)
    } ?: listOf()
}

fun PackageManager.getPackageInfoCompat(pkg: String, flags: Int = 0): PackageInfo {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(flags.toLong()))
    } else {
        @Suppress("DEPRECATION")
        getPackageInfo(pkg, flags)
    }
}
