package tk.zwander.rootactivitylauncher.views

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import tk.zwander.rootactivitylauncher.R
import tk.zwander.rootactivitylauncher.data.EnabledFilterMode
import tk.zwander.rootactivitylauncher.data.ExportedFilterMode
import tk.zwander.rootactivitylauncher.databinding.FilterDialogBinding

class FilterDialog(
    context: Context,
    private var enabledMode: EnabledFilterMode,
    private var exportedMode: ExportedFilterMode,
    onConfirmListener: (enabledMode: EnabledFilterMode, exportedMode: ExportedFilterMode) -> Unit
) : MaterialAlertDialogBuilder(context) {
    @SuppressLint("InflateParams")
    private val view = LayoutInflater.from(context).inflate(R.layout.filter_dialog, null)
    private val binding = FilterDialogBinding.bind(view)

    init {
        setTitle(R.string.filter)
        setView(view)

        setNegativeButton(android.R.string.cancel, null)
        setPositiveButton(android.R.string.ok) { _, _ ->
            onConfirmListener(enabledMode, exportedMode)
        }

        binding.enabledGroup.check(enabledMode.id)
        binding.enabledGroup.setOnCheckedChangeListener { _, checkedId ->
            enabledMode = when (checkedId) {
                R.id.filter_enabled -> EnabledFilterMode.SHOW_ENABLED
                R.id.filter_disabled -> EnabledFilterMode.SHOW_DISABLED
                else -> EnabledFilterMode.SHOW_ALL
            }
        }

        binding.exportedGroup.check(exportedMode.id)
        binding.exportedGroup.setOnCheckedChangeListener { _, checkedId ->
            exportedMode = when (checkedId) {
                R.id.filter_exported -> ExportedFilterMode.SHOW_EXPORTED
                R.id.filter_unexported -> ExportedFilterMode.SHOW_UNEXPORTED
                else -> ExportedFilterMode.SHOW_ALL
            }
        }
    }
}