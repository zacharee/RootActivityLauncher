package tk.zwander.rootactivitylauncher.views

import android.content.Context
import android.view.LayoutInflater
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.filter_dialog.view.*
import tk.zwander.rootactivitylauncher.R
import tk.zwander.rootactivitylauncher.data.EnabledFilterMode
import tk.zwander.rootactivitylauncher.data.ExportedFilterMode

class FilterDialog(
    context: Context,
    private var enabledMode: EnabledFilterMode,
    private var exportedMode: ExportedFilterMode,
    onConfirmListener: (enabledMode: EnabledFilterMode, exportedMode: ExportedFilterMode) -> Unit
) : MaterialAlertDialogBuilder(context) {
    private val view = LayoutInflater.from(context).inflate(R.layout.filter_dialog, null)

    init {
        setTitle(R.string.filter)
        setView(view)

        setNegativeButton(android.R.string.cancel, null)
        setPositiveButton(android.R.string.ok) { _, _ ->
            onConfirmListener(enabledMode, exportedMode)
        }

        view.enabled_group.check(enabledMode.id)
        view.enabled_group.setOnCheckedChangeListener { _, checkedId ->
            enabledMode = when (checkedId) {
                R.id.filter_enabled -> EnabledFilterMode.SHOW_ENABLED
                R.id.filter_disabled -> EnabledFilterMode.SHOW_DISABLED
                else -> EnabledFilterMode.SHOW_ALL
            }
        }

        view.exported_group.check(exportedMode.id)
        view.exported_group.setOnCheckedChangeListener { _, checkedId ->
            exportedMode = when (checkedId) {
                R.id.filter_exported -> ExportedFilterMode.SHOW_EXPORTED
                R.id.filter_unexported -> ExportedFilterMode.SHOW_UNEXPORTED
                else -> ExportedFilterMode.SHOW_ALL
            }
        }
    }
}