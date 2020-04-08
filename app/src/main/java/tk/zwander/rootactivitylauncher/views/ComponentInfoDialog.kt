package tk.zwander.rootactivitylauncher.views

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.ComponentInfo
import android.content.pm.ServiceInfo
import android.util.Log
import android.util.PrintWriterPrinter
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textview.MaterialTextView
import tk.zwander.rootactivitylauncher.R
import java.io.PrintWriter
import java.io.StringWriter

class ComponentInfoDialog(context: Context, info: ComponentInfo) : MaterialAlertDialogBuilder(context) {
    init {
        setTitle(R.string.component_info)
        setPositiveButton(android.R.string.ok, null)

        val sWriter = StringWriter()
        val pWriter = PrintWriter(sWriter)
        val printer = PrintWriterPrinter(pWriter)

        if (info is ActivityInfo) info.dump(printer, "")
        if (info is ServiceInfo) info.dump(printer, "")

        setMessage(sWriter.toString())
        sWriter.close()
        pWriter.close()
    }

    override fun create(): AlertDialog {
        return super.create().apply {
            setOnShowListener {
                window?.findViewById<MaterialTextView>(android.R.id.message)?.apply {
                    setTextIsSelectable(true)
                }
            }
        }
    }
}