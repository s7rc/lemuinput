package com.swordfish.touchinput.radial.customization

import android.annotation.SuppressLint
import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.math.MathUtils
import com.dinuscxj.gesture.MultiTouchGestureDetector
import com.swordfish.lemuroid.common.graphics.GraphicsUtils
import com.swordfish.touchinput.radial.settings.TouchControllerSettingsManager

/**
 * Smooth per-button customization using MultiTouchGestureDetector.
 * 
 * Usage:
 * - Tap a button to select it
 * - Pinch to scale selected button  
 * - Drag to move selected button
 */
@SuppressLint("ClickableViewAccessibility")
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
    var currentSettings by remember { mutableStateOf(settings) }
    
    val context = LocalContext.current
    val maxMargins = GraphicsUtils.convertDpToPixel(TouchControllerSettingsManager.MAX_MARGINS, context)
    
    // Find button at touch position
    fun findButtonAt(x: Float, y: Float): String? {
        return buttonBounds.entries.findLast { (_, rect) ->
            rect.contains(Offset(x, y))
        }?.key
    }
    
    // Update selected button's position
    fun updateButtonPosition(deltaX: Float, deltaY: Float) {
        selectedButtonId?.let { buttonId ->
            val currentOverride = currentSettings.buttonOverrides[buttonId] ?: 
                TouchControllerSettingsManager.ButtonOverride()
            
            val normalizedDeltaX = deltaX / (screenWidth / 2f)
            val normalizedDeltaY = deltaY / (screenHeight / 2f)
            
            val newOverride = currentOverride.copy(
                offsetX = MathUtils.clamp(currentOverride.offsetX + normalizedDeltaX, -2f, 2f),
                offsetY = MathUtils.clamp(currentOverride.offsetY + normalizedDeltaY, -2f, 2f)
            )
            
            val newOverrides = currentSettings.buttonOverrides.toMutableMap()
            newOverrides[buttonId] = newOverride
            val newSettings = currentSettings.copy(buttonOverrides = newOverrides)
            currentSettings = newSettings
            onSettingsChange(newSettings)
        }
    }
    
    // Update selected button's scale
    fun updateButtonScale(scaleFactor: Float) {
selectedButtonId?.let { buttonId ->
            val currentOverride = currentSettings.buttonOverrides[buttonId] ?: 
                TouchControllerSettingsManager.ButtonOverride()
            
            val currentScale = currentOverride.scale ?: currentSettings.scale
            val newScale = MathUtils.clamp(
                currentScale + (scaleFactor - 1f) * 0.5f,
                0.3f,
                3.0f
            )
            
            val newOverride = currentOverride.copy(scale = newScale)
            val newOverrides = currentSettings.buttonOverrides.toMutableMap()
            newOverrides[buttonId] = newOverride
            val newSettings = currentSettings.copy(buttonOverrides = newOverrides)
            currentSettings = newSettings
            onSettingsChange(newSettings)
        }
    }
    
    // Reset selected button
    fun resetSelectedButton() {
        selectedButtonId?.let { buttonId ->
            val newOverrides = currentSettings.buttonOverrides.toMutableMap()
            newOverrides.remove(buttonId)
            val newSettings = currentSettings.copy(buttonOverrides = newOverrides)
            currentSettings = newSettings
            onSettingsChange(newSettings)
            selectedButtonId = null
        }
    }
    
    // Reset all buttons
    fun resetAllButtons() {
        val newSettings = currentSettings.copy(buttonOverrides = emptyMap())
        currentSettings = newSettings
        onSettingsChange(newSettings)
        selectedButtonId = null
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        // Gesture detection layer using AndroidView for smooth performance
        AndroidView(
            factory = { context ->
                android.view.View(context).apply {
                    setBackgroundColor(android.graphics.Color.parseColor("#4D000000")) // 30% black
                    
                    val gestureDetector = MultiTouchGestureDetector(
                        context,
                        object : MultiTouchGestureDetector.SimpleOnMultiTouchGestureListener() {
                            
                            override fun onScale(detector: MultiTouchGestureDetector) {
                                if (selectedButtonId != null) {
                                    updateButtonScale(detector.scale)
                                }
                            }
                            
                            override fun onMove(detector: MultiTouchGestureDetector) {
                                if (selectedButtonId != null) {
                                    updateButtonPosition(detector.moveX, -detector.moveY)
                                }
                            }
                        }
                    )
                    
                    setOnTouchListener { _, event ->
                        when (event.actionMasked) {
                            MotionEvent.ACTION_DOWN -> {
                                // Tap to select button
                                val buttonId = findButtonAt(event.x, event.y)
                                selectedButtonId = buttonId
                            }
                        }
                        gestureDetector.onTouchEvent(event)
                        true
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Selection indicator overlay
        selectedButtonId?.let { buttonId ->
            buttonBounds[buttonId]?.let { rect ->
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val path = Path().apply { addRect(rect) }
                    
                    // Dashed border
                    drawPath(
                        path = path,
                        color = Color(0xFF00BCD4),
                        style = Stroke(
                            width = 4.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f),
                            cap = StrokeCap.Round
                        )
                    )
                    
                    // Corner handles
                    val handleSize = 12.dp.toPx()
                    listOf(rect.topLeft, rect.topRight, rect.bottomLeft, rect.bottomRight).forEach { corner ->
                        drawCircle(color = Color(0xFF00BCD4), radius = handleSize / 2, center = corner)
                        drawCircle(color = Color.White, radius = (handleSize / 2) - 2.dp.toPx(), center = corner)
                    }
                }
                
                // Info card
                val override = currentSettings.buttonOverrides[buttonId]
                val currentScale = override?.scale ?: currentSettings.scale
                
                Box(modifier = Modifier.align(Alignment.TopCenter).padding(top = 80.dp)) {
                    Card {
                        Text(
                            text = "Scale: ${String.format("%.2f", currentScale)}x\nPinch to resize • Drag to move",
                            modifier = Modifier.padding(12.dp),
                            fontSize = 14.sp,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
        
        // Hint when no button selected
        if (selectedButtonId == null) {
            Box(modifier = Modifier.align(Alignment.Center).padding(16.dp)) {
                Card {
                    Text(
                        text = "Tap any button to customize\n\nPinch to resize\nDrag to move",
                        modifier = Modifier.padding(16.dp),
                        fontSize = 16.sp,
                        style = MaterialTheme.typography.bodyLarge
                    )
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
