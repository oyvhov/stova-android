package app.stova.core.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import app.stova.profiles.Accent
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingContrastTest {
    private fun contrast(a:Color,b:Color):Float {
        val light=maxOf(a.luminance(),b.luminance());val dark=minOf(a.luminance(),b.luminance())
        return (light+.05f)/(dark+.05f)
    }
    @Test fun theWholeStoryRemainsReadableEvenOverAWhiteImage() {
        for(theme in VisualTheme.entries) for(dimming in listOf(0,25,50,75,100)) {
            val colors=stovaColorScheme(theme,Accent.OCEAN)
            for(position in listOf(.25f,.45f,.65f,.8f,.9f)) {
                val background=colors.background.copy(alpha=backdropScrimAlpha(dimming,position,reading=true)).compositeOver(Color.White)
                assertTrue("$theme/$dimming at $position: text needs strong contrast",contrast(colors.onSurface,background)>=7f)
                if(position<=.65f) assertTrue("$theme/$dimming: facts must stay readable",contrast(colors.onSurfaceVariant,background)>=4.5f)
            }
        }
    }
}
