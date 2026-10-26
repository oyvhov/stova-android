package app.stova.home

import app.stova.profiles.spotlightMillis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.stova.media.SpotlightEntry
import app.stova.profiles.Profile
import app.stova.profiles.season
import app.stova.profiles.atHour
import app.stova.profiles.seasonal
import app.stova.profiles.ProfileSession
import app.stova.profiles.StartFocus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** How long each spotlight title holds the hero before the next one fades in. */
const val SPOTLIGHT_MILLIS = 12_000L

/** How long a run of season changes settles before the media rows restart. */
const val SEASON_SETTLE_MILLIS = 600L

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val session: ProfileSession,
    sources: List<HomeRowSource>,
    spotlightSource: (Profile) -> Flow<List<SpotlightEntry>> = { flowOf(emptyList()) },
) : ViewModel() {
    private val calendarDate = MutableStateFlow(java.time.LocalDate.now())
    fun setCalendarDate(date: java.time.LocalDate) { calendarDate.value = date }
    // Only a profile or effective season change restarts media, never an unrelated appearance edit.
    private val seasonalProfile = combine(session.active, calendarDate) { profile, date ->
        profile?.copy(appearance = profile.appearance.seasonal(date))
    }.distinctUntilChangedBy { it?.id to it?.appearance?.season() }
    private val focusedState = MutableStateFlow<FocusedTarget?>(null)
    val focused: StateFlow<FocusedTarget?> = focusedState
    private val focusedRow = MutableStateFlow<HomeRowKey?>(null)
    private val rotation = MutableStateFlow(true)
    private var spotlightIndex = 0
    private var spotlightProfileSeason: Pair<String, app.stova.profiles.Season>? = null
    init {
        viewModelScope.launch { session.active.distinctUntilChangedBy { it?.id }.collect { focusedState.value = null; focusedRow.value = null; spotlightIndex = 0 } }
    }
    fun rotationEnabled() = session.active.value?.home?.backdropRotation ?: false
    fun setSpotlightRotation(enabled: Boolean) { rotation.value = enabled }
    /** Remembers the card for focus restore; the hero and backdrop follow the spotlight, not focus. */
    fun focus(target: FocusedTarget) { focusedState.value = target }
    fun focusRow(key: HomeRowKey) { focusedRow.value = key }

    private var lastPool: List<SpotlightEntry> = emptyList()
    private val hour = MutableStateFlow(java.time.LocalTime.now().hour)
    /** The shell feeds the hour only while kids time is on and the app is started. */
    fun setHour(value: Int) { hour.value = value }

    private val spotlightPool: Flow<List<SpotlightEntry>> = combine(session.active,calendarDate,hour) { profile,date,now ->
            profile?.copy(appearance=profile.appearance.seasonal(date).let {
                it.copy(backgroundFilter = it.backgroundFilter.atHour(it.kidsHours, now))
            })
        }.distinctUntilChangedBy { listOf(it?.id,it?.appearance?.season(),it?.appearance?.spotlightSource,it?.appearance?.backgroundFilter) }
        .flatMapLatest { profile ->
            val key = profile?.let { it.id to it.appearance.season() }
            if (key != spotlightProfileSeason) { spotlightIndex = 0; spotlightProfileSeason = key }
            // While a new choice loads, keep only titles that the new choice also allows; others go at once.
            if (profile == null) flowOf(emptyList()) else flow {
                val a = profile.appearance
                emit(lastPool.filter { it.account.profileId == profile.id &&
                    app.stova.media.acceptsSpotlight(it.item, a.spotlightSource) && app.stova.media.acceptsBackground(it.item, a.backgroundFilter) })
                emitAll(spotlightSource(profile))
            }
        }
        .distinctUntilChanged()
        .onEach { lastPool = it }

    /**
     * The title the hero shows, stepping through the library pool. It only ticks while Home is on
     * screen, and picks up where it left off when Home comes back.
     */
    val spotlight: StateFlow<SpotlightEntry?> = combine(spotlightPool, rotation, session.active.map { it?.appearance?.spotlightMillis ?: SPOTLIGHT_MILLIS }.distinctUntilChanged()) { pool, rotate, millis -> Triple(pool, rotate, millis) }
        .flatMapLatest { (pool, rotate, millis) ->
            flow {
                if (pool.isEmpty()) { emit(null); return@flow }
                while (true) {
                    emit(pool[spotlightIndex % pool.size])
                    if (!rotate || pool.size < 2) return@flow
                    delay(millis)
                    spotlightIndex = (spotlightIndex + 1) % pool.size
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(0), null)

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
                // A season change restarts only the sources that depend on it, never the app scan.
                else emitAll(combine(sources.map { source ->
                    if (!source.followsSeason) source.rows(profile)
                    else settledSeason(profile.id).flatMapLatest { source.rows(it) }
                }) { parts -> parts.toList().flatten() })
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /**
     * The profile with its season laid over: the first value at once, and a run of changes (fast
     * presses through Theme) only once it settles, so the media refresh starts once, not per press.
     */
    @OptIn(FlowPreview::class)
    private fun settledSeason(profileId: String): Flow<Profile> {
        var first = true
        return seasonalProfile.filterNotNull().filter { it.id == profileId }
            .debounce { if (first) { first = false; 0L } else SEASON_SETTLE_MILLIS }
    }

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

    val initialFocus: StateFlow<HomeRowKey?> = combine(rows, session.active, focusedRow) { rows, profile, lastRow ->
        when {
            rows.isNullOrEmpty() -> null
            lastRow != null && rows.any { it.key == lastRow } -> lastRow
            profile?.home?.startFocus == StartFocus.FAVORITE_APPS && rows.any { it.key == HomeRowKey.APPS } -> HomeRowKey.APPS
            else -> rows.firstOrNull { it.key != HomeRowKey.APPS }?.key ?: rows.first().key
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
