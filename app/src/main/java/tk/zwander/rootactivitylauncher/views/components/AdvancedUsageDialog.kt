package tk.zwander.rootactivitylauncher.views.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tk.zwander.rootactivitylauncher.R

private val items = listOf(
    R.string.usage_advanced_search_has_permission to R.string.usage_advanced_search_has_permission_desc,
    R.string.usage_advanced_search_declares_permission to R.string.usage_advanced_search_declares_permission_desc,
    R.string.usage_advanced_search_requires_permission to R.string.usage_advanced_search_requires_permission_desc,
    R.string.usage_advanced_search_requires_feature to R.string.usage_advanced_search_requires_feature_desc
)

@Composable
fun AdvancedUsageDialog(
    showing: Boolean,
    onDismissRequest: () -> Unit
) {
    if (showing) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = {
                Text(text = stringResource(id = R.string.usage_advanced_search))
            },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items = items, key = { it }) {
                        OutlinedCard {
                            Column(
                                modifier = Modifier.fillMaxWidth()
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = stringResource(id = it.first),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )

                                Text(
                                    text = stringResource(id = it.second)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismissRequest) {
                    Text(text = stringResource(id = android.R.string.ok))
                }
            }
        )
    }
}
