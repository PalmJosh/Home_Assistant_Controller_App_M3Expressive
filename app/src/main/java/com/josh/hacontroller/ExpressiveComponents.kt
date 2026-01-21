package com.josh.hacontroller

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable

// --- 1. THE EXPRESSIVE SHAPE TOKENS ---
// M3 Expressive favors "Extra Large" (28dp) to "Full" (Circle/Stadium)
val ExpressiveContainerShape = RoundedCornerShape(28.dp)
val ExpressiveElementShape = RoundedCornerShape(16.dp)

// --- 2. BOUNCY / ANIMATED CARD ---
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExpressiveCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null, // Logic is passed here
    shape: Shape = ExpressiveContainerShape,
    colors: CardColors = CardDefaults.cardColors(),
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptics = LocalHapticFeedback.current

    // Spring animation for the "Squish" effect
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "scale"
    )

    Card(
        modifier = modifier
            .scale(scale)
            .clip(shape)
            // FIX: Use combinedClickable instead of clickable
            .combinedClickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
                onLongClick = {
                    if (onLongClick != null) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongClick()
                    }
                }
            ),
        shape = shape,
        colors = colors
    ) {
        content()
    }
}

// --- 3. WIDE EXPRESSIVE SLIDER ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpressiveSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (() -> Unit)? = null,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    modifier: Modifier = Modifier
) {
    // The "Expressive" slider is taller and looks like a pill.
    // We customize the Track to be tall.

    Slider(
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        valueRange = valueRange,
        modifier = modifier,
        thumb = {
            // M3 Expressive often uses a vertical bar or a larger thumb inside the track.
            // For simplicity, we use the standard thumb but let it float inside the thick track.
            SliderDefaults.Thumb(
                interactionSource = remember { MutableInteractionSource() },
                colors = SliderDefaults.colors(thumbColor = Color.White),
                modifier = Modifier.scale(1.2f) // Slightly larger thumb
            )
        },
        track = { sliderState ->
            SliderDefaults.Track(
                sliderState = sliderState,
                modifier = Modifier.height(24.dp), // WIDER TRACK
                colors = SliderDefaults.colors(
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                ),
                thumbTrackGapSize = 0.dp // Seamless
            )
        }
    )
}