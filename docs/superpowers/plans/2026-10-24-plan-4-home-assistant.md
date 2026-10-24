# Stova Plan 4 – Home Assistant · implementasjonsplan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Stova koplar til Home Assistant med OAuth (eller langtidstoken), viser eit lite Smarthus-dashbord med native, flate kort i Tunet-stil per profil, og viser opptil tre statusbrikker på heimeskjermen.

**Architecture:** `HaAuth` eig token-livssyklusen. `HaSocket` (OkHttp WebSocket) eig tilkoplinga, abonnerer berre på entitetane som den aktive profilen bruker, og dekodar komprimerte endringar med `EntityStateDecoder` til ein `StateFlow<Map<String, HaEntity>>`. Kortlogikken (typeval, tenestekall og formatering) er reine funksjonar. Compose-korta teiknar berre tilstanden.

**Tech Stack:** Som Plan 1–2. OkHttp WebSocket (`newWebSocket`) og MockWebServer sin WebSocket-støtte i testane. `android.webkit.WebView` for innlogging.

**Spec:** `docs/superpowers/specs/2026-10-24-stova-launcher-design.md` §4 (barnemodus), §5.2, §8

## Global Constraints

- Alle Global Constraints frå Plan 1 og 2 gjeld.
- `client_id = "https://oyvhov.github.io/stova/"`, `redirect_uri = "https://oyvhov.github.io/stova/auth-callback"`.
- `http://` berre til private vertar (10/8, 172.16/12, 192.168/16, 127/8, `*.local`, `*.lan`, `*.home.arpa`). Elles: «Bruk https for adresser utanfor heimenettet.».
- Access-token blir fornya når det er mindre enn 60 s att. Reconnect-backoff: 1, 2, 4, 8, 16 og 30 s (maks). Fråkopling 30 s etter `onStop`.
- Tokens berre i `SecretStore` (`ha/tokens`), aldri i `household.json` eller loggar.
- Kort: 1 × 1 = 240 × 160 dp, 2 × 1 = 496 × 160 dp. `Surface`-fyll, 16 dp hjørne, ikonrute i `Raised` (40 dp, 12 dp hjørne), stor verdi i `headlineMedium`, etikett i `labelMedium` + `Muted`, statusbrikke øvst til høgre i `Raised`. Ingen gradientar, ingen gjennomsikt.
- Grafar: 2 dp linje i `Text` med 60 % alfa, utan fyll.
- Lime berre på aktive brytarar (på), spel-knappar og fokus. Aldri på målte verdiar.
- Maks tre kort med «Vis på heim» per profil.

## Review Focus

1. **HA startar på nytt (oppdatering) medan Stova er open.** Forventa: status «Koplar til …», og automatisk ny tilkopling med backoff og nytt abonnement på same entitetar. Ingen kræsj, ingen duplikate abonnement. Test i oppgåve 5.
2. **Refresh-token blir trekt tilbake i HA.** Forventa: status «Logg inn på nytt» i Smarthus og Innstillingar. Socketen gir opp utan evig løkkje. Test i oppgåve 2 og 5.
3. **Ein entitet i eit kort blir sletta eller er `unavailable`.** Forventa: kortet viser «Utilgjengeleg» i `Muted`, er framleis fokuserbart og kan fjernast i redigering. Test i oppgåve 6.
4. **Barneprofil utan smarthus-løyve trykkjer OK på eit lyskort.** Forventa: ingen tenestekall. Kortet viser hengelås-ikon. Test i oppgåve 7.
5. **Ein profil med mange kort, der mange entitetar endrar seg ofte (straummålar kvart sekund).** Forventa: berre dei abonnerte entitetane kjem inn, `coalesce_messages` er på, og UI-en hakkar ikkje. Test i oppgåve 5 (array-meldingar), måling i Plan 5.

---

### Task 1: HA-adresse og LAN-regel

**Files:**
- Create: `app/src/main/java/app/stova/ha/HaAddress.kt`
- Test: `app/src/test/java/app/stova/ha/HaAddressTest.kt`

**Interfaces:**
- Produces:
  - `data class HaAddress(val base: String, val websocket: String)` (`base` utan avsluttande `/`, `websocket = base` med `http`→`ws`/`https`→`wss` + `/api/websocket`)
  - `sealed interface AddressCheck { data class Ok(val address: HaAddress) : AddressCheck; data object Invalid : AddressCheck; data object InsecurePublic : AddressCheck }`
  - `fun checkHaAddress(input: String): AddressCheck`. Utan skjema og utan port → `http://<input>:8123`. Utan skjema med port → `http://<input>`.
  - `fun isPrivateHost(host: String): Boolean`

- [x] **Step 1: Skriv testane som feilar**

```kotlin
class HaAddressTest {
    @Test fun bareHostGetsDefaultPort() = assertEquals(AddressCheck.Ok(HaAddress("http://homeassistant.local:8123", "ws://homeassistant.local:8123/api/websocket")), checkHaAddress("homeassistant.local"))
    @Test fun httpsPublicOk() = assertEquals("wss://ha.example.no/api/websocket", (checkHaAddress("https://ha.example.no/") as AddressCheck.Ok).address.websocket)
    @Test fun httpPublicRejected() = assertEquals(AddressCheck.InsecurePublic, checkHaAddress("http://ha.example.no"))
    @Test fun privateRanges() {
        listOf("10.0.0.5", "172.16.1.1", "172.31.255.1", "192.168.1.10", "127.0.0.1", "ha.local", "ha.lan", "ha.home.arpa").forEach { assertTrue(it, isPrivateHost(it)) }
        listOf("172.32.0.1", "8.8.8.8", "ha.example.no").forEach { assertFalse(it, isPrivateHost(it)) }
    }
    @Test fun garbageInvalid() = assertEquals(AddressCheck.Invalid, checkHaAddress("http://"))
}
```

- [x] **Step 2: Køyr og sjå feil**

