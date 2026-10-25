package app.stova.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import coil3.compose.AsyncImage
import app.stova.core.ui.focusOutline
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.stova.R
import app.stova.core.theme.*
import app.stova.home.SeasonalMark
import app.stova.profiles.*

internal val themeLabels = listOf(R.string.theme_forest,R.string.theme_midnight,R.string.theme_cinema,
    R.string.theme_plum,R.string.theme_christmas,R.string.theme_halloween)
internal val accentLabels = listOf(R.string.appearance_lime,R.string.appearance_ocean,R.string.appearance_iris,
    R.string.appearance_coral,R.string.appearance_gold,R.string.appearance_mint,R.string.theme_rose,
    R.string.theme_pearl,R.string.theme_holly,R.string.theme_pumpkin)

@Composable
internal fun ThemeSwatches(selected: VisualTheme) {
    val outline = MaterialTheme.colorScheme.onSurface
    Canvas(Modifier.size(108.dp,22.dp).clearAndSetSemantics { }) {
        app.stova.profiles.PLAIN_MOODS.forEachIndexed { index, mood ->
            val x=index*18*density
            listOf(mood.background,mood.surface,mood.raised).forEachIndexed { stripe,color ->
                drawRect(color,Offset(x,stripe*6*density+2*density),Size(13*density,6*density))
            }
            if(mood==selected) drawRect(outline,Offset(x,2*density),Size(13*density,18*density),style=androidx.compose.ui.graphics.drawscope.Stroke(density))
        }
    }
}
@Composable
internal fun AccentSwatches(selected: Accent) {
    val outline = MaterialTheme.colorScheme.onSurface
    Canvas(Modifier.size(118.dp,22.dp).clearAndSetSemantics { }) {
        Accent.entries.forEachIndexed { index, accent ->
            val center=Offset((index*12+5)*density,size.height/2)
            drawCircle(accent.color,4*density,center)
            if(accent==selected) drawCircle(outline,5.5f*density,center,style=androidx.compose.ui.graphics.drawscope.Stroke(density))
        }
    }
}

/** A non-interactive slice of Home: it never takes focus away from the controls. */
@Composable
internal fun ThemePreview(stored: AppearancePrefs, entry: app.stova.media.SpotlightEntry? = null, home: HomePrefs = HomePrefs()) {
    StovaTheme(appearance = stored) {
        val p = LocalAppearance.current
        val ink = MaterialTheme.colorScheme.background
        val title = entry?.item?.title ?: stringResource(R.string.custom_preview_title)
        val backdrop = entry?.item?.art?.backdrop.takeUnless { p.backgroundSource == BackgroundSource.THEME }
        val sampleKey = app.stova.home.HomeRowKey.latestMovies(entry?.account?.id.orEmpty())
        val row = home.rowFormats[sampleKey] ?: RowStyle()
        val dimensions = app.stova.home.artworkDimensions(row.format ?: app.stova.home.defaultRowFormat(sampleKey,p),row.size ?: p.cardSize)
        val source = remember { MutableInteractionSource() }
        val sibling = remember { MutableInteractionSource() }
        LaunchedEffect(source) { withFrameNanos { }; source.emit(FocusInteraction.Focus()) }
        Box(Modifier.fillMaxWidth().testTag("appearance-preview").clip(MaterialTheme.shapes.large)
            .background(ink).border(1.dp,MaterialTheme.colorScheme.outlineVariant,MaterialTheme.shapes.large)) {
            if (backdrop != null) AsyncImage(backdrop,null,Modifier.matchParentSize().testTag("preview-backdrop"),contentScale=ContentScale.Crop)
            else Canvas(Modifier.matchParentSize().clearAndSetSemantics { }) {
                drawRect(p.visualTheme.surface)
                val mountain = Path().apply {
                    moveTo(size.width*.45f,size.height);lineTo(size.width*.68f,size.height*.15f)
                    lineTo(size.width*.83f,size.height*.73f);lineTo(size.width*.94f,size.height*.34f)
                    lineTo(size.width,size.height*.55f);lineTo(size.width,size.height);close()
                }
                drawPath(mountain,p.visualTheme.raised)
            }
            Box(Modifier.matchParentSize().background(Brush.horizontalGradient(listOf(ink,ink.copy(alpha=.86f),ink.copy(alpha=.12f)))))
            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.theme_preview),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    if(p.seasonalDecor!=SeasonalDecor.OFF) SeasonalMark(p.season())
                    Spacer(Modifier.weight(1f))
                    Text(stringResource(themeLabels[p.visualTheme.ordinal]),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if(p.heroSize!=HeroSize.OFF) Box(Modifier.testTag("preview-hero").padding(bottom=if(p.heroSize==HeroSize.LARGE) 18.dp else 0.dp)) {
                    val logo=entry?.item?.art?.logo
                    if(p.heroTitle==HeroTitle.LOGO && logo!=null) AsyncImage(logo,null,Modifier.widthIn(max=180.dp).height(28.dp),contentScale=ContentScale.Fit)
                    else Text(title,style=MaterialTheme.typography.titleMedium,color=MaterialTheme.colorScheme.onSurface,maxLines=1,overflow=TextOverflow.Ellipsis)
                }
                CompositionLocalProvider(app.stova.home.LocalRowHasFocus provides true) {
                    Row(horizontalArrangement=Arrangement.spacedBy((p.cardSpacing.dp*.4f).dp)) {
                        listOf(source,sibling).forEach { interaction ->
                            Column {
                                Box(Modifier.size((dimensions.width*.36f).dp,(dimensions.height*.36f).dp)
                                    .focusOutline(interaction,MaterialTheme.shapes.medium,zoom=true).clip(MaterialTheme.shapes.medium)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)) {
                                    val art=entry?.item?.art?.let { if(row.format==RowFormat.POSTER || (row.format==null && p.movieStyle==MovieStyle.POSTER)) it.poster ?: it.wide else it.wide ?: it.poster }
                                    if(art!=null) AsyncImage(art,null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
                                    if(p.progressStyle.dp>0) Box(Modifier.align(Alignment.BottomStart).fillMaxWidth(.4f).height(p.progressStyle.dp.dp).background(MaterialTheme.colorScheme.primary))
                                }
                                if(p.showTitles) Text(title,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurface,maxLines=1)
                            }
                        }
                    }
                }
            }
        }
    }
}
