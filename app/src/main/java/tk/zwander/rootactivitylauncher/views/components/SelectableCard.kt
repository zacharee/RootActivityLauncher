package tk.zwander.rootactivitylauncher.views.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SelectableCard(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedColor: Color = MaterialTheme.colorScheme.primaryContainer,
    unselectedColor: Color = AlertDialogDefaults.containerColor,
    content: @Composable ColumnScope.() -> Unit,
) {
    val outlineColor = MaterialTheme.colorScheme.outline

    val color by animateColorAsState(
        targetValue = if (selected) {
            selectedColor
        } else {
            unselectedColor
        },
        label = "SelectableCardColor",
    )

    OutlinedCard(
        modifier = modifier,
        colors = CardDefaults.outlinedCardColors(
            containerColor = color,
        ),
        onClick = onClick,
        content = content,
        border = remember {
            BorderStroke(1.dp, outlineColor)
        }
    )
}