Run: `pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest --tests "app.stova.ha.HaAddressTest"`
Expected: FAIL.

- [x] **Step 3: Implementer `HaAddress.kt`**

- [x] **Step 4: Køyr testen**

Expected: PASS.

- [x] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(ha): address normalisation with LAN-only plain HTTP"
```

---

### Task 2: OAuth og token-livssyklus

**Files:**
- Create: `app/src/main/java/app/stova/ha/HaAuth.kt`
- Modify: `app/src/main/java/app/stova/settings/HouseholdSettings.kt` (legg til `haBaseUrl: String? = null`), `AppContainer.kt` (`haAuth`)
- Test: `app/src/test/java/app/stova/ha/HaAuthTest.kt`

**Interfaces:**
- Consumes: `SecretStore`, `HaAddress`, `StovaJson`.
- Produces:
  - `const val HA_CLIENT_ID`, `const val HA_REDIRECT_URI` (verdiar i Global Constraints)
  - `@Serializable data class HaTokens(val access: String, val expiresAt: Long, val refresh: String? = null)` (`refresh == null` → langtidstoken, `expiresAt = Long.MAX_VALUE`)
  - `fun authorizeUrl(address: HaAddress, state: String): String` → `"${base}/auth/authorize?response_type=code&client_id=<enc>&redirect_uri=<enc>&state=<enc>"`
  - `fun parseCallback(url: String, expectedState: String): String?`: `code` når `url` startar med `HA_REDIRECT_URI` og `state` stemmer, elles null.
  - `enum class HaAuthState { SIGNED_OUT, SIGNED_IN }`, `class HaSignedOut : Exception()`
  - `class HaAuth(http: OkHttpClient, secrets: SecretStore, address: suspend () -> HaAddress?, clock: () -> Long = System::currentTimeMillis)` med:
    - `val state: StateFlow<HaAuthState>`
    - `suspend fun exchange(code: String)`: `POST /auth/token` som form med `grant_type=authorization_code`, `code` og `client_id`.
    - `suspend fun useLongLivedToken(token: String)`
    - `suspend fun accessToken(): String`: fornyar med `grant_type=refresh_token`, `refresh_token` og `client_id` når mindre enn 60 s er att. 400/401 på fornying → tokens blir sletta, `SIGNED_OUT`, og `HaSignedOut` blir kasta. Ein `Mutex` hindrar parallelle fornyingar.
    - `suspend fun invalidateAccess()`: set `expiresAt = 0`, så neste kall fornyar.
    - `suspend fun signOut()`: `POST /auth/revoke` (`token=<refresh>`, `action=revoke`) som beste forsøk, slett tokens og set `SIGNED_OUT`.

- [x] **Step 1: Skriv testane som feilar**

```kotlin
class HaAuthTest {
    @get:Rule val server = MockWebServer()
    private var now = 1_000_000L; private val secrets = InMemorySecretStore()
    private fun auth() = HaAuth(OkHttpClient(), secrets, { HaAddress(server.url("/").toString().trimEnd('/'), "") }, { now })
    @Test fun authorizeUrlEncodes() = assertEquals(
        "http://ha:8123/auth/authorize?response_type=code&client_id=https%3A%2F%2Foyvhov.github.io%2Fstova%2F&redirect_uri=https%3A%2F%2Foyvhov.github.io%2Fstova%2Fauth-callback&state=s1",
        authorizeUrl(HaAddress("http://ha:8123", ""), "s1"))
    @Test fun callbackNeedsMatchingState() {
        assertEquals("c1", parseCallback("https://oyvhov.github.io/stova/auth-callback?code=c1&state=s1", "s1"))
        assertNull(parseCallback("https://oyvhov.github.io/stova/auth-callback?code=c1&state=evil", "s1"))
        assertNull(parseCallback("https://evil.example/auth-callback?code=c1&state=s1", "s1"))
    }
    @Test fun exchangeThenRefreshNearExpiry() = runTest {
        server.enqueue(MockResponse().setBody("""{"access_token":"a1","expires_in":1800,"refresh_token":"r1","token_type":"Bearer"}"""))
        val a = auth(); a.exchange("c1"); assertEquals("a1", a.accessToken())
        now += 1_741_000
        server.enqueue(MockResponse().setBody("""{"access_token":"a2","expires_in":1800,"token_type":"Bearer"}"""))
        assertEquals("a2", a.accessToken())
        server.takeRequest(); assertTrue(server.takeRequest().body.readUtf8().contains("grant_type=refresh_token"))
    }
    @Test fun revokedRefreshSignsOut() = runTest {
        server.enqueue(MockResponse().setBody("""{"access_token":"a1","expires_in":1800,"refresh_token":"r1"}"""))
        val a = auth(); a.exchange("c1"); a.invalidateAccess()
        server.enqueue(MockResponse().setResponseCode(400).setBody("""{"error":"invalid_grant"}"""))
        assertThrows(HaSignedOut::class.java) { runBlocking { a.accessToken() } }
        assertEquals(HaAuthState.SIGNED_OUT, a.state.value); assertNull(secrets.read("ha/tokens"))
    }
    @Test fun longLivedNeverRefreshes() = runTest { val a = auth(); a.useLongLivedToken("ll"); now = Long.MAX_VALUE - 1; assertEquals("ll", a.accessToken()) }
}
```

- [x] **Step 2: Køyr og sjå feil**

Run: `pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest --tests "app.stova.ha.HaAuthTest"`
Expected: FAIL.

- [x] **Step 3: Implementer `HaAuth.kt` og `haBaseUrl`**

- [x] **Step 4: Køyr testen**

Expected: PASS.

- [x] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(ha): OAuth code exchange, refresh, revoke and long-lived token"
```

---

### Task 3: Innlogging på TV-en

**Files:**
- Create: `app/src/main/java/app/stova/ha/ui/HaSetupFlow.kt`, `HaLoginWebView.kt`
- Modify: `settings/SettingsScreen.kt` (legg til `SettingsSection.SMART_HOME`, berre for vaksne), `app/src/main/res/values*/strings.xml`
- Test: `app/src/test/java/app/stova/ha/ui/HaSetupFlowTest.kt`

