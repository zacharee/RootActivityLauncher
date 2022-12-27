package tk.zwander.rootactivitylauncher.util.launch

import android.content.Intent
import android.content.IntentFilter
import tk.zwander.rootactivitylauncher.data.ExtraInfo

data class LaunchArgs(val intent: Intent, val extras: List<ExtraInfo>, val filters: List<IntentFilter>)