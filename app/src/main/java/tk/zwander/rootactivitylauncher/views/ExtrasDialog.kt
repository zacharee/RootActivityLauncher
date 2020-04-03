package tk.zwander.rootactivitylauncher.views

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.extras_dialog.view.*
import tk.zwander.rootactivitylauncher.R
import tk.zwander.rootactivitylauncher.adapters.ExtrasDialogAdapter
import tk.zwander.rootactivitylauncher.util.findExtrasForActivity
import tk.zwander.rootactivitylauncher.util.updateExtrasForActivity


class ExtrasDialog(context: Context, activityKey: String) : MaterialAlertDialogBuilder(context) {
    private val view: View = LayoutInflater.from(context).inflate(R.layout.extras_dialog, null)
    private val adapter = ExtrasDialogAdapter()

    init {
        setView(view)

        adapter.setItems(context.findExtrasForActivity(activityKey))
        view.list.adapter = adapter

        setTitle(R.string.extras)
        setNegativeButton(android.R.string.cancel, null)
        setPositiveButton(android.R.string.ok) { _, _ ->
            val newData = adapter.currentData()

            context.updateExtrasForActivity(activityKey, newData)
        }
    }
}