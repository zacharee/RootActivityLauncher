package tk.zwander.rootactivitylauncher.data

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ExtraInfo(
    var key: String,
    var value: String
) : Parcelable