**Interfaces:**
- Consumes: `checkHaAddress`, `authorizeUrl`, `parseCallback`, `HaAuth`, `JsonFileStore<HouseholdSettings>`, `RemoteTextField`, `StovaButton`.
- Produces:
  - `class HaLoginClient(expectedState: String, onCode: (String) -> Unit) : WebViewClient()`: `shouldOverrideUrlLoading` returnerer `true` og kallar `onCode` når `parseCallback` gir ein kode. Andre URL-ar blir lasta i WebView.
  - `@Composable fun HaLoginWebView(address: HaAddress, onCode: (String) -> Unit, onCancel: () -> Unit)`: `AndroidView(WebView)` med JavaScript på og `state` = 16 tilfeldige byte i Base64-URL. Cookies blir tømde når flyten er ferdig.
  - `@Composable fun HaSetupFlow(container: AppContainer, onDone: () -> Unit)`: adressefelt → «Logg inn» (WebView) eller «Bruk langtidstoken» (tekstfelt). Ved suksess blir `haBaseUrl` lagra.
    - Feilmeldingar: «Ugyldig adresse», «Bruk https for adresser utanfor heimenettet.», «Innlogginga feila. Prøv igjen.»
  - Innstillingar → Smarthus (hushald): status («Tilkopla som … / Ikkje tilkopla / Logg inn på nytt»), «Kople til» / «Logg ut», og per barneprofil brytaren «Kan styre smarthus» (`Profile.kidSmartHomeControl`, sjå oppgåve 6).

- [x] **Step 1: Skriv testane som feilar**

```kotlin
@RunWith(AndroidJUnit4::class) @Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class HaSetupFlowTest {
    @get:Rule val compose = createComposeRule()
    @Test fun loginClientInterceptsOnlyMatchingCallback() {
        var code: String? = null; val c = HaLoginClient("s1") { code = it }
        assertTrue(c.shouldOverrideUrlLoading(WebView(context), FakeRequest("https://oyvhov.github.io/stova/auth-callback?code=c1&state=s1")))
        assertEquals("c1", code)
        assertFalse(c.shouldOverrideUrlLoading(WebView(context), FakeRequest("http://ha.local:8123/auth/login")))
    }
    @Test fun insecurePublicAddressShowsMessage() {
        compose.setContent { StovaTheme { HaSetupFlow(container) {} } }
        compose.onNodeWithText("Adresse").performTextInput("http://ha.example.no"); compose.onNodeWithText("Logg inn").performClick()
        compose.onNodeWithText("Bruk https for adresser utanfor heimenettet.").assertExists()
    }
    @Test fun longLivedTokenSavesAndSignsIn() { /* "Bruk langtidstoken" → skriv "ll" → "Lagre" → haAuth.state == SIGNED_IN, household.haBaseUrl == "http://ha.local:8123" */ }
}
```
(`FakeRequest` er ein liten `WebResourceRequest`-implementasjon i testen.)

- [x] **Step 2: Køyr og sjå feil**

Run: `pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest --tests "app.stova.ha.ui.*"`
Expected: FAIL.

- [x] **Step 3: Implementer flyten, WebView-klienten og innstillingsseksjonen**

- [x] **Step 4: Køyr testane**

Run: `pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest lintDebug`
Expected: PASS, 0 lint-feil.

- [x] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(ha): TV sign-in via WebView OAuth or long-lived token"
```

---

### Task 4: Dekoding av `subscribe_entities`

**Files:**
- Create: `app/src/main/java/app/stova/ha/EntityStateDecoder.kt`
- Test: `app/src/test/java/app/stova/ha/EntityStateDecoderTest.kt`

**Interfaces:**
- Produces:
  - `data class HaEntity(val id: String, val state: String, val attributes: JsonObject, val lastChanged: Long, val lastUpdated: Long)` (millisekund)
  - `object EntityStateDecoder { fun apply(current: Map<String, HaEntity>, event: JsonObject): Map<String, HaEntity> }`
- Algoritme (komprimert format frå HA):
  - `event.a`: `{id: {s, a?, c?, lc, lu?}}` → ny `HaEntity(id, s, a ?: {}, lc*1000, (lu ?: lc)*1000)`, som erstattar eksisterande.
  - `event.c`: `{id: {"+": {s?, a?, c?, lc?, lu?}, "-": {a: [nøklar]}}}`. Ukjend id blir hoppa over.
    - `+.s` → `state`.
    - `+.lc` → `lastChanged` og `lastUpdated`. `+.lu` → `lastUpdated`.
    - `+.a` → attributta blir slått saman grunt (nye nøklar overskriv).
    - `-.a` → dei nøklane blir fjerna frå attributta.
  - `event.r`: `[id]` → fjerna.
  - `c` (context) blir ignorert. Tidsstempel er sekund som `Double`.

- [x] **Step 1: Skriv testane som feilar**

```kotlin
class EntityStateDecoderTest {
    private fun j(s: String) = StovaJson.parseToJsonElement(s).jsonObject
    private val added = EntityStateDecoder.apply(emptyMap(), j("""{"a":{"light.stove":{"s":"on","a":{"brightness":200,"friendly_name":"Stove"},"c":"x","lc":1700000000.5}}}"""))
    @Test fun add() { val e = added.getValue("light.stove"); assertEquals("on", e.state); assertEquals(1_700_000_000_500, e.lastChanged); assertEquals(e.lastChanged, e.lastUpdated) }
    @Test fun changeStateAndMergeAttributes() {
        val e = EntityStateDecoder.apply(added, j("""{"c":{"light.stove":{"+":{"s":"off","lc":1700000100.0,"a":{"brightness":0}}}}}""")).getValue("light.stove")
        assertEquals("off", e.state); assertEquals(0, e.attributes["brightness"]!!.jsonPrimitive.int); assertEquals("Stove", e.attributes["friendly_name"]!!.jsonPrimitive.content)
    }
    @Test fun removeAttribute() = assertNull(EntityStateDecoder.apply(added, j("""{"c":{"light.stove":{"-":{"a":["brightness"]}}}}""")).getValue("light.stove").attributes["brightness"])
    @Test fun removeEntity() = assertFalse(EntityStateDecoder.apply(added, j("""{"r":["light.stove"]}""")).containsKey("light.stove"))
    @Test fun changeForUnknownIgnored() = assertEquals(added, EntityStateDecoder.apply(added, j("""{"c":{"switch.x":{"+":{"s":"on"}}}}""")))
}
```

- [x] **Step 2: Køyr og sjå feil**

Run: `pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest --tests "app.stova.ha.EntityStateDecoderTest"`
Expected: FAIL.

- [x] **Step 3: Implementer `EntityStateDecoder`**

- [x] **Step 4: Køyr testen**

Expected: PASS.

- [x] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(ha): decode compressed subscribe_entities events"
```

