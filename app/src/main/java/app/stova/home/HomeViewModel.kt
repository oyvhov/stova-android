package app.stova.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.stova.media.SpotlightEntry
import app.stova.profiles.Profile
import app.stova.profiles.ProfileSession
import app.stova.profiles.StartFocus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** How long each spotlight title holds the hero before the next one fades in. */
const val SPOTLIGHT_MILLIS = 12_000L

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val session: ProfileSession,
    sources: List<HomeRowSource>,
    spotlightSource: (Profile) -> Flow<List<SpotlightEntry>> = { flowOf(emptyList()) },
) : ViewModel() {
    private val focusedState = MutableStateFlow<FocusedTarget?>(null)
    val focused: StateFlow<FocusedTarget?> = focusedState
    private val rotation = MutableStateFlow(true)
    private var spotlightIndex = 0
    init {
        viewModelScope.launch { session.active.distinctUntilChangedBy { it?.id }.collect { focusedState.value = null; spotlightIndex = 0 } }
    }
    fun rotationEnabled() = session.active.value?.home?.backdropRotation ?: false
    fun setSpotlightRotation(enabled: Boolean) { rotation.value = enabled }
    /** Remembers the card for focus restore; the hero and backdrop follow the spotlight, not focus. */
    fun focus(target: FocusedTarget) { focusedState.value = target }

    private val spotlightPool: Flow<List<SpotlightEntry>> = session.active
        .distinctUntilChangedBy { it?.id }
        .flatMapLatest { profile -> if (profile == null) flowOf(emptyList()) else spotlightSource(profile) }
        .distinctUntilChanged()

    /**
     * The title the hero shows, stepping through the library pool. It only ticks while Home is on
     * screen, and picks up where it left off when Home comes back.
     */
    val spotlight: StateFlow<SpotlightEntry?> = combine(spotlightPool, rotation) { pool, rotate -> pool to rotate }
        .flatMapLatest { (pool, rotate) ->
            flow {
                if (pool.isEmpty()) { emit(null); return@flow }
                while (true) {
                    emit(pool[spotlightIndex % pool.size])
                    if (!rotate || pool.size < 2) return@flow
                    delay(SPOTLIGHT_MILLIS)
                    spotlightIndex = (spotlightIndex + 1) % pool.size
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * Null until every source has answered for the current profile: "not loaded yet" must never
     * look like "empty", or the empty state flashes on every cold start. Restarts the sources only
     * when another profile becomes active, not on every pref edit.
     */
    private val available: StateFlow<List<HomeRowUi>?> = session.active
        .distinctUntilChangedBy { it?.id }
        .flatMapLatest { profile ->
            flow {
                emit(null)
                if (profile == null || sources.isEmpty()) emit(emptyList())
                else emitAll(combine(sources.map { it.rows(profile) }) { parts -> parts.toList().flatten() })
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val rows: StateFlow<List<HomeRowUi>?> = combine(available, session.active) { rows, profile ->
        if (profile == null || rows == null) null
        else {
            val byKey = rows.associateBy { it.key }
            arrangeRows(rows.map { it.key }, profile.home).mapNotNull { byKey[it] }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Every row a profile could show, before its own order and hiding are applied. */
    val availableRows: StateFlow<List<HomeRowKey>> = available.map { rows -> rows.orEmpty().map { it.key } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val initialFocus: StateFlow<HomeRowKey?> = combine(rows, session.active) { rows, profile ->
        when {
            rows.isNullOrEmpty() -> null
            profile?.home?.startFocus == StartFocus.FAVORITE_APPS && rows.any { it.key == HomeRowKey.APPS } -> HomeRowKey.APPS
            else -> rows.firstOrNull { it.key != HomeRowKey.APPS }?.key ?: rows.first().key
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
