package com.swordfish.touchinput.radial.controls

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import com.swordfish.touchinput.radial.LocalLemuroidPadTheme
import com.swordfish.touchinput.radial.settings.TouchControllerSettingsManager
import com.swordfish.touchinput.radial.ui.LemuroidButtonForeground
import com.swordfish.touchinput.radial.ui.LemuroidControlBackground
import com.swordfish.touchinput.radial.utils.getEffectiveScale
import com.swordfish.touchinput.radial.utils.toButtonIdentifier
import gg.padkit.PadKitScope
import gg.padkit.controls.ControlButton
import gg.padkit.ids.Id
import gg.padkit.layouts.radial.secondarydials.LayoutRadialSecondaryDialsScope

context(PadKitScope, LayoutRadialSecondaryDialsScope)
@Composable
fun LemuroidControlButton(
    modifier: Modifier = Modifier,
    id: Id.Key,
    label: String? = null,
    icon: Int? = null,
    settings: TouchControllerSettingsManager.Settings,
) {
    val theme = LocalLemuroidPadTheme.current
    val buttonId = id.toButtonIdentifier()
    
    // Apply per-button scale if override exists
    val effectiveScale = getEffectiveScale(buttonId, settings)
    val baseScale = settings.scale
    val relativeScale = if (baseScale > 0f) effectiveScale / baseScale else 1f
    
    ControlButton(
        modifier = modifier
            .padding(theme.padding)
            .scale(relativeScale),
        id = id,
        foreground = { LemuroidButtonForeground(pressed = it, icon = icon, label = label) },
        background = { LemuroidControlBackground() },
    )
}
