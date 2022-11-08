package tk.zwander.rootactivitylauncher.views.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tk.zwander.patreonsupportersretrieval.data.SupporterInfo
import tk.zwander.patreonsupportersretrieval.util.DataParser
import tk.zwander.rootactivitylauncher.R
import tk.zwander.rootactivitylauncher.util.launchUrl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatreonSupportersDialog(
    showing: Boolean,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val supporters = remember {
        mutableStateListOf<SupporterInfo>()
    }

    if (showing) {
        LaunchedEffect(null) {
            supporters.clear()
            supporters.addAll(DataParser.getInstance(context).parseSupporters())
        }

        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = {
                Text(text = stringResource(id = R.string.supporters))
            },
            confirmButton = {
                TextButton(onClick = onDismissRequest) {
                    Text(text = stringResource(id = android.R.string.ok))
                }
            },
            text = {
                Column {
                    Text(text = stringResource(id = R.string.supporters_desc))

                    Spacer(modifier = Modifier.size(8.dp))

                    LazyColumn {
                        items(items = supporters, key = { it }) {
                            OutlinedCard(
                                onClick = {
                                    context.launchUrl(it.link)
                                },
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = it.name)
                                }
                            }
                        }
                    }
                }
            }
        )
    }
}
