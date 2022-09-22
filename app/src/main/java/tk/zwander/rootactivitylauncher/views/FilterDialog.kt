package tk.zwander.rootactivitylauncher.views

import android.content.Context
import android.view.LayoutInflater
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import tk.zwander.rootactivitylauncher.R
import tk.zwander.rootactivitylauncher.data.EnabledFilterMode
import tk.zwander.rootactivitylauncher.data.ExportedFilterMode
import tk.zwander.rootactivitylauncher.data.PermissionFilterMode
import tk.zwander.rootactivitylauncher.databinding.FilterDialogBinding

class FilterDialog(
    context: Context,
    private var enabledMode: EnabledFilterMode,
    private var exportedMode: ExportedFilterMode,
    private var permissionMode: PermissionFilterMode,
    onConfirmListener: (enabledMode: EnabledFilterMode, exportedMode: ExportedFilterMode, permissionMode: PermissionFilterMode) -> Unit
) : MaterialAlertDialogBuilder(context) {
    private val binding = FilterDialogBinding.inflate(LayoutInflater.from(context))

    init {
        setTitle(R.string.filter)
        setView(binding.root)

        setNegativeButton(android.R.string.cancel, null)
        setPositiveButton(android.R.string.ok) { _, _ ->
            onConfirmListener(enabledMode, exportedMode, permissionMode)
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

        binding.permissionGroup.check(permissionMode.id)
        binding.permissionGroup.setOnCheckedChangeListener { _, checkedId ->
            permissionMode = when (checkedId) {
                R.id.filter_requires_permission -> PermissionFilterMode.SHOW_REQUIRES_PERMISSION
                R.id.filter_requires_no_permission -> PermissionFilterMode.SHOW_REQUIRES_NO_PERMISSION
                else -> PermissionFilterMode.SHOW_ALL
            }
        }
    }
}