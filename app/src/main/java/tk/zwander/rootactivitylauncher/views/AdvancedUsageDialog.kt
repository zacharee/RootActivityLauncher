package tk.zwander.rootactivitylauncher.views

import android.content.Context
import android.view.LayoutInflater
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import tk.zwander.rootactivitylauncher.R
import tk.zwander.rootactivitylauncher.adapters.AdvancedSearchAdapter
import tk.zwander.rootactivitylauncher.databinding.AdvancedSearchDialogBinding

class AdvancedUsageDialog(context: Context) : MaterialAlertDialogBuilder(context) {
    private val binding = AdvancedSearchDialogBinding.inflate(LayoutInflater.from(context))
    private val adapter = AdvancedSearchAdapter()

    init {
        setView(binding.root)
        binding.advancedSearchRecycler.adapter = adapter

        setTitle(R.string.usage_advanced_search)
        setPositiveButton(android.R.string.ok, null)
    }
}