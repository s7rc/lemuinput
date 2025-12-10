package com.swordfish.touchinput.radial.customization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import gg.padkit.ids.Id

/**
 * Tracks the screen positions and bounds of all buttons for hit detection.
 */
class ButtonBoundsTracker {
    private val bounds = mutableStateMapOf<String, Rect>()
    
    fun updateBounds(buttonId: String, rect: Rect) {
        bounds[buttonId] = rect
    }
    
    fun getBounds(): Map<String, Rect> = bounds.toMap()
    
    fun clear() {
        bounds.clear()
    }
}

/**
 * Modifier to track button position and size for customization overlay.
 */
fun Modifier.trackButtonBounds(
    buttonId: String,
    tracker: ButtonBoundsTracker?
): Modifier {
    if (tracker == null) return this
    
    return this.onGloballyPositioned { layoutCoordinates ->
        val position = layoutCoordinates.positionInRoot()
        val size = layoutCoordinates.size
        
        val rect = Rect(
            left = position.x,
            top = position.y,
            right = position.x + size.width,
            bottom = position.y + size.height
        )
        
        tracker.updateBounds(buttonId, rect)
    }
}

/**
 * Remember a ButtonBoundsTracker instance.
 */
@Composable
fun rememberButtonBoundsTracker(): ButtonBoundsTracker {
    return remember { ButtonBoundsTracker() }
}
