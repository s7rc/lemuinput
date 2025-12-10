package com.swordfish.touchinput.radial.customization

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.compose.foundation.Canvas
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
import com.swordfish.touchinput.radial.settings.TouchControllerSettingsManager
import kotlin.math.abs

/**
 * Smooth per-button customization using native Android gestures.
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
                currentScale * scaleFactor,
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
        // Native gesture detection layer
        AndroidView(
            factory = { ctx ->
                android.view.View(ctx).apply {
                    setBackgroundColor(android.graphics.Color.parseColor("#4D000000"))
                    
                    var lastX = 0f
                    var lastY = 0f
                    var pointerCount = 0
                    
                    val scaleDetector = ScaleGestureDetector(ctx, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                        override fun onScale(detector: ScaleGestureDetector): Boolean {
                            if (selectedButtonId != null) {
                                updateButtonScale(detector.scaleFactor)
                            }
                            return true
                        }
                    })
                    
                    setOnTouchListener { _, event ->
                        scaleDetector.onTouchEvent(event)
                        
                        when (event.actionMasked) {
                            MotionEvent.ACTION_DOWN -> {
                                lastX = event.x
                                lastY = event.y
                                pointerCount = 1
                                // Tap to select
                                selectedButtonId = findButtonAt(event.x, event.y)
                            }
                            
                            MotionEvent.ACTION_POINTER_DOWN -> {
                                pointerCount = event.pointerCount
                            }
                            
                            MotionEvent.ACTION_MOVE -> {
                                if (selectedButtonId != null && pointerCount == 1) {
                                    // Single finger drag = move
                                    val deltaX = event.x - lastX
                                    val deltaY = event.y - lastY
                                    
                                    if (abs(deltaX) > 1f || abs(deltaY) > 1f) {
                                        updateButtonPosition(deltaX, deltaY)
                                        lastX = event.x
                                        lastY = event.y
                                    }
                                }
                            }
                            
                            MotionEvent.ACTION_POINTER_UP -> {
                                pointerCount = event.pointerCount - 1
                            }
                            
                            MotionEvent.ACTION_UP -> {
                                pointerCount = 0
                            }
                        }
                        true
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Selection indicator
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
