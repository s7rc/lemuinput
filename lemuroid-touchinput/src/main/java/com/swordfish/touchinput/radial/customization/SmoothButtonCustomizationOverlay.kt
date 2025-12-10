package com.swordfish.touchinput.radial.customization

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swordfish.touchinput.radial.settings.TouchControllerSettingsManager

/**
 * SIMPLE slider-based per-button customization.
 * Tap a button to select it, then use sliders to adjust.
 */
@Composable
fun SmoothButtonCustomizationOverlay(
    settings: TouchControllerSettingsManager.Settings,
    buttonBounds: Map<String, Rect>,
    screenWidth: Int,
    screenHeight: Int,
    onSettingsChange: (TouchControllerSettingsManager.Settings) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedButtonId by remember { mutableStateOf<String?>(null) }
    
    val currentOverride = selectedButtonId?.let { settings.buttonOverrides[it] }
    val currentScale = currentOverride?.scale ?: settings.scale
    val currentOffsetX = currentOverride?.offsetX ?: 0f
    val currentOffsetY = currentOverride?.offsetY ?: 0f
    
    fun updateButtonScale(scale: Float) {
        selectedButtonId?.let { buttonId ->
            val override = settings.buttonOverrides[buttonId] ?: TouchControllerSettingsManager.ButtonOverride()
            val newOverride = override.copy(scale = scale)
            val newOverrides = settings.buttonOverrides.toMutableMap()
            newOverrides[buttonId] = newOverride
            onSettingsChange(settings.copy(buttonOverrides = newOverrides))
        }
    }
    
    fun updateButtonOffsetX(offset: Float) {
        selectedButtonId?.let { buttonId ->
            val override = settings.buttonOverrides[buttonId] ?: TouchControllerSettingsManager.ButtonOverride()
            val newOverride = override.copy(offsetX = offset)
            val newOverrides = settings.buttonOverrides.toMutableMap()
            newOverrides[buttonId] = newOverride
            onSettingsChange(settings.copy(buttonOverrides = newOverrides))
        }
    }
    
    fun updateButtonOffsetY(offset: Float) {
        selectedButtonId?.let { buttonId ->
            val override = settings.buttonOverrides[buttonId] ?: TouchControllerSettingsManager.ButtonOverride()
            val newOverride = override.copy(offsetY = offset)
            val newOverrides = settings.buttonOverrides.toMutableMap()
            newOverrides[buttonId] = newOverride
            onSettingsChange(settings.copy(buttonOverrides = newOverrides))
        }
    }
    
    fun resetSelectedButton() {
        selectedButtonId?.let { buttonId ->
            val newOverrides = settings.buttonOverrides.toMutableMap()
            newOverrides.remove(buttonId)
            onSettingsChange(settings.copy(buttonOverrides = newOverrides))
        }
    }
    
    fun resetAllButtons() {
        onSettingsChange(settings.copy(buttonOverrides = emptyMap()))
        selectedButtonId = null
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        // Clickable overlay to select buttons
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
                .clickable(enabled = false) { }
        ) {
            // Show selection indicator
            selectedButtonId?.let { buttonId ->
                buttonBounds[buttonId]?.let { rect ->
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val path = Path().apply { addRect(rect) }
                        drawPath(
                            path = path,
                            color = Color(0xFF00BCD4),
                            style = Stroke(
                                width = 4.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f),
                                cap = StrokeCap.Round
                            )
                        )
                        
                        val handleSize = 12.dp.toPx()
                        listOf(rect.topLeft, rect.topRight, rect.bottomLeft, rect.bottomRight).forEach { corner ->
                            drawCircle(color = Color(0xFF00BCD4), radius = handleSize / 2, center = corner)
                            drawCircle(color = Color.White, radius = (handleSize / 2) - 2.dp.toPx(), center = corner)
                        }
                    }
                }
            }
        }
        
        // Control panel
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (selectedButtonId == null) {
                    Text(
                        "Tap any button to customize it",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    Text(
                        "Adjust Selected Button",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Scale slider
                    Text("Scale: ${String.format("%.2f", currentScale)}x")
                    Slider(
                        value = currentScale,
                        onValueChange = { updateButtonScale(it) },
                        valueRange = 0.3f..3.0f,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Horizontal position slider
                    Text("Horizontal: ${String.format("%.2f", currentOffsetX)}")
                    Slider(
                        value = currentOffsetX,
                        onValueChange = { updateButtonOffsetX(it) },
                        valueRange = -2f..2f,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Vertical position slider
                    Text("Vertical: ${String.format("%.2f", currentOffsetY)}")
                    Slider(
                        value = currentOffsetY,
                        onValueChange = { updateButtonOffsetY(it) },
                        valueRange = -2f..2f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { resetSelectedButton() },
                        enabled = selectedButtonId != null
                    ) {
                        Text("Reset Button")
                    }
                    
                    Button(onClick = { resetAllButtons() }) {
                        Text("Reset All")
                    }
                    
                    Button(onClick = onDone) {
                        Text("Done")
                    }
                }
            }
        }
        
        // Tap detection for button selection
        buttonBounds.forEach { (buttonId, rect) ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable {
                        selectedButtonId = buttonId
                    }
            )
        }
    }
}