---

### Task 5: WebSocket-klient

**Files:**
- Create: `app/src/main/java/app/stova/ha/HaSocket.kt`, `HaProtocol.kt`
- Modify: `AppContainer.kt` (`haSocket`), `MainActivity.kt` (`onStart` → `haSocket.start()`, `onStop` → `haSocket.stop()`)
- Test: `app/src/test/java/app/stova/ha/HaSocketTest.kt`, `HaProtocolTest.kt`

**Interfaces:**
- Consumes: `HaAuth.accessToken`, `HaAuth.invalidateAccess`, `HaSignedOut`, `HaAddress`, `EntityStateDecoder`, `HaEntity`.
- Produces:
  - `HaProtocol.kt` (reine funksjonar som byggjer og les meldingar):
    - Byggjarar: `fun authMessage(token: String): String`, `fun supportedFeatures(id: Int): String`, `fun subscribeEntities(id: Int, ids: Set<String>): String` (sorterte `entity_ids`), `fun unsubscribe(id: Int, subscription: Int): String`, `fun callService(id: Int, domain: String, service: String, entityId: String, data: JsonObject): String` (`target.entity_id` og `service_data`), `fun historyRequest(id: Int, entityId: String, start: Instant): String` (`minimal_response`, `no_attributes`, `significant_changes_only = false`) og `fun getStates(id: Int): String`.
    - `fun splitIncoming(text: String): List<JsonObject>` tek både éin melding og ein array (coalesce).
    - `fun parseHistory(result: JsonObject, entityId: String): List<Pair<Long, Double>>` les `s` som tal og `lu` (eller `lc`). Ikkje-numeriske punkt blir hoppa over.
    - `fun parseStates(result: JsonArray): List<HaEntity>` les fullt format (`entity_id`, `state`, `attributes`, `last_changed`/`last_updated` som ISO).
  - `enum class HaStatus { NOT_CONFIGURED, DISCONNECTED, CONNECTING, CONNECTED, SIGNED_OUT }`
  - `class HaSocket(http: OkHttpClient, auth: HaAuth, address: suspend () -> HaAddress?, scope: CoroutineScope, backoff: List<Long> = listOf(1_000, 2_000, 4_000, 8_000, 16_000, 30_000))` med:
    - `val status: StateFlow<HaStatus>`, `val entities: StateFlow<Map<String, HaEntity>>`
    - `fun watch(entityIds: Set<String>)`: avsluttar det gamle abonnementet og abonnerer på nytt berre når settet har endra seg.
    - `fun start()`, `fun stop(graceMillis: Long = 30_000)`
    - `suspend fun callService(domain: String, service: String, entityId: String, data: JsonObject = JsonObject(emptyMap())): Boolean`
    - `suspend fun history(entityId: String, hours: Int = 24): List<Pair<Long, Double>>`
    - `suspend fun states(): List<HaEntity>`
  - Flyt:
    1. Opne → `auth_required` → `auth`.
    2. `auth_ok`: `supported_features` (id 1), deretter `subscribe_entities` (aukande id).
    3. `auth_invalid`: `invalidateAccess()`. Eitt nytt forsøk med fornya token, deretter `SIGNED_OUT` (stopp).
    4. Lukka eller feil: `DISCONNECTED` og ny tilkopling etter backoff-lista. Vellukka `auth_ok` nullstiller backoff.
    5. `pingInterval(30 s)` på OkHttp-klienten.
    6. Ventande kall blir fullførte med `false`/tom liste når tilkoplinga fell.

- [ ] **Step 1: Skriv testane som feilar**

```kotlin
class HaProtocolTest {
    @Test fun coalescedArraySplit() = assertEquals(2, splitIncoming("""[{"id":2,"type":"event"},{"id":2,"type":"event"}]""").size)
    @Test fun subscribeSortsIds() = assertTrue(subscribeEntities(3, setOf("sensor.b", "light.a")).contains(""""entity_ids":["light.a","sensor.b"]"""))
    @Test fun historySkipsNonNumeric() = assertEquals(listOf(1_700_000_000_000L to 21.5),
        parseHistory(StovaJson.parseToJsonElement("""{"sensor.t":[{"s":"21.5","lu":1700000000.0},{"s":"unavailable","lu":1700000060.0}]}""").jsonObject, "sensor.t"))
}
class HaSocketTest {
    @get:Rule val server = MockWebServer()
    // FakeHaServer: WebSocketListener som svarar auth_required → (auth) → auth_ok, svarar result på supported_features,
    // og på subscribe_entities sender result + event {"a":{"light.stove":{"s":"on","a":{},"lc":1.0}}}.
    @Test fun connectsAuthenticatesAndReceivesEntities() = runTest { /* start(); watch(setOf("light.stove")) → entities.first { it.isNotEmpty() }["light.stove"]!!.state == "on"; status == CONNECTED; FakeHaServer har motteke supported_features med coalesce_messages */ }
    @Test fun reconnectsAndResubscribesAfterDrop() = runTest { /* FakeHaServer lukkar etter første event; advanceTimeBy(1_000) → ny tilkopling, nytt subscribe_entities med same ids, berre eitt aktivt abonnement */ }
    @Test fun authInvalidTwiceSignsOut() = runTest { /* FakeHaServer svarar auth_invalid alltid → status == SIGNED_OUT etter to forsøk, ingen fleire tilkoplingar etter advanceTimeBy(60_000) */ }
    @Test fun callServiceSendsTargetAndReturnsSuccess() = runTest { /* callService("light","toggle","light.stove") → FakeHaServer får {"type":"call_service","domain":"light","service":"toggle","target":{"entity_id":"light.stove"}} og svarar success → true */ }
}
```

