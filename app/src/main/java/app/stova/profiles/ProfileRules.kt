package app.stova.profiles

sealed interface StartDecision {
    data object CreateFirst : StartDecision
    data object Pick : StartDecision
    data class NeedsPin(val profile: Profile) : StartDecision
    data class Enter(val profile: Profile) : StartDecision
}

/** What a cold start shows: first-run setup, the picker, a PIN prompt or the home screen. */
fun decideStart(file: ProfilesFile): StartDecision {
    val profiles = file.profiles
    if (profiles.isEmpty()) return StartDecision.CreateFirst
    val target = when {
        profiles.size == 1 -> profiles.single()
        file.startInLastProfile -> profiles.firstOrNull { it.id == file.lastProfileId }
        else -> null
    } ?: return StartDecision.Pick
    return if (target.pin != null) StartDecision.NeedsPin(target) else StartDecision.Enter(target)
}

/** A child leaves their profile only with an adult's PIN, when any adult has set one. */
fun leavingNeedsAdultPin(file: ProfilesFile?, current: Profile): Boolean =
    current.kid && (file == null || file.profiles.any { !it.kid && it.pin != null })
