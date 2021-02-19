package tk.zwander.rootactivitylauncher.data.component

import android.content.pm.ActivityInfo

data class ReceiverInfo(
        override val info: ActivityInfo,
        override val label: CharSequence
) : BaseComponentInfo(info, label)