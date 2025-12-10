package com.swordfish.touchinput.radial.utils

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.IntOffset
import com.swordfish.touchinput.radial.settings.TouchControllerSettingsManager
import gg.padkit.ids.Id
import kotlin.math.roundToInt

/**
 * Apply button-specific position offset.
 * Offset values are normalized where screen width/height map to -1.0 to 1.0
 */
fun Modifier.applyButtonOverride(
    buttonId: String,
    settings: TouchControllerSettingsManager.Settings
): Modifier {
    val override = settings.buttonOverrides[buttonId] ?: return this
    
    // If no position offset, skip
    if (override.offsetX == 0f && override.offsetY == 0f) return this
    
    return this.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        
        // Convert normalized offset to actual pixels
        // Normalized: -1.0 to 1.0 maps to full screen width/height
        // So divide by 2 to get half-screen range
        val offsetXPx = (override.offsetX * constraints.maxWidth).roundToInt()
        val offsetYPx = (override.offsetY * constraints.maxHeight).roundToInt()
        
        layout(placeable.width, placeable.height) {
            placeable.place(offsetXPx, offsetYPx)
        }
    }
}

/**
 * Converts an Id to a unique button identifier string.
 */
fun Id.toButtonIdentifier(): String {
    return "control_${this.hashCode()}"
}

/**
 * Gets the effective scale for a button.
 */
fun getEffectiveScale(
    buttonId: String,
    settings: TouchControllerSettingsManager.Settings,
): Float {
    val override = settings.buttonOverrides[buttonId]
    return override?.scale ?: settings.scale
}

/**
 * Checks if a button has any custom overrides.
 */
fun hasButtonOverride(
    buttonId: String,
    settings: TouchControllerSettingsManager.Settings,
): Boolean {
    return settings.buttonOverrides.containsKey(buttonId)
}