- [ ] **Step 2: Køyr og sjå feil**

Run: `pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest --tests "app.stova.ha.HaProtocolTest" --tests "app.stova.ha.HaSocketTest"`
Expected: FAIL.

- [ ] **Step 3: Implementer `HaProtocol.kt`, `HaSocket.kt` og livssyklusen i `MainActivity`**

Socketen bruker `scope` med ein `Dispatchers.IO`-basert `SupervisorJob`. All skriving til `entities` skjer på éin serialisert korutine (`Channel` av innkomne meldingar).

- [ ] **Step 4: Køyr testane**

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(ha): websocket client with coalesced subscriptions, reconnect backoff and service calls"
```

---

### Task 6: Kortmodell, profilfelt og handlingar

**Files:**
- Create: `app/src/main/java/app/stova/ha/cards/CardModel.kt`, `CardActions.kt`, `CardFormat.kt`
- Modify: `app/src/main/java/app/stova/profiles/Profile.kt` (legg til `dashboard: List<CardConfig> = emptyList()` og `kidSmartHomeControl: Boolean = false`), `AppContainer.kt` (`haRepository`)
- Create: `app/src/main/java/app/stova/ha/HaRepository.kt`
- Test: `app/src/test/java/app/stova/ha/cards/CardModelTest.kt`, `CardActionsTest.kt`, `CardFormatTest.kt`, `app/src/test/java/app/stova/ha/HaRepositoryTest.kt`

**Interfaces:**
- Consumes: `HaEntity`, `HaSocket.watch`, `HaSocket.callService`, `ProfileSession.active`, `ProfileRepository.update`.
- Produces:
  - `@Serializable enum class CardType { LIGHT, TOGGLE, CLIMATE, SENSOR, WEATHER, ENERGY_PRICE, MEDIA_PLAYER, SCENE, CAMERA }`
  - `@Serializable data class CardConfig(val id: String, val type: CardType, val entityId: String, val wide: Boolean = false, val onHome: Boolean = false)`
  - `fun cardTypesFor(entity: HaEntity): List<CardType>`. Etter domene:
    - `light` → LIGHT
    - `switch`/`input_boolean`/`fan` → TOGGLE
    - `climate` → CLIMATE
    - `sensor` → [ENERGY_PRICE, SENSOR] når attributta har ein `raw_today`- eller `today`-array, elles [SENSOR]
    - `binary_sensor` → SENSOR
    - `weather` → WEATHER
    - `media_player` → MEDIA_PLAYER
    - `scene`/`script` → SCENE
    - `camera` → CAMERA
    - anna → tom liste
  - `data class ServiceCall(val domain: String, val service: String, val data: JsonObject = JsonObject(emptyMap()))`
  - `fun primaryAction(type: CardType, entity: HaEntity): ServiceCall?`:
    - LIGHT → `light.toggle`
    - TOGGLE → `homeassistant.toggle`
    - MEDIA_PLAYER → `media_player.media_play_pause`
    - SCENE → `scene.turn_on` eller `script.turn_on` etter domene
    - anna → null (opnar dialog eller gjer ingenting)
  - `fun longAction(type: CardType, entity: HaEntity): ServiceCall?`: MEDIA_PLAYER → `media_player.media_next_track`, anna → null.
  - `fun brightnessCall(percent: Int): ServiceCall` → `light.turn_on {brightness_pct: percent.coerceIn(1, 100)}`
  - `fun climateCall(entity: HaEntity, delta: Double): ServiceCall` → `climate.set_temperature {temperature}`. Gjeldande `temperature` + delta, runda til 0,5 og klemt til `min_temp..max_temp`.
  - `fun canControl(profile: Profile): Boolean = !profile.kid || profile.kidSmartHomeControl`
  - `sealed interface StateText { data class Number(val value: Double, val unit: String?) : StateText; data class OnOff(val on: Boolean) : StateText; data class Raw(val text: String) : StateText; data object Unavailable : StateText }`
  - `fun stateText(entity: HaEntity?): StateText`:
    - null, `unavailable` eller `unknown` → Unavailable
    - `on`/`off` → OnOff
    - tal → Number med `unit_of_measurement`
    - anna → Raw
  - `fun formatNumber(value: Double, locale: Locale): String`: maks éin desimal, fjernar `,0`.
  - `fun priceCurve(entity: HaEntity): List<Double>`: `raw_today[].value`, elles `today[]`.
  - `fun friendlyName(entity: HaEntity): String`: `friendly_name`, elles `entity_id`.
  - `class HaRepository(socket: HaSocket, session: ProfileSession, profiles: ProfileRepository, scope: CoroutineScope)` med:
    - `val cards: StateFlow<List<CardConfig>>`
    - `val homeCards: StateFlow<List<CardConfig>>`: `onHome`, maks 3.
    - `suspend fun add(entity: HaEntity, type: CardType)`
    - `suspend fun remove(cardId: String)`
    - `suspend fun move(cardId: String, delta: Int)`
    - `suspend fun setOnHome(cardId: String, on: Boolean): Boolean`: false når 3 alt er på.
    - `suspend fun setWide(cardId: String, wide: Boolean)`
    - `suspend fun perform(call: ServiceCall, entityId: String, profile: Profile): Boolean`: gjer ingenting og returnerer false når `!canControl(profile)`.
    - Kallar `socket.watch(cards.map { it.entityId }.toSet())` kvar gong korta eller den aktive profilen endrar seg.

- [ ] **Step 1: Skriv testane som feilar**

```kotlin
class CardModelTest {
    private fun e(id: String, state: String = "on", attrs: String = "{}") = HaEntity(id, state, StovaJson.parseToJsonElement(attrs).jsonObject, 0, 0)
    @Test fun nordpoolOffersPriceFirst() = assertEquals(listOf(CardType.ENERGY_PRICE, CardType.SENSOR), cardTypesFor(e("sensor.nordpool", "1.35", """{"today":[1.2,1.35]}""")))
    @Test fun domains() {
        assertEquals(listOf(CardType.TOGGLE), cardTypesFor(e("fan.kitchen"))); assertEquals(listOf(CardType.SCENE), cardTypesFor(e("script.goodnight")))
        assertEquals(emptyList<CardType>(), cardTypesFor(e("automation.x")))
    }
}
class CardActionsTest {
    private val climate = HaEntity("climate.stue", "heat", StovaJson.parseToJsonElement("""{"temperature":21.5,"min_temp":5,"max_temp":22}""").jsonObject, 0, 0)
    @Test fun climateStepsAndClamps() {
        assertEquals(22.0, climateCall(climate, +0.5).data["temperature"]!!.jsonPrimitive.double, 0.0)
        assertEquals(22.0, climateCall(climate, +2.0).data["temperature"]!!.jsonPrimitive.double, 0.0)
    }
    @Test fun scriptUsesScriptDomain() = assertEquals(ServiceCall("script", "turn_on"), primaryAction(CardType.SCENE, HaEntity("script.x", "off", JsonObject(emptyMap()), 0, 0)))
    @Test fun brightnessClamped() = assertEquals(1, brightnessCall(0).data["brightness_pct"]!!.jsonPrimitive.int)
    @Test fun kidWithoutPermissionCannotControl() { assertFalse(canControl(Profile("k", "K", AvatarColor.ROSE, kid = true))); assertTrue(canControl(Profile("k", "K", AvatarColor.ROSE, kid = true, kidSmartHomeControl = true))) }
}
class CardFormatTest {
    @Test fun unavailable() { assertEquals(StateText.Unavailable, stateText(null)); assertEquals(StateText.Unavailable, stateText(HaEntity("sensor.x", "unavailable", JsonObject(emptyMap()), 0, 0))) }
    @Test fun numberWithUnitNynorsk() {
        val s = stateText(HaEntity("sensor.t", "21.0", StovaJson.parseToJsonElement("""{"unit_of_measurement":"°C"}""").jsonObject, 0, 0)) as StateText.Number
        assertEquals("21", formatNumber(s.value, Locale.forLanguageTag("nn"))); assertEquals("4,1", formatNumber(4.06, Locale.forLanguageTag("nn"))); assertEquals("°C", s.unit)
    }
    @Test fun rawTodayPreferred() = assertEquals(listOf(1.0, 2.0), priceCurve(HaEntity("sensor.p", "1", StovaJson.parseToJsonElement("""{"raw_today":[{"value":1.0},{"value":2.0}],"today":[9.0]}""").jsonObject, 0, 0)))
}
class HaRepositoryTest {
    @Test fun watchFollowsActiveProfileCards() = runTest { /* FakeSocket registrerer watch-kall; add(light.stove) → siste watch == {"light.stove"}; byt profil → watch == den nye profilen sine id-ar */ }
    @Test fun maxThreeOnHome() = runTest { /* 4 kort; setOnHome × 4 → fjerde returnerer false, homeCards.size == 3 */ }
    @Test fun kidPerformIsNoop() = runTest { /* perform(...) med barneprofil → false, FakeSocket fekk ingen callService */ }
}
```

- [ ] **Step 2: Køyr og sjå feil**

Run: `pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest --tests "app.stova.ha.cards.*" --tests "app.stova.ha.HaRepositoryTest"`
Expected: FAIL.

- [ ] **Step 3: Implementer filene, profilfelta og `HaRepository`**

`ProfileRepository.create(kid = true)` set `kidSmartHomeControl = false` eksplisitt (standardverdien).

- [ ] **Step 4: Køyr testane**

Run: `pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(ha): card model, service-call mapping, formatting and per-profile dashboards"
```

---

### Task 7: Korta på skjermen

**Files:**
- Create: `app/src/main/java/app/stova/core/ui/StovaIcons.kt` (ikon som korta treng, teikna som `ImageVector`, 24-rutenett, 2 dp strek, same stil som `SpoleIcons.kt` i Spole)
- Create: `app/src/main/java/app/stova/ha/cards/HaCard.kt`, `CardFrame.kt`, `Sparkline.kt`, `LightCard.kt`, `ToggleCard.kt`, `ClimateCard.kt`, `SensorCard.kt`, `WeatherCard.kt`, `EnergyPriceCard.kt`, `MediaPlayerCard.kt`, `SceneCard.kt`, `CameraCard.kt`, `CardDialogs.kt`
- Test: `app/src/test/java/app/stova/ha/cards/HaCardTest.kt`, `SparklineTest.kt`

**Interfaces:**
- Consumes: `CardConfig`, `HaEntity`, `stateText`, `formatNumber`, `priceCurve`, `primaryAction`, `longAction`, `brightnessCall`, `climateCall`, `HaRepository.perform`, `HaSocket.history`, `focusOutline`.
- Produces:
  - `fun sparklinePoints(values: List<Double>, width: Float, height: Float): List<Offset>`: normalisert mellom min og maks. Flat linje midt på når alle verdiane er like. Tom liste ved under to punkt.
  - `@Composable fun CardFrame(config: CardConfig, icon: ImageVector, label: String, badge: String?, locked: Boolean, onClick: () -> Unit, onLongClick: () -> Unit, content: @Composable () -> Unit)`: storleik og stil frå Global Constraints. Ved `locked` ligg ein hengelås i hjørnet, og `onClick` gjer ingenting.
  - `@Composable fun HaCard(config: CardConfig, entity: HaEntity?, locked: Boolean, perform: (ServiceCall) -> Unit, openDialog: (CardDialog) -> Unit)`: vel kort etter `config.type`. Utan entitet → «Utilgjengeleg».
  - `sealed interface CardDialog { data class Brightness(val entityId: String); data class Climate(val entityId: String); data class Graph(val entityId: String); data class Camera(val entityId: String) }` og `@Composable fun CardDialogHost(dialog: CardDialog?, ...)`:
    - lysstyrke: venstre/høgre ±10 %, sendt 300 ms etter siste tastetrykk
    - klima: opp/ned ±0,5
    - graf: `HaSocket.history` 24 t
    - kamera: fullskjerm, oppdatert kvart 2. s
  - Kamera: `AsyncImage` mot `"$base/api/camera_proxy/$entityId"` med `Authorization: Bearer` via Coil `httpHeaders`. Biletet blir oppdatert kvart 10. s med `cache`-parameter berre medan kortet er synleg (`LifecycleResumeEffect` + synlegheit i `LazyGrid`).
  - Mediaspelar: `entity_picture` (relativ → `base` + sti), `media_title`, `media_artist`, og spel/pause-ikon i lime-sirkel.
  - Vêr: tilstand → ikon (`clear-night`, `cloudy`, `fog`, `hail`, `lightning`, `lightning-rainy`, `partlycloudy`, `pouring`, `rainy`, `snowy`, `snowy-rainy`, `sunny`, `windy`, `windy-variant`, `exceptional`) og `temperature` + `temperature_unit`.
  - Straumpris: noverande verdi + eining, og `Sparkline` for i dag med noverande time markert med ein 6 dp prikk i `Text`.
  - Strengar for alle etikettar og tilstandar (`På`/`Av`, `Utilgjengeleg`, `Lysstyrke`, `Mål`, `Spel`/`Pause`, `Køyr`), med engelske motstykke.

- [ ] **Step 1: Skriv testane som feilar**

```kotlin
class SparklineTest {
    @Test fun normalises() = assertEquals(listOf(Offset(0f, 100f), Offset(50f, 0f), Offset(100f, 50f)), sparklinePoints(listOf(0.0, 10.0, 5.0), 100f, 100f))
    @Test fun flatLineInMiddle() = assertEquals(listOf(Offset(0f, 50f), Offset(100f, 50f)), sparklinePoints(listOf(3.0, 3.0), 100f, 100f))
}
@RunWith(AndroidJUnit4::class) @Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class HaCardTest {
    @get:Rule val compose = createComposeRule()
    @Test fun lightClickPerformsToggle() { /* HaCard(LIGHT, light.stove "on") → klikk → perform fekk ServiceCall("light","toggle") */ }
    @Test fun lockedCardDoesNothingAndShowsLock() { /* locked = true → klikk → perform ikkje kalla; node med contentDescription "Låst" finst */ }
    @Test fun missingEntityShowsUnavailable() { /* entity = null → "Utilgjengeleg" finst, kortet er fokuserbart */ }
    @Test fun sensorShowsValueAndUnit() { /* sensor.ute "4.06" °C → "4,1" og "°C" finst */ }
    @Test fun cardsSurviveDoubleFontScale() { /* fontScale 2f, SensorCard → verdien er synleg (assertIsDisplayed) */ }
}
```

- [ ] **Step 2: Køyr og sjå feil**

Run: `pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest --tests "app.stova.ha.cards.HaCardTest" --tests "app.stova.ha.cards.SparklineTest"`
Expected: FAIL.

- [ ] **Step 3: Implementer ikona, korta og dialogane**

- [ ] **Step 4: Køyr testane (også `DesignRulesTest`)**

Run: `pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(ha): flat Tunet-style cards, dialogs and sparklines"
```

---

### Task 8: Smarthus-sida og redigering

**Files:**
- Create: `app/src/main/java/app/stova/ha/ui/SmartHomeScreen.kt`, `AddCardFlow.kt`, `HaStatusLabel.kt`
- Modify: `app/src/main/java/app/stova/navigation/Destination.kt` (set inn `SMART_HOME` etter `HOME`), `navigation/TopBar.kt` (ny ikonknapp «Smarthus» før Appar, berre når `household.haBaseUrl != null`), `StovaApp.kt` (`SMART_HOME` er ei fullskjermside som Tilbake lukkar; ingen sidemeny)
- Test: `app/src/test/java/app/stova/ha/ui/SmartHomeScreenTest.kt`

**Interfaces:**
- Consumes: `HaRepository`, `HaSocket.status`, `HaSocket.entities`, `HaSocket.states`, `HaCard`, `CardDialogHost`, `cardTypesFor`, `friendlyName`, `canControl`.
- Produces:
  - `@Composable fun SmartHomeScreen(container: AppContainer, profile: Profile)`:
    - Overskrift «Smarthus» + `HaStatusLabel` (`Tilkopla`, `Koplar til …`, `Fråkopla`, `Logg inn på nytt`, i `Muted` utanom `SIGNED_OUT`, som er `Caution`).
    - Rutenett `LazyVerticalGrid(GridCells.Fixed(3))` med 16 dp mellomrom. Breie kort spenner over 2 kolonnar.
    - Tom tilstand: «Ingen kort enno» og knappen «Legg til kort» (berre vaksne).
    - Ikkje sett opp: «Kople til Home Assistant i Innstillingar».
  - Redigeringsmodus (vaksne, knappen «Rediger»): kvar kort får meny «Flytt til venstre», «Flytt til høgre», «Brei / Smal», «Vis på heim» (med melding «Maks tre kort på heim» ved full) og «Fjern».
  - `@Composable fun AddCardFlow(container: AppContainer, onDone: () -> Unit)`: henter `states()` og grupperer etter domene med norske gruppenamn («Lys», «Brytarar», «Klima», «Sensorar», «Vêr», «Mediaspelarar», «Scener og skript», «Kamera»), sortert på `friendlyName`, med søkjefelt øvst. Ved fleire korttypar blir brukaren spurd om type.

- [ ] **Step 1: Skriv testane som feilar**

```kotlin
@RunWith(AndroidJUnit4::class) @Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class SmartHomeScreenTest {
    @get:Rule val compose = createComposeRule()
    @Test fun emptyStateOffersAddForAdult() { /* vaksen, ingen kort → "Ingen kort enno" og "Legg til kort" finst */ }
    @Test fun kidSeesNoEditButton() { /* barneprofil med 1 kort → "Rediger" finst ikkje */ }
    @Test fun addCardFlowGroupsAndAdds() { /* FakeSocket.states = [light.stove (Stove), sensor.ute (Ute)] → "Legg til kort" → "Lys" → "Stove" → profile.dashboard har 1 LIGHT-kort */ }
    @Test fun fourthOnHomeShowsLimit() { /* 4 kort, 3 på heim → "Vis på heim" på det fjerde → "Maks tre kort på heim" */ }
}
```

- [ ] **Step 2: Køyr og sjå feil**

Run: `pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest --tests "app.stova.ha.ui.SmartHomeScreenTest"`
Expected: FAIL.

- [ ] **Step 3: Implementer sida, redigeringa, legg-til-flyten og destinasjonen**

- [ ] **Step 4: Køyr testane**

Run: `pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest lintDebug`
Expected: PASS, 0 lint-feil.

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(ha): Smart home page with grid, edit mode and add-card flow"
```

