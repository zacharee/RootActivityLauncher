package tk.zwander.rootactivitylauncher.views.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
    modifier: Modifier = Modifier,
) {
    var showingAdvancedUsage by remember {
        mutableStateOf(false)
    }

    val focusRequester = remember { FocusRequester() }

    val softwareKeyboardController = LocalSoftwareKeyboardController.current

    Box(
        modifier = modifier,
        contentAlignment = Alignment.CenterEnd
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandHorizontally(expandFrom = Alignment.End),
            exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.End),
        ) {
            LaunchedEffect(null) {
                focusRequester.requestFocus()
            }

            TextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth()
                    .clearFocusOnKeyboardDismiss()
                    .focusRequester(focusRequester),
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
                                softwareKeyboardController?.hide()
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
                },
                keyboardOptions = KeyboardOptions.Default.copy(autoCorrectEnabled = false),
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

@OptIn(ExperimentalLayoutApi::class)
fun Modifier.clearFocusOnKeyboardDismiss(): Modifier = composed {
    var isFocused by remember { mutableStateOf(false) }
    var keyboardAppearedSinceLastFocused by remember { mutableStateOf(false) }
    if (isFocused) {
        val imeIsVisible = WindowInsets.isImeVisible
        val focusManager = LocalFocusManager.current
        LaunchedEffect(imeIsVisible) {
            if (imeIsVisible) {
                keyboardAppearedSinceLastFocused = true
            } else if (keyboardAppearedSinceLastFocused) {
                focusManager.clearFocus()
            }
        }
    }
    onFocusEvent {
        if (isFocused != it.isFocused) {
            isFocused = it.isFocused
            if (isFocused) {
                keyboardAppearedSinceLastFocused = false
            }
        }
    }
}
