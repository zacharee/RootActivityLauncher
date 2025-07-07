package tk.zwander.rootactivitylauncher.views.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BaseAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: Shape = AlertDialogDefaults.shape,
    containerColor: Color = AlertDialogDefaults.containerColor,
    iconContentColor: Color = AlertDialogDefaults.iconContentColor,
    titleContentColor: Color = AlertDialogDefaults.titleContentColor,
    textContentColor: Color = AlertDialogDefaults.textContentColor,
    tonalElevation: Dp = AlertDialogDefaults.TonalElevation,
    properties: DialogProperties = DialogProperties(),
) {
    val actualProperties = DialogProperties(
        usePlatformDefaultWidth = false,
        decorFitsSystemWindows = properties.decorFitsSystemWindows,
        dismissOnBackPress = properties.dismissOnBackPress,
        dismissOnClickOutside = properties.dismissOnClickOutside,
        securePolicy = properties.securePolicy,
    )
    val actualModifier = modifier.fillMaxWidth(0.85f)

    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = actualModifier,
        properties = actualProperties,
    ) {
        Surface(
            shape = shape,
            color = containerColor,
            tonalElevation = tonalElevation,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
            ) {
                icon?.let {
                    CompositionLocalProvider(LocalContentColor provides iconContentColor) {
                        Box(Modifier.padding(bottom = 8.dp).align(Alignment.CenterHorizontally)) {
                            icon()
                        }
                    }
                }
                title?.let {
                    ProvideContentColorTextStyle(
                        contentColor = titleContentColor,
                        textStyle = MaterialTheme.typography.headlineSmall,
                    ) {
                        Box(
                            // Align the title to the center when an icon is present.
                            Modifier.padding(bottom = 8.dp)
                                .align(
                                    if (icon == null) {
                                        Alignment.Start
                                    } else {
                                        Alignment.CenterHorizontally
                                    }
                                ),
                        ) {
                            title()
                        }
                    }
                }
                text?.let {
                    val textStyle = MaterialTheme.typography.bodyMedium
                    ProvideContentColorTextStyle(
                        contentColor = textContentColor,
                        textStyle = textStyle,
                    ) {
                        Box(
                            Modifier.weight(weight = 1f, fill = false)
                                .padding(bottom = 8.dp)
                                .align(Alignment.Start),
                        ) {
                            text()
                        }
                    }
                }
                Box(modifier = Modifier.align(Alignment.End)) {
                    val textStyle = MaterialTheme.typography.labelLarge
                    ProvideContentColorTextStyle(
                        contentColor = MaterialTheme.colorScheme.primary,
                        textStyle = textStyle,
                        content = {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                dismissButton?.invoke()
                                confirmButton()
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ProvideContentColorTextStyle(
    contentColor: Color,
    textStyle: TextStyle,
    content: @Composable () -> Unit
) {
    val mergedStyle = LocalTextStyle.current.merge(textStyle)
    CompositionLocalProvider(
        LocalContentColor provides contentColor,
        LocalTextStyle provides mergedStyle,
        content = content
    )
}
