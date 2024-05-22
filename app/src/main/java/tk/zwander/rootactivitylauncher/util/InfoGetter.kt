package tk.zwander.rootactivitylauncher.util

import android.content.Context
import android.os.IBinder
import android.os.UserHandle
import com.rosan.dhizuku.api.Dhizuku
import rikka.shizuku.Shizuku

interface BinderWrapper {
    suspend fun Context.getUidAndPackage(): Pair<Int, String?>
    suspend fun wrapBinder(binder: IBinder): IBinder
}

interface ShizukuBinderWrapper : BinderWrapper {
    override suspend fun Context.getUidAndPackage(): Pair<Int, String?> {
        val uid = try {
            Shizuku.getUid()
        } catch (e: IllegalStateException) {
            // It shouldn't be possible for the Shizuku Binder not to exist at this point, but sometimes it doesn't.
            // If that happens, just assume user 2000 (shell).
            2000
        }

        return UserHandle.getUserId(uid) to packageManager.getPackagesForUid(uid)?.firstOrNull()
    }

    override suspend fun wrapBinder(binder: IBinder): IBinder {
        return rikka.shizuku.ShizukuBinderWrapper(binder)
    }
}

interface DhizukuBinderWrapper : BinderWrapper {
    override suspend fun Context.getUidAndPackage(): Pair<Int, String?> {
        val packageName = "com.rosan.dhizuku"

        return UserHandle.getUserId(
            packageManager.getApplicationInfo(packageName, 0).uid
        ) to packageName
    }

    override suspend fun wrapBinder(binder: IBinder): IBinder {
        return Dhizuku.binderWrapper(binder)
    }
}
