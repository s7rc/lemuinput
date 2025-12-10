package com.swordfish.touchinput.radial.utils

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import com.swordfish.touchinput.radial.settings.TouchControllerSettingsManager
import gg.padkit.ids.Id
import kotlin.math.roundToInt

/**
 * Extension function to apply button-specific overrides from settings.
 * If a button has a custom position/scale override, this applies it.
 */
fun Modifier.applyButtonOverride(
    buttonId: String,
    settings: TouchControllerSettingsManager.Settings,
    screenWidthPx: Int,
    screenHeightPx: Int,
): Modifier {
    val override = settings.buttonOverrides[buttonId] ?: return this
    
    return this.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        
        // Calculate the offset in pixels from normalized coordinates
        val offsetXPx = (override.offsetX * screenWidthPx).roundToInt()
        val offsetYPx = (override.offsetY * screenHeightPx).roundToInt()
        
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
