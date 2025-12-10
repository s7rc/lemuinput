package com.swordfish.touchinput.radial.customization

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swordfish.touchinput.radial.settings.TouchControllerSettingsManager
import kotlin.math.abs

/**
 * Interactive overlay for customizing individual touch controller buttons.
 * 
 * Gestures:
 * - Tap: Select button
 * - Long press + drag: Move selected button
 * - Vertical swipe: Resize selected button (up = larger, down = smaller)
 */
@Composable
fun TouchControllerCustomizationOverlay(
    settings: TouchControllerSettingsManager.Settings,
    buttonBounds: Map<String, Rect>,
    screenWidth: Int,
    screenHeight: Int,
    onSettingsChange: (TouchControllerSettingsManager.Settings) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedButtonId by remember { mutableStateOf<String?>(null) }
    var isDragging by remember { mutableStateOf(false) }
    var dragStartOffset by remember { mutableStateOf(Offset.Zero) }
    var longPressDetected by remember { mutableStateOf(false) }

    val density = LocalDensity.current

    // Convert screen coordinates to normalized coordinates (-1.0 to 1.0)
    fun toNormalizedX(px: Float): Float = (px / screenWidth) * 2f - 1f
    fun toNormalizedY(px: Float): Float = (px / screenHeight) * 2f - 1f

    // Find which button was tapped
    fun findButtonAt(offset: Offset): String? {
        return buttonBounds.entries.findLast { (_, rect) ->
            rect.contains(offset)
        }?.key
    }

    // Update button position
    fun updateButtonPosition(buttonId: String, dragAmount: Offset) {
        val currentOverride = settings.buttonOverrides[buttonId] ?: 
            TouchControllerSettingsManager.ButtonOverride()
        
        // Convert drag pixels directly to normalized offset
        // Screen width maps to -1.0 to 1.0, so divide by half width
        val deltaX = dragAmount.x / (screenWidth / 2f)
        val deltaY = dragAmount.y / (screenHeight / 2f)
        
        val newOverride = currentOverride.copy(
            offsetX = (currentOverride.offsetX + deltaX).coerceIn(-2f, 2f),
            offsetY = (currentOverride.offsetY + deltaY).coerceIn(-2f, 2f)
        )
        
        val newOverrides = settings.buttonOverrides.toMutableMap()
        newOverrides[buttonId] = newOverride
        onSettingsChange(settings.copy(buttonOverrides = newOverrides))
    }

    // Update button scale
    fun updateButtonScale(buttonId: String, deltaY: Float) {
        val currentOverride = settings.buttonOverrides[buttonId] ?: 
            TouchControllerSettingsManager.ButtonOverride()
        
        val currentScale = currentOverride.scale ?: settings.scale
        // Make scaling more sensitive: 100px drag = 0.5x scale change
        val scaleChange = -deltaY / 200f
        val newScale = (currentScale + scaleChange).coerceIn(0.3f, 3.0f)
        
        val newOverride = currentOverride.copy(scale = newScale)
        val newOverrides = settings.buttonOverrides.toMutableMap()
        newOverrides[buttonId] = newOverride
        onSettingsChange(settings.copy(buttonOverrides = newOverrides))
    }

    // Reset selected button
    fun resetSelectedButton() {
        selectedButtonId?.let { buttonId ->
            val newOverrides = settings.buttonOverrides.toMutableMap()
            newOverrides.remove(buttonId)
            onSettingsChange(settings.copy(buttonOverrides = newOverrides))
            selectedButtonId = null
        }
    }

    // Reset all buttons
    fun resetAllButtons() {
        onSettingsChange(settings.copy(buttonOverrides = emptyMap()))
        selectedButtonId = null
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Main gesture detection layer - MUST consume all events
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f)) // Semi-transparent overlay
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { offset ->
                            // Long press to enter drag mode
                            val buttonId = findButtonAt(offset)
                            if (buttonId != null) {
                                selectedButtonId = buttonId
                                longPressDetected = true
                                dragStartOffset = offset
                            }
                        },
                        onTap = { offset ->
                            // Tap to select
                            if (!isDragging) {
                                selectedButtonId = findButtonAt(offset)
                                longPressDetected = false
                            }
                        }
                    )
                }
                .pointerInput(selectedButtonId, longPressDetected) {
                    detectDragGestures(
                        onDragStart = {
                            if (longPressDetected && selectedButtonId != null) {
                                isDragging = true
                            }
                        },
                        onDragEnd = {
                            isDragging = false
                            longPressDetected = false
                        },
                        onDragCancel = {
                            isDragging = false
                            longPressDetected = false
                        }
                    ) { change, dragAmount ->
                        change.consume() // CRITICAL: Consume all drag events
                        
                        if (isDragging && selectedButtonId != null) {
                            // Long press drag = move button
                            updateButtonPosition(selectedButtonId!!, dragAmount)
                        } else if (selectedButtonId != null && !longPressDetected) {
                            // Regular drag on selected button = resize (vertical only)
                            if (abs(dragAmount.y) > abs(dragAmount.x)) {
                                updateButtonScale(selectedButtonId!!, dragAmount.y)
                            }
                        }
                    }
                }
        ) {
            // Draw selection indicator
            selectedButtonId?.let { buttonId ->
                buttonBounds[buttonId]?.let { rect ->
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val path = Path().apply {
                            addRect(rect)
                        }
                        
                        // Draw dashed border around selected button
                        drawPath(
                            path = path,
                            color = Color(0xFF00BCD4), // Cyan highlight
                            style = Stroke(
                                width = 4.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f),
                                cap = StrokeCap.Round
                            )
                        )
                        
                        // Draw corner handles
                        val handleSize = 12.dp.toPx()
                        val corners = listOf(
                            rect.topLeft,
                            rect.topRight,
                            rect.bottomLeft,
                            rect.bottomRight
                        )
                        
                        corners.forEach { corner ->
                            drawCircle(
                                color = Color(0xFF00BCD4),
                                radius = handleSize / 2,
                                center = corner
                            )
                            drawCircle(
                                color = Color.White,
                                radius = (handleSize / 2) - 2.dp.toPx(),
                                center = corner
                            )
                        }
                    }
                    
                    // Show info text
                    val override = settings.buttonOverrides[buttonId]
                    val currentScale = override?.scale ?: settings.scale
                    
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 80.dp)
                    ) {
                        Card {
                            Text(
                                text = if (isDragging) "Moving..." else "Scale: ${String.format("%.2f", currentScale)}x\n${if (longPressDetected) "Release to place" else "Long press to move • Swipe ↕ to resize"}",
                                modifier = Modifier.padding(12.dp),
                                fontSize = 14.sp,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
            
            // Show hint if no button selected
            if (selectedButtonId == null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                ) {
                    Card {
                        Text(
                            text = "Tap any button to customize\n\nLong press + drag to move\nSwipe up/down to resize",
                            modifier = Modifier.padding(16.dp),
                            fontSize = 16.sp,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }

        // Top control bar
        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { resetSelectedButton() }, enabled = selectedButtonId != null) {
                    Text("Reset Button")
                }
                TextButton(onClick = { resetAllButtons() }) {
                    Text("Reset All")
                }
                TextButton(onClick = onDone) {
                    Text("Done")
                }
            }
        }
    }
}
