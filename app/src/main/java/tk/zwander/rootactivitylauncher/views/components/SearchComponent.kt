package tk.zwander.rootactivitylauncher.views.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import tk.zwander.rootactivitylauncher.R
import tk.zwander.rootactivitylauncher.views.dialogs.AdvancedUsageDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchComponent(
    expanded: Boolean,
    query: String,
    onExpandChange: (Boolean) -> Unit,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showingAdvancedUsage by remember {
        mutableStateOf(false)
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.CenterEnd
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandHorizontally(expandFrom = Alignment.End),
            exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.End)
        ) {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                ),
                leadingIcon = {
                    IconButton(
                        onClick = {
                            showingAdvancedUsage = true
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_baseline_help_outline_24),
                            contentDescription = stringResource(id = R.string.usage_advanced_search)
                        )
                    }
                },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            if (query.isNotBlank()) {
                                onQueryChange("")
                            } else {
                                onExpandChange(false)
                            }
                        }
                    ) {
                        Icon(
                            painter = rememberVectorPainter(Icons.Outlined.Close),
                            contentDescription = stringResource(
                                id = if (query.isNotBlank()) {
                                    R.string.clear
                                } else {
                                    R.string.close
                                }
                            )
                        )
                    }
                }
            )
        }

        AnimatedVisibility(
            visible = !expanded,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            IconButton(onClick = { onExpandChange(true) }) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(id = R.string.search)
                )
            }
        }
    }

    AdvancedUsageDialog(showing = showingAdvancedUsage) {
        showingAdvancedUsage = false
    }
}
