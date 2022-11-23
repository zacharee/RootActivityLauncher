package tk.zwander.rootactivitylauncher.util

import android.content.pm.ApplicationInfo
import android.net.Uri
import com.android.internal.R
import tk.zwander.rootactivitylauncher.data.component.BaseComponentInfo

fun BaseComponentInfo.getCoilData(): Any {
    val res = info.iconResource

    return if (res != 0) {
        Uri.parse("android.resource://${info.packageName}/$res")
    } else {
        return info.applicationInfo.getCoilData()
    }
}

fun ApplicationInfo.getCoilData(): Any {
    val res = icon

    return if (res != 0) {
        Uri.parse("android.resource://${packageName}/$res")
    } else {
        Uri.parse("android.resource://android/${R.drawable.sym_def_app_icon}")
    }
}