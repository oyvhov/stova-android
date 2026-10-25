package app.stova.media.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.core.theme.RatingColors
import app.stova.media.model.Ratings
import java.text.NumberFormat
import java.util.Locale

fun communityText(score: Double, locale: Locale): String = String.format(locale, "%.1f", score)
fun criticText(percent: Int, locale: Locale): String = NumberFormat.getPercentInstance(locale).format(percent / 100.0)

private val numericAge = Regex("(?:NO[- ]?)?([0-9]{1,2})\\+?", RegexOption.IGNORE_CASE)

@Composable
private fun officialRatingText(official: String): String {
    val value = official.trim()
    val age = numericAge.matchEntire(value)?.groupValues?.get(1)?.toIntOrNull()
    return if (age != null) pluralStringResource(R.plurals.rating_age_years, age, age) else value
}

/** Plain facts over the hero's existing scrim; no individual backgrounds or outlines. */
@Composable
fun RatingRow(ratings: Ratings, modifier: Modifier = Modifier, year: Int? = null) {
    val locale = LocalLocale.current.platformLocale
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        ratings.community?.takeIf { it in 0.0..10.0 }?.let {
            RatingFact(stringResource(R.string.appearance_star_score, communityText(it, locale)))
        }
        year?.let { RatingFact(it.toString()) }
        ratings.official?.takeIf { it.isNotBlank() }?.let { RatingFact(officialRatingText(it)) }
        ratings.criticPercent?.takeIf { it in 0..100 }?.let { score ->
            val fresh = score >= 60
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painterResource(if (fresh) R.drawable.ic_tomato else R.drawable.ic_rotten_splat),
                    stringResource(if (fresh) R.string.rating_critic_fresh else R.string.rating_critic_rotten),
                    Modifier.size(14.dp),
                    colorFilter = ColorFilter.tint(if (fresh) RatingColors.Fresh else RatingColors.Rotten),
                )
                RatingFact(criticText(score, locale))
            }
        }
    }
}

@Composable
private fun RatingFact(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
}
