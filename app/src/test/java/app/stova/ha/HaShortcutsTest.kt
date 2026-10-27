package app.stova.ha

import app.stova.core.theme.AvatarColor
import app.stova.profiles.Profile
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HaShortcutsTest {
    private val adult = Profile("p", "A", AvatarColor.LIME)

    // A scene tile on Home: the connection is held only while the call runs, then left to rest.
    @Test fun aSceneRunsAndTheConnectionRestsAfterwards() = runTest {
        val ha = FakeHaConnection()
        var called: List<String>? = null
        ha.service = { domain, service, entity, _ -> called = listOf(domain, service, entity); true }
        assertTrue(runHaShortcut(ha, "scene.kino", adult))
        assertEquals(listOf("scene", "turn_on", "scene.kino"), called)
        assertEquals(0, ha.holders)
        assertTrue(runHaShortcut(ha, "script.god_natt", adult))
        assertEquals(listOf("script", "turn_on", "script.god_natt"), called)
    }

    @Test fun onlyScenesAndScriptsAndOnlyForThoseWhoMayControl() = runTest {
        val ha = FakeHaConnection()
        assertFalse(runHaShortcut(ha, "lock.ytterdor", adult))
        assertFalse(runHaShortcut(ha, "scene.kino", adult.copy(kid = true)))
        assertTrue(runHaShortcut(ha, "scene.kino", adult.copy(kid = true, kidSmartHomeControl = true)))
        assertEquals(1, ha.calls)
    }

    @Test fun noConnectionInTimeIsAFailureNotAHang() = runTest {
        val ha = FakeHaConnection().apply { status.value = HaStatus.SIGNED_OUT }
        assertFalse(runHaShortcut(ha, "scene.kino", adult))
        assertEquals(0, ha.calls)
        assertEquals(0, ha.holders)
    }

    // The Apps page lists the house's scenes and scripts by their Home Assistant names, and no lights.
    @Test fun theAppsPageGetsScenesAndScriptsByName() = runTest {
        fun entity(id: String, name: String) = HaEntity(id, "off", kotlinx.serialization.json.buildJsonObject {
            put("friendly_name", kotlinx.serialization.json.JsonPrimitive(name)) }, 0, 0)
        val ha = FakeHaConnection().apply {
            allStates = listOf(entity("scene.kino", "Kino"), entity("light.stova", "Stova"), entity("script.god_natt", "God natt"),
                entity("scene.gamal", "Gamal"))
        }
        val registry = HaRegistry(mapOf("scene.kino" to HaEntityInfo(name = "Kinokveld"), "scene.gamal" to HaEntityInfo(hidden = true)))
        val scenes = haShortcutScenes(ha) { registry }
        assertEquals(listOf("God natt" to "script.god_natt", "Kinokveld" to "scene.kino"), scenes.map { it.label to it.entityId })
        assertEquals("ha:scene.kino", scenes.last().key)
        assertEquals(0, ha.holders)
    }
}
