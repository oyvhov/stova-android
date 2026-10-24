package app.stova.profiles

import app.stova.core.security.PinHasher
import app.stova.core.theme.AvatarColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileRulesTest {
    private val adult = Profile("a", "A", AvatarColor.LIME)
    private val kid = Profile("k", "K", AvatarColor.ROSE, kid = true)
    private val pinned = adult.copy(pin = PinHasher.hash("1357"))

    @Test fun noProfilesCreatesFirst() = assertEquals(StartDecision.CreateFirst, decideStart(ProfilesFile()))

    @Test fun lastProfileWithoutPinEnters() =
        assertEquals(StartDecision.Enter(adult), decideStart(ProfilesFile(listOf(adult, kid), lastProfileId = "a")))

    @Test fun lastProfileWithPinNeedsPin() =
        assertEquals(StartDecision.NeedsPin(pinned), decideStart(ProfilesFile(listOf(pinned, kid), lastProfileId = "a")))

    @Test fun startInLastOffPicks() =
        assertEquals(StartDecision.Pick, decideStart(ProfilesFile(listOf(adult, kid), "a", startInLastProfile = false)))

    @Test fun singleProfileEntersEvenWithoutLast() =
        assertEquals(StartDecision.Enter(adult), decideStart(ProfilesFile(listOf(adult))))

    @Test fun kidLeavingNeedsPinOnlyWhenAnAdultHasPin() {
        assertFalse(leavingNeedsAdultPin(ProfilesFile(listOf(adult, kid)), kid))
        assertTrue(leavingNeedsAdultPin(ProfilesFile(listOf(pinned, kid)), kid))
        assertFalse(leavingNeedsAdultPin(ProfilesFile(listOf(pinned, kid)), pinned))
    }
}
