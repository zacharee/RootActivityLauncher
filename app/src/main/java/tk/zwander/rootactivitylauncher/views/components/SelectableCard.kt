package tk.zwander.rootactivitylauncher.views.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RippleConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectableCard(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedColor: Color = MaterialTheme.colorScheme.primaryContainer,
    unselectedColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit,
) {
    val color by animateColorAsState(
        targetValue = if (selected) {
            selectedColor
        } else {
            unselectedColor
        },
        label = "SelectableCardColor",
    )

    val ripple = LocalRippleConfiguration.current

    CompositionLocalProvider(
        LocalRippleConfiguration provides RippleConfiguration(
            color = selectedColor,
            rippleAlpha = ripple.rippleAlpha,
        ),
    ) {
        OutlinedCard(
            modifier = modifier,
            colors = CardDefaults.outlinedCardColors(
                containerColor = color,
            ),
            onClick = onClick,
            content = content
        )
    }
}
