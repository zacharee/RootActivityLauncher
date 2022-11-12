package tk.zwander.rootactivitylauncher.data.model

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import tk.zwander.rootactivitylauncher.data.ExtraInfo
import tk.zwander.rootactivitylauncher.util.findActionForComponent
import tk.zwander.rootactivitylauncher.util.findCategoriesForComponent
import tk.zwander.rootactivitylauncher.util.findDataForComponent
import tk.zwander.rootactivitylauncher.util.findExtrasForComponent
import java.util.UUID

class ExtrasDialogModel(private val context: Context, private val componentKey: String) {
    val extras = MutableStateFlow<List<Pair<UUID, ExtraInfo>>>(
        ArrayList<Pair<UUID, ExtraInfo>>().apply {
            addAll(context.findExtrasForComponent(componentKey).map {
                UUID.randomUUID() to it
            })

            add(UUID.randomUUID() to ExtraInfo("", ""))
        }
    )

    val categories = MutableStateFlow<List<Pair<UUID, String?>>>(
        ArrayList<Pair<UUID, String?>>().apply {
            addAll(context.findCategoriesForComponent(componentKey).map {
                UUID.randomUUID() to it
            })

            add(UUID.randomUUID() to "")
        }
    )

    val action = MutableStateFlow(context.findActionForComponent(componentKey))
    val data = MutableStateFlow(context.findDataForComponent(componentKey))
}