---

### Task 9: Statusbrikker på heim

**Files:**
- Create: `app/src/main/java/app/stova/ha/ui/HomeStatusChips.kt`
- Modify: `home/HomeScreen.kt` (toppfeltet: brikker til venstre for klokka)
- Test: `app/src/test/java/app/stova/ha/ui/HomeStatusChipsTest.kt`

**Interfaces:**
- Consumes: `HaRepository.homeCards`, `HaSocket.entities`, `stateText`, `formatNumber`, `StovaIcons`.
- Produces:
  - `fun chipText(config: CardConfig, entity: HaEntity?, locale: Locale): ChipContent`, der `data class ChipContent(val icon: CardType, val text: String)`:
    - vêr: «4°»
    - sensor/straumpris: verdi og eining, til dømes «135 øre»
    - lys/brytar: «På»/«Av»
    - mediaspelar: `media_title` (maks 24 teikn med …)
    - utilgjengeleg: «–»
  - `@Composable fun HomeStatusChips(chips: List<ChipContent>)`: maks 3, `Raised`-piller med 12 dp hjørne, ikon 16 dp og `labelLarge`. **Ikkje fokuserbare**, slik at D-pad-flyten på heim ikkje blir forstyrra.

- [ ] **Step 1: Skriv testane som feilar**

