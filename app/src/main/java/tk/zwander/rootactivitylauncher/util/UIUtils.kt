package tk.zwander.rootactivitylauncher.util

import android.content.pm.ApplicationInfo
import android.net.Uri
import com.android.internal.R
import tk.zwander.rootactivitylauncher.data.component.BaseComponentInfo

fun BaseComponentInfo.getIconResourceId(): Pair<String, Int> {
    val res = info.iconResource

    return if (res != 0) {
        info.packageName to res
    } else {
        info.applicationInfo.getIconResourceId()
    }
}

fun ApplicationInfo.getIconResourceId(): Pair<String, Int> {
    val res = icon

    return if (res != 0) {
        packageName to res
    } else {
        "android" to R.drawable.sym_def_app_icon
    }
}

fun BaseComponentInfo.getCoilData(): Uri {
    val id = getIconResourceId()

    return Uri.parse("android.resource://${id.first}/${id.second}")
}

fun ApplicationInfo.getCoilData(): Uri {
    val id = getIconResourceId()

    return Uri.parse("android.resource://${id.first}/${id.second}")
}