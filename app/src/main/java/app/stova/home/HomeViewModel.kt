package app.stova.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.stova.profiles.ProfileSession
import app.stova.profiles.StartFocus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(session: ProfileSession, sources: List<HomeRowSource>) : ViewModel() {
    /** Restarts the sources only when another profile becomes active, not on every pref edit. */
    private val available: StateFlow<List<HomeRowUi>> = session.active
        .distinctUntilChangedBy { it?.id }
        .flatMapLatest { profile ->
            if (profile == null || sources.isEmpty()) flowOf(emptyList())
            else combine(sources.map { it.rows(profile) }) { parts -> parts.toList().flatten() }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val rows: StateFlow<List<HomeRowUi>> = combine(available, session.active) { rows, profile ->
        if (profile == null) emptyList()
        else {
            val byKey = rows.associateBy { it.key }
            arrangeRows(rows.map { it.key }, profile.home).mapNotNull { byKey[it] }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Every row a profile could show, before its own order and hiding are applied. */
    val availableRows: StateFlow<List<HomeRowKey>> = combine(available, session.active) { rows, _ -> rows.map { it.key } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val initialFocus: StateFlow<HomeRowKey?> = combine(rows, session.active) { rows, profile ->
        when {
            rows.isEmpty() -> null
            profile?.home?.startFocus == StartFocus.FAVORITE_APPS && rows.any { it.key == HomeRowKey.APPS } -> HomeRowKey.APPS
            else -> rows.firstOrNull { it.key != HomeRowKey.APPS }?.key ?: rows.first().key
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
