package com.swordfish.touchinput.radial.utils

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.IntOffset
import com.swordfish.touchinput.radial.settings.TouchControllerSettingsManager
import gg.padkit.ids.Id
import kotlin.math.roundToInt

/**
 * Apply button-specific customization (position offset).
 * Offsets are normalized (-1.0 to 1.0) and converted to pixels during layout.
 */
fun Modifier.applyButtonOverride(
    buttonId: String,
    settings: TouchControllerSettingsManager.Settings
): Modifier {
    val override = settings.buttonOverrides[buttonId] ?: return this
    
    // If no position offset, return as-is
    if (override.offsetX == 0f && override.offsetY == 0f) return this
    
    return this.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        
        // Convert normalized offsets to pixels based on parent constraints
        val offsetXPx = (override.offsetX * constraints.maxWidth / 2).roundToInt()
        val offsetYPx = (override.offsetY * constraints.maxHeight / 2).roundToInt()
        
        layout(placeable.width, placeable.height) {
            placeable.place(offsetXPx, offsetYPx)
        }
    }
}

/**
 * Converts an Id to a unique button identifier string.
 * This identifier is used as the key in buttonOverrides map.
 */
fun Id.toButtonIdentifier(): String {
    // Use the Id's string representation which includes its unique identifier
    return "control_${this.hashCode()}"
}

/**
 * Gets the effective scale for a button, considering both global and per-button settings.
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
