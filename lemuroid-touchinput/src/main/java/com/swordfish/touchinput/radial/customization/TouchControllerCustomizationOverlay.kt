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
    var isResizing by remember { mutableStateOf(false) }
    var longPressDetected by remember { mutableStateOf(false) }
    
    // Accumulate drag offset locally to avoid recomposition glitches
    var accumulatedOffsetX by remember { mutableStateOf(0f) }
    var accumulatedOffsetY by remember { mutableStateOf(0f) }
    var accumulatedScale by remember { mutableStateOf(0f) }

    // Find which button was tapped
    fun findButtonAt(offset: Offset): String? {
        return buttonBounds.entries.findLast { (_, rect) ->
            rect.contains(offset)
        }?.key
    }

    // Save position changes to settings
    fun savePosition(buttonId: String) {
        if (accumulatedOffsetX == 0f && accumulatedOffsetY == 0f) return
        
        val currentOverride = settings.buttonOverrides[buttonId] ?: 
            TouchControllerSettingsManager.ButtonOverride()
        
        // Convert pixels to normalized (-1.0 to 1.0 range)
        val deltaX = accumulatedOffsetX / (screenWidth / 2f)
        val deltaY = accumulatedOffsetY / (screenHeight / 2f)
        
        val newOverride = currentOverride.copy(
            offsetX = (currentOverride.offsetX + deltaX).coerceIn(-2f, 2f),
            offsetY = (currentOverride.offsetY + deltaY).coerceIn(-2f, 2f)
        )
        
        val newOverrides = settings.buttonOverrides.toMutableMap()
        newOverrides[buttonId] = newOverride
        onSettingsChange(settings.copy(buttonOverrides = newOverrides))
        
        // Reset accumulator
        accumulatedOffsetX = 0f
        accumulatedOffsetY = 0f
    }

    // Save scale changes to settings
    fun saveScale(buttonId: String) {
        if (accumulatedScale == 0f) return
        
        val currentOverride = settings.buttonOverrides[buttonId] ?: 
            TouchControllerSettingsManager.ButtonOverride()
        
        val currentScale = currentOverride.scale ?: settings.scale
        val newScale = (currentScale + accumulatedScale).coerceIn(0.3f, 3.0f)
        
        val newOverride = currentOverride.copy(scale = newScale)
        val newOverrides = settings.buttonOverrides.toMutableMap()
        newOverrides[buttonId] = newOverride
        onSettingsChange(settings.copy(buttonOverrides = newOverrides))
        
        // Reset accumulator
        accumulatedScale = 0f
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
        // Main gesture detection layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { offset ->
                            val buttonId = findButtonAt(offset)
                            if (buttonId != null) {
                                selectedButtonId = buttonId
                                longPressDetected = true
                            }
                        },
                        onTap = { offset ->
                            if (!isDragging && !isResizing) {
                                selectedButtonId = findButtonAt(offset)
                                longPressDetected = false
                                accumulatedOffsetX = 0f
                                accumulatedOffsetY = 0f
                                accumulatedScale = 0f
                            }
                        }
                    )
                }
                .pointerInput(selectedButtonId) {
                    detectDragGestures(
                        onDragStart = {
                            if (longPressDetected && selectedButtonId != null) {
                                isDragging = true
                                isResizing = false
                            } else if (selectedButtonId != null) {
                                isResizing = true
                                isDragging = false
                            }
                        },
                        onDragEnd = {
                            // Save accumulated changes on drag end
                            selectedButtonId?.let { buttonId ->
                                if (isDragging) {
                                    savePosition(buttonId)
                                } else if (isResizing) {
                                    saveScale(buttonId)
                                }
                            }
                            isDragging = false
                            isResizing = false
                            longPressDetected = false
                        },
                        onDragCancel = {
                            // Reset on cancel
                            accumulatedOffsetX = 0f
                            accumulatedOffsetY = 0f
                            accumulatedScale = 0f
                            isDragging = false
                            isResizing = false
                            longPressDetected = false
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        
                        if (isDragging && selectedButtonId != null) {
                            // Accumulate movement
                            accumulatedOffsetX += dragAmount.x
                            accumulatedOffsetY += dragAmount.y
                        } else if (isResizing && selectedButtonId != null) {
                            // Accumulate scale (vertical drag only)
                            if (abs(dragAmount.y) > abs(dragAmount.x)) {
                                // 100px = 0.5x scale change
                                accumulatedScale += -dragAmount.y / 200f
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
                        
                        // Draw dashed border
                        drawPath(
                            path = path,
                            color = Color(0xFF00BCD4),
                            style = Stroke(
                                width = 4.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f),
                                cap = StrokeCap.Round
                            )
                        )
                        
                        // Draw corner handles
                        val handleSize = 12.dp.toPx()
                        val corners = listOf(rect.topLeft, rect.topRight, rect.bottomLeft, rect.bottomRight)
                        
                        corners.forEach { corner ->
                            drawCircle(color = Color(0xFF00BCD4), radius = handleSize / 2, center = corner)
                            drawCircle(color = Color.White, radius = (handleSize / 2) - 2.dp.toPx(), center = corner)
                        }
                    }
                    
                    // Show info
                    val override = settings.buttonOverrides[buttonId]
                    val currentScale = override?.scale ?: settings.scale
                    val displayScale = currentScale + accumulatedScale
                    
                    Box(modifier = Modifier.align(Alignment.TopCenter).padding(top = 80.dp)) {
                        Card {
                            Text(
                                text = when {
                                    isDragging -> "Moving... (${String.format("%.0f", accumulatedOffsetX)}px, ${String.format("%.0f", accumulatedOffsetY)}px)"
                                    isResizing -> "Resizing... ${String.format("%.2f", displayScale)}x"
                                    else -> "Scale: ${String.format("%.2f", currentScale)}x\n${if (longPressDetected) "Drag to move" else "Long press to move • Drag to resize"}"
                                },
                                modifier = Modifier.padding(12.dp),
                                fontSize = 14.sp,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
            
            // Show hint
            if (selectedButtonId == null) {
                Box(modifier = Modifier.align(Alignment.Center).padding(16.dp)) {
                    Card {
                        Text(
                            text = "Tap any button to customize\n\nLong press + drag to move\nDrag up/down to resize",
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
                modifier = Modifier.fillMaxWidth().padding(8.dp),
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
