package tk.zwander.rootactivitylauncher.util

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import tk.zwander.rootactivitylauncher.R
import tk.zwander.rootactivitylauncher.data.model.AppModel
import java.io.File

fun Context.extractApk(result: Uri, info: AppModel) {
    val dir = DocumentFile.fromTreeUri(this, result) ?: return

    val baseDir = File(info.info.sourceDir)

    val splits = info.info.splitSourceDirs?.mapIndexed { index, s ->
        val splitApk = File(s)
        val splitName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            info.info.splitNames[index]
        } else splitApk.nameWithoutExtension

        splitName to s
    }

    val baseFile = dir.createFile(
        "application/vnd.android.package-archive",
        info.info.packageName
    ) ?: return
    contentResolver.openOutputStream(baseFile.uri).use { writer ->
        Log.e("RootActivityLauncher", "$baseDir")
        try {
            baseDir.inputStream().use { reader ->
                reader.copyTo(writer!!)
            }
        } catch (e: Exception) {
            Log.e("RootActivityLauncher", "Extraction failed", e)
            Toast.makeText(
                this,
                resources.getString(R.string.extraction_failed, e.message),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    splits?.forEach { split ->
        val name = split.first
        val path = File(split.second)

        val file = dir.createFile(
            "application/vnd.android.package-archive",
            "${info.info.packageName}_$name"
        ) ?: return
        contentResolver.openOutputStream(file.uri).use { writer ->
            try {
                path.inputStream().use { reader ->
                    reader.copyTo(writer!!)
                }
            } catch (e: Exception) {
                Log.e("RootActivityLauncher", "Extraction failed", e)
                Toast.makeText(
                    this,
                    resources.getString(R.string.extraction_failed, e.message),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