```kotlin
class HomeStatusChipsTest {
    private val nn = Locale.forLanguageTag("nn")
    @Test fun weatherShowsDegrees() = assertEquals("4°", chipText(CardConfig("1", CardType.WEATHER, "weather.home"),
        HaEntity("weather.home", "rainy", StovaJson.parseToJsonElement("""{"temperature":4.1,"temperature_unit":"°C"}""").jsonObject, 0, 0), nn).text)
    @Test fun priceWithUnit() = assertEquals("135 øre", chipText(CardConfig("2", CardType.ENERGY_PRICE, "sensor.p"),
        HaEntity("sensor.p", "135", StovaJson.parseToJsonElement("""{"unit_of_measurement":"øre"}""").jsonObject, 0, 0), nn).text)
    @Test fun missingIsDash() = assertEquals("–", chipText(CardConfig("3", CardType.SENSOR, "sensor.x"), null, nn).text)
}
```

- [ ] **Step 2: Køyr og sjå feil**

Run: `pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest --tests "app.stova.ha.ui.HomeStatusChipsTest"`
Expected: FAIL.

- [ ] **Step 3: Implementer `chipText`, `HomeStatusChips` og koplinga i `HomeScreen`**

Vêr-avrunding: heiltal med `formatNumber(round(t))` + «°».

