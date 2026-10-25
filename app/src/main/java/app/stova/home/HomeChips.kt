package app.stova.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.stova.core.theme.LocalAppearance
import app.stova.ha.HaEntity
import app.stova.ha.HaRepository
import app.stova.ha.cards.CardConfig
import app.stova.ha.ui.ChipContent
import app.stova.ha.ui.chipText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import java.util.Locale

/** Only changes to what a chip actually displays can invalidate the top bar. */
internal fun homeChipContents(
    cards: Flow<List<CardConfig>>,
    entities: Flow<Map<String, HaEntity>>,
    locale: Locale,
): Flow<List<ChipContent>> = combine(cards, entities) { selected, live ->
    selected.take(3).map { chipText(it, live[it.entityId], locale) }
}.distinctUntilChanged()

/** Collection belongs to the slot, so HA events cannot invalidate the media rows. */
@Composable
internal fun HomeChips(repository: HaRepository?, content: @Composable (List<ChipContent>) -> Unit) {
    if (repository == null || !LocalAppearance.current.showHaChips) {
        content(emptyList())
        return
    }
    val locale = LocalConfiguration.current.locales[0]
    val source = remember(repository, locale) { homeChipContents(repository.homeCards, repository.entities, locale) }
    val chips by source.collectAsStateWithLifecycle(initialValue = emptyList())
    content(chips)
}
