package tk.zwander.rootactivitylauncher

import android.app.Application
import android.os.Build
import com.bugsnag.android.Bugsnag
import org.lsposed.hiddenapibypass.HiddenApiBypass

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        Bugsnag.start(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            HiddenApiBypass.setHiddenApiExemptions("L")
        }
    }
}