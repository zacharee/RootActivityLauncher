package tk.zwander.rootactivitylauncher.views

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.hmomeni.progresscircula.ProgressCircula
import kotlin.math.roundToInt

@Composable
fun ScrimView(
    progress: Float?,
    modifier: Modifier = Modifier
) {
    var actualProgress by remember {
        mutableFloatStateOf(0f)
    }

    LaunchedEffect(progress) {
        if (progress != null) {
            actualProgress = progress
        }
    }

    AnimatedVisibility(
        visible = progress != null,
        modifier = modifier,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(
                    interactionSource = remember {
                        MutableInteractionSource()
                    },
                    indication = null,
                ) {},
            contentAlignment = Alignment.Center
        ) {
            val accentColor = MaterialTheme.colorScheme.primary
            val textColor = Color.White
            val rimWidth = with(LocalDensity.current) {
                8.dp.toPx()
            }

            AndroidView(
                factory = {
                    ProgressCircula(context = it).apply {
                        indeterminate = false
                        rimColor = accentColor.toArgb()
                        showProgress = true
                        speed = 0.5f
                        this.rimWidth = rimWidth
                        this.textColor = textColor.toArgb()
                    }
                },
                modifier = Modifier.size(200.dp),
                update = {
                    it.progress = (actualProgress * 100).roundToInt()
                }
            )
        }
    }
}
