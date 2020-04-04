package tk.zwander.rootactivitylauncher.data.component

import android.content.pm.ActivityInfo
import kotlinx.coroutines.Deferred

data class ActivityInfo(
    override val info: ActivityInfo,
    override val label: Deferred<CharSequence>
) : BaseComponentInfo(info, label)