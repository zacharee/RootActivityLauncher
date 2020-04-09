package tk.zwander.rootactivitylauncher.views

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.ComponentInfo
import android.content.pm.ServiceInfo
import android.text.Html
import android.text.Spanned
import android.util.PrintWriterPrinter
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

        val message = when (info) {
            is ActivityInfo -> processActivityInfo(info)
            is ServiceInfo -> processServiceInfo(info)
            else -> null
        }

        setMessage(message)
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

    fun processServiceInfo(info: ServiceInfo): Spanned {
        val sWriter = StringWriter()
        val pWriter = PrintWriter(sWriter)
        val printer = PrintWriterPrinter(pWriter)

        info.dump(printer, "")

        val string = formatDump(sWriter.toString())

        sWriter.close()
        pWriter.close()

        return Html.fromHtml(
            string
        )
    }

    fun processActivityInfo(info: ActivityInfo): Spanned {
        val sWriter = StringWriter()
        val pWriter = PrintWriter(sWriter)
        val printer = PrintWriterPrinter(pWriter)

        info.dump(printer, "")

        val string = formatDump(sWriter.toString())

        sWriter.close()
        pWriter.close()

        return Html.fromHtml(
            string
        )
    }

    private fun formatDump(dump: String): String {
        val string = StringBuilder()
        dump.lines().apply {
            forEachIndexed { index, it ->
                val startsWithTwoSpaces = it.startsWith("  ")
                val formatted = it
                    .replace("  ", "&nbsp;&nbsp;")
                    .replace(", ", ",&nbsp;")
                    .replace(Regex(" "), "<br />${if (startsWithTwoSpaces) "&nbsp;&nbsp;" else ""}<b>")
                    .replace(Regex("(\r\n|\n)"), "<br />")
                    .replace(Regex("^&nbsp;&nbsp;"), "&nbsp;&nbsp;<b>")
                    .replaceFirst(Regex("^(?!&nbsp;&nbsp;)"), "<b>")
                    .replace("=", "</b>=")
                    .replace(Regex(":$"), "</b>:")

                when {
                    index == lastIndex -> string.append(formatted)
                    formatted.indexOf(":") == formatted.lastIndex -> string.append("$formatted<br />")
                    else -> string.appendHtmlLn(formatted)
                }
            }
        }
        return string.toString()
    }

    private fun StringBuilder.appendHtmlLn(line: Any?): StringBuilder {
        return append("$line<br><br>")
    }
}