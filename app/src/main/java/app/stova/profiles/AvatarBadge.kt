package app.stova.profiles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import app.stova.core.theme.color

/** Each profile's picture by profile id, from its Jellyfin or Emby account (see `profilePictures`). */
val LocalProfilePictures = compositionLocalOf<Map<String, String>> { emptyMap() }

/**
 * The user's own picture from the profile's media account, in a circle. Without one, or until it has
 * arrived, a circle in the profile's colour with the first letter of the name, in ink.
 */
@Composable
fun AvatarBadge(profile: Profile, size: Dp, modifier: Modifier = Modifier) {
    val fontSize = with(LocalDensity.current) { (size * 0.42f).toSp() }
    val picture = LocalProfilePictures.current[profile.id]
    var shown by remember(picture) { mutableStateOf(false) }
    Box(modifier.size(size).clip(CircleShape).background(profile.avatar.color), contentAlignment = Alignment.Center) {
        if (!shown) Text(
            profile.name.take(1).uppercase(),
            color = MaterialTheme.colorScheme.onPrimary,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clearAndSetSemantics { },
        )
        if (picture != null) AsyncImage(picture, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize(),
            onSuccess = { shown = true }, onError = { shown = false })
    }
}
