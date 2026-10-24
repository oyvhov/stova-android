package app.stova.ha

import org.junit.Assert.*
import org.junit.Test

class HaAddressTest {
    @Test fun bareHostGetsDefaultPort() = assertEquals(AddressCheck.Ok(HaAddress("http://homeassistant.local:8123", "ws://homeassistant.local:8123/api/websocket")), checkHaAddress("homeassistant.local"))
    @Test fun httpsPublicOk() = assertEquals("wss://ha.example.no/api/websocket", (checkHaAddress("https://ha.example.no/") as AddressCheck.Ok).address.websocket)
    @Test fun httpPublicRejected() = assertEquals(AddressCheck.InsecurePublic, checkHaAddress("http://ha.example.no"))
    @Test fun privateRanges() {
        listOf("10.0.0.5", "172.16.1.1", "172.31.255.1", "192.168.1.10", "127.0.0.1", "ha.local", "ha.lan", "ha.home.arpa").forEach { assertTrue(it, isPrivateHost(it)) }
        listOf("172.32.0.1", "8.8.8.8", "ha.example.no", "192.168.1.999", "ha.local.evil.com").forEach { assertFalse(it, isPrivateHost(it)) }
    }
    @Test fun garbageInvalid() { listOf("http://", "ftp://ha.local", "http://u:p@ha.local", "http://ha.local/?token=x").forEach { assertEquals(AddressCheck.Invalid, checkHaAddress(it)) } }
    @Test fun explicitPortAndSubpath() = assertEquals(AddressCheck.Ok(HaAddress("http://ha.lan:123/ha", "ws://ha.lan:123/ha/api/websocket")), checkHaAddress("ha.lan:123/ha/"))
}
