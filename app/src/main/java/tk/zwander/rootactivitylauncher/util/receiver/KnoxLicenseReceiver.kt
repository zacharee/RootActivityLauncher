package tk.zwander.rootactivitylauncher.util.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class KnoxLicenseReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent) {
        Log.e("RootActivityLauncher", "${intent.action}")

        intent.extras?.keySet()?.forEach {
            @Suppress("DEPRECATION")
            Log.e("RootActivityLauncher", "$it : ${intent.extras?.get(it)}")
        }
    }
}