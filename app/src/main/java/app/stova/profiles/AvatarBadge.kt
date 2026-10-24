package app.stova.profiles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import app.stova.core.theme.color

/** A circle in the profile's colour with the first letter of the name, in ink. */
@Composable
fun AvatarBadge(profile: Profile, size: Dp, modifier: Modifier = Modifier) {
    val fontSize = with(LocalDensity.current) { (size * 0.42f).toSp() }
    Box(modifier.size(size).background(profile.avatar.color, CircleShape), contentAlignment = Alignment.Center) {
        Text(
            profile.name.take(1).uppercase(),
            color = MaterialTheme.colorScheme.onPrimary,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
        )
    }
}
