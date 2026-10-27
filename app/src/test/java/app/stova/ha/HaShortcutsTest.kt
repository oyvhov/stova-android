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

    // The tile shows the icon Home Assistant shows: chosen for the entity, its own, or the domain's.
    @Test fun scenesCarryTheIconHomeAssistantShows() = runTest {
        fun entity(id: String, icon: String? = null) = HaEntity(id, "off", kotlinx.serialization.json.buildJsonObject {
            put("friendly_name", kotlinx.serialization.json.JsonPrimitive(id))
            icon?.let { put("icon", kotlinx.serialization.json.JsonPrimitive(it)) } }, 0, 0)
        val ha = FakeHaConnection().apply {
            allStates = listOf(entity("scene.a", "mdi:sofa"), entity("scene.b", "mdi:sofa"), entity("scene.c"), entity("script.d"), entity("scene.e"))
        }
        val registry = HaRegistry(mapOf("scene.b" to HaEntityInfo(icon = "mdi:movie-open"), "scene.e" to HaEntityInfo(originalIcon = "mdi:weather-night")))
        val icons = haShortcutScenes(ha) { registry }.associate { it.entityId to it.icon }
        assertEquals(mapOf("scene.a" to "mdi:sofa", "scene.b" to "mdi:movie-open", "scene.c" to "mdi:palette",
            "script.d" to "mdi:script-text", "scene.e" to "mdi:weather-night"), icons)
    }
}