- [ ] **Step 4: Køyr testane**

Run: `pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(ha): up to three status chips next to the clock on home"
```

---

### Task 10: Verifisering mot ekte Home Assistant

**Files:**
- Create: `docs/verification/plan-4.md`

- [ ] **Step 1: Bygg og installer på Stova_TV og Shield**

Run: `pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest assembleDebug lintDebug`, deretter `adb -s <serial> install -r app/build/outputs/apk/debug/app-debug.apk`
Expected: alle testar bestått, 0 lint-feil.

- [ ] **Step 2: Manuell sjekkliste (kryss av i `docs/verification/plan-4.md`)**

- OAuth-innlogging med fjernkontrollen. Etter omstart av Stova er ein framleis innlogga.
- Langtidstoken som alternativ.
- Legg til eitt kort av kvar type frå ditt eige HA. Verdiane stemmer med Tunet.
- Lys av/på frå Stova. Endringa blir synleg i Tunet innan 1 s, og omvendt.
- Statusbrikkene vêr, straumpris og person/lys står til høgre for klokka på heim.
- Start HA på nytt. Status går til «Koplar til …» og tilbake til «Tilkopla» utan handling frå brukaren.
- Barneprofil: korta blir viste med hengelås, og OK gjer ingenting. Med «Kan styre smarthus» på fungerer korta.
- Kamera: snapshot blir oppdatert kvart 10. s, og fullskjerm kvart 2. s.
- Skriftstorleik 2,0: korta er lesbare, og ingen verdiar er klipte.

- [ ] **Step 3: Commit**

```bash
git add -A && git commit -m "docs: plan 4 verification against real Home Assistant"
```
