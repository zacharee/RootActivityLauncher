package tk.zwander.rootactivitylauncher.views.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tk.zwander.rootactivitylauncher.data.BaseOption

@Composable
fun <T : BaseOption> OptionGroup(
    name: String,
    modes: Array<T>,
    selectedMode: T,
    onModeSelected: (T) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = name,
            fontSize = 18.sp,
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            modes.forEach { mode ->
                SelectableCard(
                    selected = mode.labelRes == selectedMode.labelRes,
                    onClick = { onModeSelected(mode) },
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = stringResource(id = mode.labelRes))
                    }
                }
            }
        }
    }
}