package tk.zwander.rootactivitylauncher.views

import android.content.Context
import android.view.LayoutInflater
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import tk.zwander.rootactivitylauncher.R
import tk.zwander.rootactivitylauncher.adapters.ExtrasDialogAdapter
import tk.zwander.rootactivitylauncher.databinding.ExtrasDialogBinding
import tk.zwander.rootactivitylauncher.util.*

class ExtrasDialog(context: Context, componentKey: String) : MaterialAlertDialogBuilder(context) {
    private val adapter = ExtrasDialogAdapter()
    private val binding = ExtrasDialogBinding.inflate(LayoutInflater.from(context))

    init {
        setView(binding.root)

        adapter.setItems(context.findExtrasForComponent(componentKey))
        binding.action.setText(context.findActionForComponent(componentKey))
        binding.data.setText(context.findDataForComponent(componentKey))
        binding.categories.setText(context.findCategoriesForComponent(componentKey).joinToString("\n"))
        binding.list.adapter = adapter

        setTitle(R.string.extras)
        setNegativeButton(android.R.string.cancel, null)
        setPositiveButton(android.R.string.ok) { _, _ ->
            val newData = adapter.currentData().filterNot { it.key.isBlank() }

            context.updateExtrasForComponent(componentKey, newData)
            context.updateActionForComponent(componentKey, binding.action.text?.toString())
            context.updateDataForComponent(componentKey, binding.data.text?.toString())
            context.updateCategoriesForComponent(componentKey, binding.categories.text?.toString()?.split("\n") ?: listOf())
        }
    }
}