# Stova Plan 1 – Fundament · implementasjonsplan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ein installerbar Android TV-launcher (`app.stova`) med Spole-tema, profilar med PIN og barnemodus, sidemeny, appar med favorittar og skjulte appar, og Home-knappen kapra på Shield.

**Architecture:** Éin Gradle-modul `:app` i Kotlin med Jetpack Compose (Material3, ikkje tv-material). Manuell DI i `AppContainer`. All persistens er JSON i `AtomicFile` via `JsonFileStore`, og løyndomar er krypterte med AES-GCM-nøkkel i Android Keystore. Logikk ligg i reine Kotlin-funksjonar som blir testa på JVM. Compose-skjermar blir testa med Robolectric i TV-konfigurasjon.

**Tech Stack:** Gradle 9.4.1, AGP 9.2.1, Kotlin 2.3.10 (compose + serialization-plugin), Compose BOM 2026.06.00, kotlinx-serialization-json 1.11.0, coroutines 1.10.2, JUnit 4.13.2, Robolectric 4.14.1, Compose ui-test-junit4.

**Spec:** `docs/superpowers/specs/2026-10-24-stova-launcher-design.md`

## Global Constraints

- Pakke og applicationId: `app.stova`. Debug har `applicationIdSuffix = ".debug"`.
- `minSdk = 30`, `targetSdk = 36`, `compileSdk = 36`, Java/Kotlin-mål 17.
- Alle Gradle-kall går gjennom `scripts/gradle.ps1`. Det set `GRADLE_USER_HOME=C:\JellyBin\.gradle-home` (ASCII-sti, delt cache med Spole) og `TEMP/TMP=C:\Launcher\.gradle-tmp`.
- Ingen `Modifier.blur`, ingen `RenderEffect` og ingen `Color(0x…)` utanfor `app/src/main/java/app/stova/core/theme/`. `DesignRulesTest` håndhevar dette.
- Fargar, hjørne, typografi og fokus nøyaktig som i spesifikasjonen §2. Lime (`#D5F478`) er berre for val, framdrift og handling.
- Tekst i UI kjem frå `res/values/strings.xml` (nynorsk) og `res/values-en/strings.xml` (engelsk). Lint `HardcodedText` er feil.
- Ingen kontroll med fast høgd rundt tekst. Compose-testar for skjermar køyrer også med `fontScale = 2f`.
- TV-testkonfigurasjon for Robolectric: `@Config(sdk = [30], qualifiers = "w960dp-h540dp-land-television-xhdpi")` (Shield = 1920×1080 ved 320 dpi = 960×540 dp).
- Stova blir aldri installert på emulator-5560, -5562 eller -5564 (Spole sine). Stova sin emulator er `Stova_TV` på port 5580. Alle `adb`-kall har `-s emulator-5580` eller `-s <shield-ip>:5555`.
- Ingen nye avhengigheiter utover tech stack-lista utan at planen seier det.
- Tokens, PIN og passord blir aldri logga.
- Variantane i ein `sealed interface X { … }`-blokk under **Interfaces** implementerer alltid `X`, også der `: X` ikkje er skrive ut. Dette gjeld i alle planane.
- Framdrift: hak av `- [ ]` → `- [x]` i planfila etter kvart steg som er gjort, og commit planfila saman med koden. Då kan ein annan agent (Claude eller Codex) halde fram frå første steg som ikkje er avkryssa.

## Review Focus

1. **Launcheren kræsjar ved oppstart på grunn av ei skadd JSON-fil.** Forventa: Stova startar med standardverdiar og tek vare på den skadde fila som `*.corrupt`. Test i oppgåve 3.
2. **Ein barneprofil prøver å kome seg ut via sidemenyen eller Tilbake.** Forventa: byte av profil krev vaksen-PIN når ein vaksen profil har PIN. Tilbake på toppnivå gjer ingenting. Testar i oppgåve 5 og 7.
3. **Home-overstyringa saman med kræsj gir ei evig løkkje.** Forventa: etter 3 kræsj på 5 minutt slår Home-tenesta seg av, og systemlauncheren fungerer. Test i oppgåve 12.
4. **Appar som blir avinstallerte medan dei er favorittar.** Forventa: dei forsvinn frå radene utan kræsj, og favorittlista blir rydda. Test i oppgåve 9.
5. **PIN-gjetting med fjernkontrollen.** Forventa: ventetida følgjer tabellen og overlever omstart av appen. Test i oppgåve 5.

---

### Task 1: Prosjektskjelett som kan setjast som HOME

**Files:**
- Create: `.gitignore`, `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `local.properties`, `scripts/gradle.ps1`
- Create (kopi frå `C:\JellyBin\reelstack-android`): `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, `gradle/wrapper/gradle-wrapper.properties`
- Create: `app/build.gradle.kts`, `app/proguard-rules.pro`, `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/app/stova/StovaApplication.kt`, `app/src/main/java/app/stova/AppContainer.kt`, `app/src/main/java/app/stova/MainActivity.kt`
- Create: `app/src/main/res/values/strings.xml`, `app/src/main/res/values-en/strings.xml`, `app/src/main/res/drawable/tv_banner.xml`, `app/src/main/res/mipmap-anydpi/ic_stova.xml`
- Test: `app/src/test/java/app/stova/HomeIntentTest.kt`, `app/src/test/java/app/stova/TestConfig.kt`

**Interfaces:**
- Produces: `class StovaApplication : Application { lateinit var container: AppContainer }`, `class AppContainer(context: Context)` (tom no; seinare oppgåver legg til felt), `class MainActivity : ComponentActivity` (registrert med `MAIN` + `HOME` + `DEFAULT`, og `MAIN` + `LEANBACK_LAUNCHER`, `launchMode="singleTask"`, `android:banner="@drawable/tv_banner"`), `const val TV_QUALIFIERS = "w960dp-h540dp-land-television-xhdpi"` i `TestConfig.kt`.

- [x] **Step 1: Init repo og byggfiler**

```powershell
git init C:\Launcher; git -C C:\Launcher checkout -b main
```
`scripts/gradle.ps1`:
```powershell
$env:GRADLE_USER_HOME = 'C:\JellyBin\.gradle-home'
$env:TEMP = 'C:\Launcher\.gradle-tmp'; $env:TMP = $env:TEMP
New-Item -ItemType Directory -Force $env:TEMP | Out-Null
& "$PSScriptRoot\..\gradlew.bat" @args
exit $LASTEXITCODE
```
`local.properties`: `sdk.dir=C\:\\Android\\sdk`. `.gitignore`: `.gradle/`, `.gradle-tmp/`, `build/`, `app/build/`, `local.properties`, `signing.properties`, `*.jks`, `.avd/`, `.kotlin/`.
Plugin-versjonar i rot-`build.gradle.kts`: `com.android.application` 9.2.1, `org.jetbrains.kotlin.plugin.compose` 2.3.10, `org.jetbrains.kotlin.plugin.serialization` 2.3.10. `app/build.gradle.kts` følgjer Spole sitt (`C:\JellyBin\reelstack-android\app\build.gradle.kts`): namespace `app.stova`, verdiane i Global Constraints, `buildFeatures { compose = true; buildConfig = true }`, release med `isMinifyEnabled = true` og `isShrinkResources = true`, `lint { error += "HardcodedText" }`, `testOptions.unitTests.isIncludeAndroidResources = true`. Avhengigheiter: compose-bom 2026.06.00 (ui, foundation, material3, ui-tooling-preview), core-ktx 1.17.0, activity-compose 1.13.0, lifecycle-runtime-compose 2.10.0, lifecycle-viewmodel-compose 2.10.0, kotlinx-serialization-json 1.11.0, kotlinx-coroutines-android 1.10.2. Test: junit 4.13.2, kotlinx-coroutines-test 1.10.2, robolectric 4.14.1, ui-test-junit4, androidx.test.ext:junit 1.3.0. debugImplementation: ui-tooling, ui-test-manifest.

- [x] **Step 2: Skriv testen som feilar**

```kotlin
@RunWith(AndroidJUnit4::class) @Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class HomeIntentTest {
    @Test fun mainActivityAnswersHomeIntent() {
        val pm = ApplicationProvider.getApplicationContext<Context>().packageManager
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val names = pm.queryIntentActivities(home, 0).map { it.activityInfo.name }
        assertTrue(names.contains("app.stova.MainActivity"))
    }
    @Test fun mainActivityIsListedOnTvLauncher() {
        val pm = ApplicationProvider.getApplicationContext<Context>().packageManager
        val tv = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        assertTrue(pm.queryIntentActivities(tv, 0).any { it.activityInfo.name == "app.stova.MainActivity" })
    }
}
```

- [x] **Step 3: Køyr testen og sjå at han feilar**

Run: `pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest --tests "app.stova.HomeIntentTest"`
Expected: FAIL (manifestet manglar aktiviteten eller kompilering feilar).

- [x] **Step 4: Lag manifest, `StovaApplication`, `AppContainer` og `MainActivity`**

`MainActivity.onCreate` kallar `enableEdgeToEdge()` og `setContent { }` (tomt innhald no). Manifest: `android:allowBackup="false"`, `android:label="@string/app_name"` (nynorsk og engelsk: «Stova»), `<uses-feature android:name="android.software.leanback" android:required="false"/>`, `<uses-feature android:name="android.hardware.touchscreen" android:required="false"/>`. Tilbake-knappen må ikkje avslutte launcheren: `onBackPressedDispatcher` får ein callback som ikkje gjer noko (navigasjonen overstyrer seinare).

- [x] **Step 5: Køyr testane og bygg**

Run: `pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest assembleDebug lintDebug`
Expected: BUILD SUCCESSFUL, 2 testar bestått, 0 lint-feil.

- [x] **Step 6: Commit**

```bash
git add -A && git commit -m "build: Stova project skeleton registered as HOME launcher"
```

---

### Task 2: Tema, tokens og designreglar

**Files:**
- Create: `app/src/main/java/app/stova/core/theme/Tokens.kt`, `Typography.kt`, `StovaTheme.kt`
- Test: `app/src/test/java/app/stova/core/theme/StovaThemeTest.kt`, `app/src/test/java/app/stova/DesignRulesTest.kt`

**Interfaces:**
- Produces:
  - `object StovaColors { val Ink; val Surface; val Raised; val Text; val Muted; val Primary; val PrimaryContainer; val PrimarySoft; val Success; val Caution; val Warning; val Outline; val Divider }` med verdiane frå spesifikasjonen §2. I tillegg, frå Spole sin `VisualTheme.FOREST` (`C:\JellyBin\reelstack-android\app\src\main\java\app\reelstack\data\model\Personalization.kt:49`): `PrimarySoft = #DCE9BD`, `Outline = #646E63`, `Divider = #3D443C`.
  - `object StovaShapes { val ExtraSmall = 4.dp; val Small = 8.dp; val Medium = 12.dp; val Large = 16.dp; val ExtraLarge = 24.dp }`
  - `object StovaLayout { val TvSafeEdge = 48.dp; val Gutter = 24.dp; val RailCollapsed = 80.dp; val RailExpanded = 200.dp; val PosterWidth = 132.dp; val PosterHeight = 198.dp; val WideWidth = 248.dp; val WideHeight = 139.5.dp; val RowGap = 16.dp; val SectionTop = 22.dp; val SectionBottom = 10.dp; const val TransitionMillis = 180; const val FocusFadeMillis = 110 }`
  - `enum class AvatarColor(val argb: Long) { LIME(0xFFD5F478), OCEAN(0xFF96D5FF), IRIS(0xFFCAB8FF), CORAL(0xFFFFB5A0), GOLD(0xFFFFD478), MINT(0xFF86E2C2), ROSE(0xFFF4ABD0), PEARL(0xFFE3E7EF) }`, `@Serializable`, i `Tokens.kt`.
  - `fun stovaTypography(): Typography` er ein kopi av `reelstackTypography()` (`C:\JellyBin\reelstack-android\app\src\main\java\app\reelstack\ui\theme\Typography.kt`).
  - `val LocalReduceMotion = staticCompositionLocalOf { false }`, `val LocalStrongFocus = staticCompositionLocalOf { false }`.
  - `@Composable fun StovaTheme(reduceMotion: Boolean = false, strongFocus: Boolean = false, content: @Composable () -> Unit)` set `MaterialTheme(darkColorScheme(...))` med alle roller eksplisitte (sjå `ReelstackColors` i Spole), `Shapes` frå `StovaShapes`, `LocalContentColor provides StovaColors.Text` og dei to locals.

- [x] **Step 1: Skriv testane som feilar**

```kotlin
class DesignRulesTest {
    private val src = File("src/main/java")
    private fun files() = src.walkTopDown().filter { it.extension == "kt" }
    @Test fun noBlurOrRenderEffect() {
        val offenders = files().filter { f -> f.readText().let { ".blur(" in it || "RenderEffect" in it } }.toList()
        assertEquals(emptyList<File>(), offenders)
    }
    @Test fun noColorLiteralsOutsideTheme() {
        val offenders = files().filterNot { it.path.replace('\\', '/').contains("core/theme/") }
            .filter { Regex("""Color\(0x""").containsMatchIn(it.readText()) }.toList()
        assertEquals(emptyList<File>(), offenders)
    }
}
@RunWith(AndroidJUnit4::class) @Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class StovaThemeTest {
    @get:Rule val compose = createComposeRule()
    @Test fun schemeUsesSpoleForestTokens() {
        lateinit var scheme: ColorScheme
        compose.setContent { StovaTheme { scheme = MaterialTheme.colorScheme } }
        assertEquals(Color(0xFFD5F478), scheme.primary)
        assertEquals(Color(0xFF101211), scheme.background)
        assertEquals(Color(0xFF191C19), scheme.surface)
        assertEquals(Color(0xFF2E332E), scheme.surfaceVariant)
    }
}
```
(Testkjelda kan bruke `Color(0x…)`; regelen gjeld berre `src/main`.)

- [x] **Step 2: Køyr og sjå feil**

Run: `pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest --tests "app.stova.DesignRulesTest" --tests "app.stova.core.theme.*"`
Expected: FAIL (`StovaTheme` finst ikkje).

- [x] **Step 3: Implementer `Tokens.kt`, `Typography.kt` og `StovaTheme.kt`**

`MainActivity` pakkar `setContent` inn i `StovaTheme { Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) }`.

- [x] **Step 4: Køyr testane**

Run: same som Step 2, deretter `pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest`
Expected: PASS.

- [x] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(theme): Spole forest tokens, typography and design rules test"
```

---

### Task 3: JSON-lagring som tåler skadde filer

**Files:**
- Create: `app/src/main/java/app/stova/core/storage/JsonFileStore.kt`
- Test: `app/src/test/java/app/stova/core/storage/JsonFileStoreTest.kt`

**Interfaces:**
- Produces:
  - `val StovaJson: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true; explicitNulls = false }`
  - `class JsonFileStore<T : Any>(file: File, serializer: KSerializer<T>, default: T, io: CoroutineDispatcher = Dispatchers.IO)` med `val data: Flow<T>` (les fila ved første innsamling og sender kvar endring), `suspend fun current(): T` og `suspend fun update(transform: (T) -> T): T` (serialisert med `Mutex`, skriv med `androidx.core.util.AtomicFile`).
  - `fun writeBlocking(transform: (T) -> T): T` er for kræsjhandsamaren (oppgåve 12). Same lås, men synkron (`runBlocking` på kallaren sin tråd).

- [x] **Step 1: Skriv testane som feilar**

```kotlin
@Serializable data class Sample(val n: Int = 0, val s: String = "")
class JsonFileStoreTest {
    @get:Rule val tmp = TemporaryFolder()
    private fun store(f: File = tmp.root.resolve("s.json")) = JsonFileStore(f, Sample.serializer(), Sample(), UnconfinedTestDispatcher())
    @Test fun missingFileGivesDefault() = runTest { assertEquals(Sample(), store().current()) }
    @Test fun updateSurvivesReopen() = runTest {
        val f = tmp.root.resolve("s.json"); store(f).update { it.copy(n = 7) }
        assertEquals(7, store(f).current().n)
    }
    @Test fun concurrentUpdatesAreNotLost() = runTest {
        val s = store(); (1..50).map { launch { s.update { it.copy(n = it.n + 1) } } }.joinAll()
        assertEquals(50, s.current().n)
    }
    @Test fun corruptFileFallsBackToDefaultAndKeepsCopy() = runTest {
        val f = tmp.root.resolve("s.json").apply { writeText("{not json") }
        assertEquals(Sample(), store(f).current())
        assertEquals("{not json", tmp.root.resolve("s.json.corrupt").readText())
    }
    @Test fun unknownKeysAreIgnored() = runTest {
        val f = tmp.root.resolve("s.json").apply { writeText("""{"n":3,"future":true}""") }
        assertEquals(3, store(f).current().n)
    }
    @Test fun dataEmitsEachUpdate() = runTest {
        val s = store(); val seen = mutableListOf<Int>()
        val job = launch { s.data.take(2).collect { seen += it.n } }
        s.update { it.copy(n = 1) }; job.join()
        assertEquals(listOf(0, 1), seen)
    }
}
```

- [x] **Step 2: Køyr og sjå feil**

Run: `pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest --tests "app.stova.core.storage.*"`
Expected: FAIL (klassen finst ikkje).

- [x] **Step 3: Implementer `JsonFileStore`**

Intern `MutableStateFlow<T?>` (null = ikkje lasta). `data` = `flow { emit(current()); emitAll(state.filterNotNull().drop(1)) }`. Ved `SerializationException` blir fila kopiert til `<namn>.corrupt` og standardverdien brukt.

- [x] **Step 4: Køyr testane**

Run: same som Step 2
Expected: PASS (6 testar).

- [x] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(storage): atomic JSON file store with corrupt-file recovery"
```

---

### Task 4: Krypterte løyndomar og PIN-hashing

**Files:**
- Create: `app/src/main/java/app/stova/core/security/SecretStore.kt`, `AesGcmBox.kt`, `KeystoreSecretStore.kt`, `PinHasher.kt`, `PinPolicy.kt`
- Test: `app/src/test/java/app/stova/core/security/AesGcmBoxTest.kt`, `PinHasherTest.kt`, `PinPolicyTest.kt`
- Test (instrumentert): `app/src/androidTest/java/app/stova/core/security/KeystoreSecretStoreTest.kt`

**Interfaces:**
- Consumes: `JsonFileStore` (oppgåve 3).
- Produces:
  - `interface SecretStore { suspend fun read(key: String): String?; suspend fun write(key: String, value: String); suspend fun delete(key: String) }`
  - `class InMemorySecretStore : SecretStore` (i `SecretStore.kt`, brukt av testar i alle planar)
  - `class AesGcmBox(key: () -> SecretKey) { fun seal(plain: ByteArray): ByteArray; fun open(sealed: ByteArray): ByteArray }`. Format: 12 byte IV + chiffertekst med 128-bits tag. `open` kastar `AEADBadTagException` ved manipulering.
  - `class KeystoreSecretStore(context: Context, alias: String = "stova.secrets") : SecretStore` lagrar `Map<String, String>` (Base64 av `seal`) i `files/secrets.json` via `JsonFileStore`. Nøkkelen er AES-256 i `AndroidKeyStore`, `PURPOSE_ENCRYPT or PURPOSE_DECRYPT`, `BLOCK_MODE_GCM`, `ENCRYPTION_PADDING_NONE`.
  - `@Serializable data class PinHash(val salt: String, val hash: String, val iterations: Int = 100_000)` (Base64-strengar)
  - `object PinHasher { fun isValid(pin: String): Boolean; fun hash(pin: String, random: SecureRandom = SecureRandom()): PinHash; fun verify(pin: String, stored: PinHash): Boolean }`. PBKDF2WithHmacSHA256, 16 byte salt, 256 bit, samanlikning med `MessageDigest.isEqual`.
  - `object PinPolicy { fun lockoutMillis(failures: Int): Long }`

- [ ] **Step 1: Skriv testane som feilar**

```kotlin
class AesGcmBoxTest {
    private val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    private val box = AesGcmBox { key }
    @Test fun roundTrip() = assertArrayEquals("hemmeleg".toByteArray(), box.open(box.seal("hemmeleg".toByteArray())))
    @Test fun sameInputGivesDifferentCiphertext() = assertFalse(box.seal(byteArrayOf(1)).contentEquals(box.seal(byteArrayOf(1))))
    @Test(expected = AEADBadTagException::class) fun tamperingIsDetected() {
        val sealed = box.seal("x".toByteArray()); sealed[sealed.lastIndex] = (sealed.last() + 1).toByte(); box.open(sealed)
    }
}
class PinHasherTest {
    @Test fun validPins() { assertTrue(PinHasher.isValid("1234")); assertTrue(PinHasher.isValid("12345678"))
        assertFalse(PinHasher.isValid("123")); assertFalse(PinHasher.isValid("123456789")); assertFalse(PinHasher.isValid("12a4")) }
    @Test fun verifyAcceptsRightAndRejectsWrong() { val h = PinHasher.hash("2468")
        assertTrue(PinHasher.verify("2468", h)); assertFalse(PinHasher.verify("2469", h)) }
    @Test fun usesSpoleStrength() { val h = PinHasher.hash("2468")
        assertEquals(100_000, h.iterations); assertEquals(16, Base64.getDecoder().decode(h.salt).size) }
    @Test fun saltDiffers() = assertNotEquals(PinHasher.hash("2468").hash, PinHasher.hash("2468").hash)
}
class PinPolicyTest {
    @Test fun lockoutTable() = assertEquals(
        listOf(0L, 0, 0, 0, 5_000, 15_000, 30_000, 60_000, 120_000, 300_000, 300_000),
        (0..10).map(PinPolicy::lockoutMillis))
}
```
Instrumentert (`KeystoreSecretStoreTest`): `write("a","b")` → `read("a") == "b"`, råfila `files/secrets.json` inneheld ikkje `"b"`, og `delete("a")` → `read("a") == null`.

- [ ] **Step 2: Køyr og sjå feil**

Run: `pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest --tests "app.stova.core.security.*"`
Expected: FAIL.

- [ ] **Step 3: Implementer filene i `core/security`**

- [ ] **Step 4: Køyr JVM-testane**

Run: same som Step 2
Expected: PASS. (Den instrumenterte testen køyrer i oppgåve 13.)

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(security): keystore-backed secret store and PBKDF2 PIN hashing"
```

---

### Task 5: Profilar – modell, lagring og reglar

**Files:**
- Create: `app/src/main/java/app/stova/profiles/Profile.kt`, `ProfileRepository.kt`, `ProfileRules.kt`
- Create: `app/src/main/java/app/stova/home/HomeRowKey.kt`
- Modify: `app/src/main/java/app/stova/AppContainer.kt` (legg til `profiles`)
- Test: `app/src/test/java/app/stova/profiles/ProfileRepositoryTest.kt`, `ProfileRulesTest.kt`, `app/src/test/java/app/stova/home/HomeRowKeyTest.kt`

**Interfaces:**
- Consumes: `JsonFileStore`, `PinHasher`, `PinPolicy`, `PinHash`, `AvatarColor`.
- Produces:
  - `@Serializable @JvmInline value class HomeRowKey(val value: String)` med `companion object { val APPS; val WATCH_NEXT; fun continueWatching(accountId: String); fun latestMovies(accountId: String); fun latestSeries(accountId: String) }` → `"apps"`, `"watch_next"`, `"continue:<id>"`, `"latest_movies:<id>"`, `"latest_series:<id>"`.
  - `fun arrangeRows(available: List<HomeRowKey>, prefs: HomePrefs): List<HomeRowKey>`. Rader i `prefs.rowOrder` kjem først i den rekkjefølgja, så resten i `available`-rekkjefølgje. Rader i `prefs.hiddenRows` blir fjerna. Nøklar som ikkje finst i `available`, blir ignorerte.
  - `@Serializable data class Profile(val id: String, val name: String, val avatar: AvatarColor, val pin: PinHash? = null, val kid: Boolean = false, val apps: AppPrefs = AppPrefs(), val home: HomePrefs = HomePrefs(), val appearance: AppearancePrefs = AppearancePrefs())`
  - `@Serializable data class AppPrefs(val favorites: List<String> = emptyList(), val hidden: Set<String> = emptySet(), val kidAllowed: Set<String> = emptySet())` (appnøkkel = `"<pakke>/<aktivitet>"`)
  - `@Serializable data class HomePrefs(val startFocus: StartFocus = StartFocus.FIRST_MEDIA_ROW, val rowOrder: List<HomeRowKey> = emptyList(), val hiddenRows: Set<HomeRowKey> = emptySet(), val backdropRotation: Boolean = true)`
  - `@Serializable enum class StartFocus { FIRST_MEDIA_ROW, FAVORITE_APPS }`
  - `@Serializable data class AppearancePrefs(val reduceMotion: Boolean = false, val strongFocus: Boolean = false)`
  - `@Serializable data class PinFailures(val count: Int = 0, val lastFailureAt: Long = 0)`
  - `@Serializable data class ProfilesFile(val profiles: List<Profile> = emptyList(), val lastProfileId: String? = null, val startInLastProfile: Boolean = true, val pinFailures: Map<String, PinFailures> = emptyMap())`
  - `sealed interface PinCheck { data object Accepted : PinCheck; data class Rejected(val waitMillis: Long) : PinCheck; data class LockedOut(val remainingMillis: Long) : PinCheck }`
  - `class ProfileRepository(store: JsonFileStore<ProfilesFile>, clock: () -> Long = System::currentTimeMillis, newId: () -> String = { UUID.randomUUID().toString() })` med `val file: Flow<ProfilesFile>`, `suspend fun create(name: String, avatar: AvatarColor, kid: Boolean): Profile`, `suspend fun update(id: String, transform: (Profile) -> Profile): Profile`, `suspend fun delete(id: String)`, `suspend fun setPin(id: String, pin: String?)`, `suspend fun verifyPin(id: String, pin: String): PinCheck`, `suspend fun rememberLast(id: String)`.
  - `sealed interface StartDecision { data object CreateFirst; data object Pick; data class NeedsPin(val profile: Profile); data class Enter(val profile: Profile) }`
  - `fun decideStart(file: ProfilesFile): StartDecision` og `fun leavingNeedsAdultPin(file: ProfilesFile, current: Profile): Boolean` i `ProfileRules.kt`.
  - `AppContainer.profiles: ProfileRepository` (fil `files/profiles.json`).

- [ ] **Step 1: Skriv testane som feilar**

```kotlin
class ProfileRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()
    private var now = 1_000_000L
    private fun repo() = ProfileRepository(JsonFileStore(tmp.root.resolve("p.json"), ProfilesFile.serializer(), ProfilesFile(), UnconfinedTestDispatcher()), { now })
    @Test(expected = IllegalArgumentException::class) fun firstProfileCannotBeKid() = runTest { repo().create("Maya", AvatarColor.ROSE, kid = true); Unit }
    @Test(expected = IllegalStateException::class) fun lastAdultCannotBeDeleted() = runTest {
        val r = repo(); val a = r.create("Øyvind", AvatarColor.LIME, false); r.create("Maya", AvatarColor.ROSE, true); r.delete(a.id) }
    @Test(expected = IllegalArgumentException::class) fun setPinRejectsInvalid() = runTest { val r = repo(); r.setPin(r.create("A", AvatarColor.LIME, false).id, "12") }
    @Test fun wrongPinsFollowPolicyAndRightPinResets() = runTest {
        val r = repo(); val id = r.create("A", AvatarColor.LIME, false).id; r.setPin(id, "1357")
        repeat(3) { assertEquals(PinCheck.Rejected(0), r.verifyPin(id, "0000")) }
        assertEquals(PinCheck.Rejected(5_000), r.verifyPin(id, "0000"))
        assertEquals(PinCheck.LockedOut(5_000), r.verifyPin(id, "1357"))
        now += 5_000
        assertEquals(PinCheck.Accepted, r.verifyPin(id, "1357"))
        assertEquals(PinCheck.Rejected(0), r.verifyPin(id, "0000"))
    }
    @Test fun lockoutSurvivesRestart() = runTest {
        val r = repo(); val id = r.create("A", AvatarColor.LIME, false).id; r.setPin(id, "1357")
        repeat(4) { r.verifyPin(id, "0000") }
        assertTrue(repo().verifyPin(id, "1357") is PinCheck.LockedOut)
    }
}
class ProfileRulesTest {
    private val adult = Profile("a", "A", AvatarColor.LIME); private val kid = Profile("k", "K", AvatarColor.ROSE, kid = true)
    private val pinned = adult.copy(pin = PinHasher.hash("1357"))
    @Test fun noProfilesCreatesFirst() = assertEquals(StartDecision.CreateFirst, decideStart(ProfilesFile()))
    @Test fun lastProfileWithoutPinEnters() = assertEquals(StartDecision.Enter(adult), decideStart(ProfilesFile(listOf(adult, kid), lastProfileId = "a")))
    @Test fun lastProfileWithPinNeedsPin() = assertEquals(StartDecision.NeedsPin(pinned), decideStart(ProfilesFile(listOf(pinned, kid), lastProfileId = "a")))
    @Test fun startInLastOffPicks() = assertEquals(StartDecision.Pick, decideStart(ProfilesFile(listOf(adult, kid), "a", startInLastProfile = false)))
    @Test fun singleProfileEntersEvenWithoutLast() = assertEquals(StartDecision.Enter(adult), decideStart(ProfilesFile(listOf(adult))))
    @Test fun kidLeavingNeedsPinOnlyWhenAnAdultHasPin() {
        assertFalse(leavingNeedsAdultPin(ProfilesFile(listOf(adult, kid)), kid))
        assertTrue(leavingNeedsAdultPin(ProfilesFile(listOf(pinned, kid)), kid))
        assertFalse(leavingNeedsAdultPin(ProfilesFile(listOf(pinned, kid)), pinned))
    }
}
class HomeRowKeyTest {
    private val j = HomeRowKey.continueWatching("j1"); private val m = HomeRowKey.latestMovies("j1")
    @Test fun preferredOrderFirstThenDefaultsMinusHidden() = assertEquals(
        listOf(m, HomeRowKey.APPS),
        arrangeRows(listOf(HomeRowKey.APPS, j, m), HomePrefs(rowOrder = listOf(m, HomeRowKey("gone")), hiddenRows = setOf(j))))
}
```

- [ ] **Step 2: Køyr og sjå feil**

Run: `pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest --tests "app.stova.profiles.*" --tests "app.stova.home.*"`
Expected: FAIL.

- [ ] **Step 3: Implementer `Profile.kt`, `ProfileRules.kt`, `HomeRowKey.kt` og `ProfileRepository.kt`, og legg `profiles` til i `AppContainer`**

`verifyPin`: Om `now - lastFailureAt < lockoutMillis(count)`, returner `LockedOut(rest)` utan å telje. Ved feil PIN: `count+1`, returner `Rejected(lockoutMillis(count+1))`. Ved rett PIN: fjern innslaget, returner `Accepted`. Ein profil utan PIN gir alltid `Accepted`.

- [ ] **Step 4: Køyr testane**

Run: same som Step 2
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(profiles): profile model, PIN lockout and start rules"
```

---

### Task 6: Kjerne-UI – fokus, knappar, PIN-tastatur, tekstfelt, klokke

**Files:**
- Create: `app/src/main/java/app/stova/core/ui/FocusOutline.kt`, `StovaButton.kt`, `PinDialog.kt`, `RemoteTextField.kt`, `Clock.kt`, `SettingsRows.kt`
- Modify: `app/src/main/res/values/strings.xml`, `values-en/strings.xml`
- Test: `app/src/test/java/app/stova/core/ui/PinDialogTest.kt`, `ClockTest.kt`

**Interfaces:**
- Consumes: `StovaTheme`, `LocalReduceMotion`, `LocalStrongFocus`, `PinCheck`.
- Produces:
  - `fun Modifier.focusOutline(source: InteractionSource, shape: Shape): Modifier`: berre teikning, 3 dp `onSurface` (4 dp når `LocalStrongFocus`), alfa-overgang på 110 ms (0 ms ved redusert rørsle). Ingen skalering, ingen glød, ingen skugge. Basert på Spole sin `FocusOutline.kt`, men utan `glow` og `focusScale`.
  - `@Composable fun StovaButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, primary: Boolean = false, icon: ImageVector? = null)`: min. 48 dp høg, `Raised`-fyll, eller lime med `onPrimary`-tekst når `primary`.
  - `@Composable fun PinDialog(title: String, onSubmit: suspend (String) -> PinCheck, onAccepted: () -> Unit, onDismiss: () -> Unit)`: talknappar 0–9 i 3×4-rutenett med «Slett» og «OK», og sifferprikkar. Talltastane på fjernkontrollen fungerer også. Ved `Rejected(w > 0)` / `LockedOut` blir teksten «Prøv igjen om %d s» vist og OK sperra til tida er ute.
  - `@Composable fun RemoteTextField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, keyboardType: KeyboardType = KeyboardType.Text)`: opnar tastaturet ved OK, ikkje ved fokus (som Spole `RemoteTextField.kt`).
  - `fun clockText(epochMillis: Long, zone: ZoneId, locale: Locale): String` → `"HH:mm"`. `@Composable fun Clock(modifier: Modifier = Modifier)` oppdaterer seg på minuttskiftet (`delay` til neste heile minutt).
  - `@Composable fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit)`, `@Composable fun SettingsSwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit, subtitle: String? = null)`, `@Composable fun SettingsNavRow(title: String, value: String? = null, onClick: () -> Unit)`.
  - Strengar: `pin_title`, `pin_delete`, `pin_ok`, `pin_wait_seconds` («Prøv igjen om %1$d s» / «Try again in %1$d s»), `pin_wrong` («Feil PIN» / «Wrong PIN»).

- [ ] **Step 1: Skriv testane som feilar**

```kotlin
@RunWith(AndroidJUnit4::class) @Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class PinDialogTest {
    @get:Rule val compose = createComposeRule()
    @Test fun acceptedPinCallsOnAccepted() {
        var accepted = false
        compose.setContent { StovaTheme { PinDialog("PIN", { if (it == "1357") PinCheck.Accepted else PinCheck.Rejected(0) }, { accepted = true }, {}) } }
        "1357".forEach { compose.onNodeWithText(it.toString()).performClick() }
        compose.onNodeWithText("OK").performClick(); compose.waitForIdle()
        assertTrue(accepted)
    }
    @Test fun lockoutShowsWaitAndDisablesOk() {
        compose.setContent { StovaTheme { PinDialog("PIN", { PinCheck.LockedOut(15_000) }, {}, {}) } }
        "0000".forEach { compose.onNodeWithText(it.toString()).performClick() }
        compose.onNodeWithText("OK").performClick()
        compose.onNodeWithText("Prøv igjen om 15 s").assertExists()
        compose.onNodeWithText("OK").assertIsNotEnabled()
    }
}
class ClockTest {
    @Test fun formats24h() = assertEquals("21:04", clockText(Instant.parse("2026-10-24T19:04:00Z").toEpochMilli(), ZoneId.of("Europe/Oslo"), Locale.forLanguageTag("nn")))
}
```

- [ ] **Step 2: Køyr og sjå feil**

Run: `pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest --tests "app.stova.core.ui.*"`
Expected: FAIL.

- [ ] **Step 3: Implementer komponentane i `core/ui` og strengane**

- [ ] **Step 4: Køyr testane (også `DesignRulesTest`)**

Run: `pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(ui): focus outline, buttons, PIN dialog, remote text field and clock"
```

---

### Task 7: Økt, sidemeny og navigasjon

**Files:**
- Create: `app/src/main/java/app/stova/profiles/ProfileSession.kt`
- Create: `app/src/main/java/app/stova/navigation/Destination.kt`, `SideRail.kt`, `StovaApp.kt`
- Modify: `app/src/main/java/app/stova/AppContainer.kt` (legg til `session`, `appScope`), `MainActivity.kt` (`setContent { StovaApp(container) }`)
- Test: `app/src/test/java/app/stova/profiles/ProfileSessionTest.kt`, `app/src/test/java/app/stova/navigation/StovaAppNavigationTest.kt`

**Interfaces:**
- Consumes: `ProfileRepository`, `decideStart`, `leavingNeedsAdultPin`, `StovaTheme`, `focusOutline`.
- Produces:
  - `class ProfileSession(repo: ProfileRepository, scope: CoroutineScope)` med `val active: StateFlow<Profile?>`, `val start: StateFlow<StartDecision?>` (rekna ut éin gong frå fila), `suspend fun enter(profileId: String)` (set aktiv og `rememberLast`) og `fun leave()` (aktiv = null, `start` = `Pick`). `active` følgjer endringar i fila, slik at ein redigert profil blir oppdatert straks.
  - `enum class Destination(@StringRes val label: Int) { HOME, APPS, SETTINGS }`. Plan 4 set inn `SMART_HOME` etter `HOME`.
  - `@Composable fun SideRail(destinations: List<Destination>, current: Destination, profile: Profile, expanded: Boolean, onSelect: (Destination) -> Unit, onProfile: () -> Unit, modifier: Modifier = Modifier)`: 80/200 dp, `Surface`-fyll i full høgd. Breiddeovergang 220 ms (0 ved redusert rørsle). Opnar seg når fokus går inn i menyen og lukkar seg når fokus forlèt han. Kvar destinasjon er minst 58 dp høg, har `Role.Tab` og er `selected` når aktiv. Aktiv destinasjon har `PrimaryContainer`-fyll.
  - `@Composable fun StovaApp(container: AppContainer)`: Ruta er `ProfileGate` når `active == null` (oppgåve 8 fyller han), elles `Row { SideRail; innhald }`. `BackHandler` på toppnivå: frå ei anna destinasjon → HOME, på HOME → ingenting. Innhaldet per destinasjon er ein tom `Box` med `testTag("screen-<namn>")` til seinare oppgåver fyller han.
  - Strengar: `nav_home` («Heim»/«Home»), `nav_apps` («Appar»/«Apps»), `nav_settings` («Innstillingar»/«Settings»), `nav_profile` («Byt profil»/«Switch profile»).

- [ ] **Step 1: Skriv testane som feilar**

```kotlin
class ProfileSessionTest {
    @get:Rule val tmp = TemporaryFolder()
    @Test fun enterSetsActiveAndRemembers() = runTest(UnconfinedTestDispatcher()) {
        val repo = ProfileRepository(JsonFileStore(tmp.root.resolve("p.json"), ProfilesFile.serializer(), ProfilesFile(), UnconfinedTestDispatcher()))
        val a = repo.create("A", AvatarColor.LIME, false)
        val s = ProfileSession(repo, backgroundScope); s.enter(a.id)
        assertEquals("A", s.active.value?.name); assertEquals(a.id, repo.file.first().lastProfileId)
        repo.update(a.id) { it.copy(name = "B") }; assertEquals("B", s.active.value?.name)
        s.leave(); assertNull(s.active.value)
    }
}
@RunWith(AndroidJUnit4::class) @Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class StovaAppNavigationTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    // Før-steg: opprett profil "A" gjennom container.profiles og kall session.enter i @Before.
    @Test fun railSelectsDestinationsAsTabs() {
        rule.onNodeWithText("Appar").performClick()
        rule.onNodeWithTag("screen-APPS").assertExists()
        rule.onNode(hasText("Appar") and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)).assertIsSelected()
    }
    @Test fun backFromAppsGoesHomeAndBackOnHomeStays() {
        rule.onNodeWithText("Appar").performClick()
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        rule.onNodeWithTag("screen-HOME").assertExists()
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        assertFalse(rule.activity.isFinishing)
    }
}
```

- [ ] **Step 2: Køyr og sjå feil**

Run: `pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest --tests "app.stova.profiles.ProfileSessionTest" --tests "app.stova.navigation.*"`
Expected: FAIL.

- [ ] **Step 3: Implementer `ProfileSession`, `Destination`, `SideRail` og `StovaApp`**

`StovaTheme` i `StovaApp` får `reduceMotion`/`strongFocus` frå `active.appearance`.

- [ ] **Step 4: Køyr testane**

Run: same som Step 2
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(nav): profile session, side rail and top-level back handling"
```

---

### Task 8: Profilval, første oppsett og profilredigering

**Files:**
- Create: `app/src/main/java/app/stova/profiles/ProfileGate.kt`, `ProfilePicker.kt`, `ProfileEditor.kt`, `AvatarBadge.kt`
- Modify: `app/src/main/java/app/stova/navigation/StovaApp.kt` (bruk `ProfileGate`; «Byt profil» i sidemenyen kallar `leave()` etter `leavingNeedsAdultPin`-sjekk)
- Test: `app/src/test/java/app/stova/profiles/ProfileGateTest.kt`

**Interfaces:**
- Consumes: `ProfileSession`, `ProfileRepository`, `StartDecision`, `PinDialog`, `RemoteTextField`, `StovaButton`, `AvatarColor`.
- Produces:
  - `@Composable fun ProfileGate(session: ProfileSession, repo: ProfileRepository)` handsamar `CreateFirst` (velkomst + `ProfileEditor` for første vaksne profil), `Pick` (`ProfilePicker`), `NeedsPin` (`PinDialog` direkte) og `Enter` (kallar `enter`).
  - `@Composable fun ProfilePicker(profiles: List<Profile>, onPick: (Profile) -> Unit, onAdd: (() -> Unit)?)`: ei horisontal rad med `AvatarBadge` på 120 dp, namn under, og hengelås-ikon når profilen har PIN. «Legg til profil» blir berre vist når `onAdd != null`.
  - `@Composable fun ProfileEditor(initial: Profile?, allowKid: Boolean, onSave: (name: String, avatar: AvatarColor, kid: Boolean) -> Unit, onCancel: () -> Unit)`. Namnet må vere 1–24 teikn.
  - `@Composable fun AvatarBadge(profile: Profile, size: Dp)`: sirkel i `avatar`-fargen med første bokstav i `onPrimary`.
  - Strengar: `welcome_title` («Velkomen til Stova»), `welcome_body` («Lag profilen din. Du kan leggje til fleire seinare.»), `profile_add` («Legg til profil»), `profile_name` («Namn»), `profile_kid` («Barneprofil»), `profile_save` («Lagre»), `profile_who` («Kven ser på?»), og engelske motstykke.

- [ ] **Step 1: Skriv testane som feilar**

```kotlin
@RunWith(AndroidJUnit4::class) @Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class ProfileGateTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @Test fun firstRunCreatesAdultProfileAndEnters() {
        rule.onNodeWithText("Velkomen til Stova").assertExists()
        rule.onNodeWithText("Barneprofil").assertDoesNotExist()
        rule.onNodeWithText("Namn").performTextInput("Øyvind")
        rule.onNodeWithText("Lagre").performClick()
        rule.onNodeWithTag("screen-HOME").assertExists()
    }
    @Test fun pickerAsksPinForProtectedProfile() { /* seed: "A" med PIN 1357, "K" barn, startInLastProfile=false */
        rule.onNodeWithText("Kven ser på?").assertExists()
        rule.onNodeWithText("A").performClick()
        rule.onNodeWithText("Skriv PIN for A").assertExists()
    }
    @Test fun kidCannotLeaveWithoutAdultPin() { /* seed: "A" med PIN, aktiv "K" */
        rule.onNodeWithText("Byt profil").performClick()
        rule.onNodeWithText("Skriv PIN for A").assertExists()
    }
    @Test fun pickerSurvivesDoubleFontScale() { /* seed som pickerAsksPin; sett fontScale 2f via RuntimeEnvironment.setFontScale(2f) */
        rule.onNodeWithText("A").assertIsDisplayed()
    }
}
```
Seed-hjelpar: `fun seed(file: ProfilesFile)` i `TestConfig.kt` skriv `profiles.json` til `context.filesDir` før aktiviteten startar (bruk `@get:Rule val rule = createAndroidComposeRule<MainActivity>()` med `RuleChain` der seed-regelen er ytst).

- [ ] **Step 2: Køyr og sjå feil**

Run: `pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest --tests "app.stova.profiles.ProfileGateTest"`
Expected: FAIL.

- [ ] **Step 3: Implementer `ProfileGate`, `ProfilePicker`, `ProfileEditor` og `AvatarBadge`, og kople dei inn i `StovaApp`**

Strengen `pin_for_profile` = «Skriv PIN for %1$s» / «Enter PIN for %1$s».

- [ ] **Step 4: Køyr testane**

Run: `pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(profiles): first-run setup, profile picker and PIN gate"
```

---

### Task 9: Appkatalog og rekkjefølgje

**Files:**
- Create: `app/src/main/java/app/stova/apps/InstalledApp.kt`, `AppCatalog.kt`, `PackageManagerAppCatalog.kt`, `AppArrangement.kt`, `AppRepository.kt`
- Modify: `app/src/main/AndroidManifest.xml` (`<queries>` for `MAIN`+`LEANBACK_LAUNCHER` og `MAIN`+`LAUNCHER`), `AppContainer.kt` (legg til `apps`)
- Test: `app/src/test/java/app/stova/apps/AppArrangementTest.kt`, `PackageManagerAppCatalogTest.kt`, `AppRepositoryTest.kt`

**Interfaces:**
- Consumes: `ProfileRepository.update`, `AppPrefs`, `Profile`.
- Produces:
  - `data class InstalledApp(val key: String, val packageName: String, val activityName: String, val label: String, val tv: Boolean)` (`key = "$packageName/$activityName"`)
  - `interface AppCatalog { suspend fun installed(): List<InstalledApp>; fun changes(): Flow<Unit>; fun launch(app: InstalledApp): Boolean; fun openAppInfo(packageName: String) }`
  - `class PackageManagerAppCatalog(context: Context, excluded: Set<String>) : AppCatalog`. LEANBACK_LAUNCHER blir skanna først, så LAUNCHER, utan duplikat per pakke (TV-aktiviteten vinn). `launch` bruker `getLeanbackLaunchIntentForPackage` med fallback til `getLaunchIntentForPackage`. `changes()` bruker `LauncherApps.Callback` som `callbackFlow`.
  - `excluded` = `setOf(context.packageName, "com.google.android.tvlauncher", "com.google.android.apps.tv.launcherx", "com.google.android.leanbacklauncher")`.
  - `data class ArrangedApps(val favorites: List<InstalledApp>, val all: List<InstalledApp>, val hidden: List<InstalledApp>)`
  - `fun arrangeApps(installed: List<InstalledApp>, prefs: AppPrefs, kid: Boolean, collator: Collator): ArrangedApps`: `favorites` i `prefs.favorites`-rekkjefølgje (berre installerte), `all` = ikkje skjulte sorterte med `collator`, `hidden` = skjulte sorterte. For barneprofil gjeld alt berre appar i `prefs.kidAllowed`, og `hidden` er tom.
  - `fun moveFavorite(favorites: List<String>, key: String, delta: Int): List<String>` (klemt til kantane)
  - `class AppRepository(catalog: AppCatalog, profiles: ProfileRepository, scope: CoroutineScope)` med `fun arranged(profile: Flow<Profile?>): Flow<ArrangedApps>` (ny skanning ved `changes()`), `suspend fun toggleFavorite(profileId: String, key: String)`, `suspend fun moveFavorite(profileId: String, key: String, delta: Int)`, `suspend fun setHidden(profileId: String, key: String, hidden: Boolean)`, `suspend fun setKidAllowed(profileId: String, key: String, allowed: Boolean)` og `suspend fun pruneMissing(profileId: String, installedKeys: Set<String>)`, som fjernar avinstallerte nøklar frå `favorites`, `hidden` og `kidAllowed`.

- [ ] **Step 1: Skriv testane som feilar**

```kotlin
class AppArrangementTest {
    private fun app(p: String, l: String) = InstalledApp("$p/.Main", p, ".Main", l, true)
    private val a = app("a", "Ærlig"); private val b = app("b", "Banan"); private val c = app("c", "Østen"); private val d = app("d", "Apple")
    private val nn = Collator.getInstance(Locale.forLanguageTag("nn"))
    @Test fun favoritesInOrderAllSortedNorwegian() {
        val r = arrangeApps(listOf(a, b, c, d), AppPrefs(favorites = listOf(c.key, "gone/.X", a.key)), kid = false, nn)
        assertEquals(listOf(c, a), r.favorites); assertEquals(listOf(d, b, a, c), r.all)
    }
    @Test fun hiddenAreSeparated() {
        val r = arrangeApps(listOf(a, b), AppPrefs(hidden = setOf(b.key)), false, nn)
        assertEquals(listOf(a), r.all); assertEquals(listOf(b), r.hidden)
    }
    @Test fun kidSeesOnlyAllowed() {
        val r = arrangeApps(listOf(a, b, c), AppPrefs(favorites = listOf(a.key, b.key), kidAllowed = setOf(b.key)), true, nn)
        assertEquals(listOf(b), r.favorites); assertEquals(listOf(b), r.all); assertEquals(emptyList<InstalledApp>(), r.hidden)
    }
    @Test fun moveFavoriteClamps() {
        assertEquals(listOf("x", "z", "y"), moveFavorite(listOf("x", "y", "z"), "y", +1))
        assertEquals(listOf("y", "x", "z"), moveFavorite(listOf("x", "y", "z"), "y", -5))
    }
}
@RunWith(AndroidJUnit4::class) @Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class PackageManagerAppCatalogTest {
    // Bruk shadowOf(pm).addActivityIfNotPresent + addIntentFilterForActivity for:
    //   "tv.app" med LEANBACK_LAUNCHER og LAUNCHER, "phone.app" berre LAUNCHER, "com.google.android.tvlauncher" med LEANBACK_LAUNCHER.
    @Test fun listsTvFirstDedupesAndExcludesStock() = runTest {
        val apps = PackageManagerAppCatalog(context, setOf(context.packageName, "com.google.android.tvlauncher")).installed()
        assertEquals(setOf("tv.app", "phone.app"), apps.map { it.packageName }.toSet())
        assertTrue(apps.first { it.packageName == "tv.app" }.tv); assertFalse(apps.first { it.packageName == "phone.app" }.tv)
    }
}
class AppRepositoryTest {
    // FakeAppCatalog med MutableStateFlow-liste; ProfileRepository i TemporaryFolder.
    @Test fun uninstalledFavoriteDisappearsAndIsPruned() = runTest { /* favorites = [a, b]; fjern b frå fake → arranged.favorites == [a]; pruneMissing → profile.apps.favorites == [a.key] */ }
}
```

- [ ] **Step 2: Køyr og sjå feil**

Run: `pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest --tests "app.stova.apps.*"`
Expected: FAIL.

- [ ] **Step 3: Implementer filene i `apps`, `<queries>` og `AppContainer.apps`**

`AppRepository.arranged` kallar `pruneMissing` når ei skanning manglar nøklar som profilen refererer til.

- [ ] **Step 4: Køyr testane**

Run: same som Step 2
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(apps): app catalog with TV-first listing, favorites, hidden and kid allow-list"
```

---

### Task 10: Appar på skjermen – heim-skjelett og appside

**Files:**
- Create: `app/src/main/java/app/stova/apps/AppArtCache.kt`, `AppTile.kt`, `AppsScreen.kt`, `AppActionsMenu.kt`
- Create: `app/src/main/java/app/stova/home/HomeScreen.kt`, `HomeViewModel.kt`, `HomeRowUi.kt`, `RowHeader.kt`
- Modify: `app/src/main/java/app/stova/navigation/StovaApp.kt` (HOME → `HomeScreen`, APPS → `AppsScreen`)
- Test: `app/src/test/java/app/stova/home/HomeViewModelTest.kt`, `app/src/test/java/app/stova/apps/AppsScreenTest.kt`

**Interfaces:**
- Consumes: `AppRepository`, `ArrangedApps`, `InstalledApp`, `AppCatalog.launch`, `HomeRowKey`, `arrangeRows`, `StartFocus`, `focusOutline`, `Clock`.
- Produces:
  - `class AppArtCache(context: Context, maxEntries: Int = 64)` med `suspend fun banner(app: InstalledApp): ImageBitmap?`. Henta på `Dispatchers.IO`: `loadBanner` ved `DisplayMetrics.DENSITY_XXHIGH` via `getResourcesForApplication` + `getDrawableForDensity`, og ved manglande banner `null`. Resultatet blir lagra i `LruCache<String, ImageBitmap>`. `suspend fun icon(app: InstalledApp): ImageBitmap?` gjer det same for ikonet.
  - `@Composable fun AppTile(app: InstalledApp, art: AppArtCache, onClick: () -> Unit, onLongClick: () -> Unit, modifier: Modifier = Modifier)`: 248 × 139,5 dp, 12 dp hjørne og `focusOutline`. Utan banner blir ikonet (64 dp) vist på `Raised`-flate med appnamnet under.
  - `sealed interface HomeRowUi { val key: HomeRowKey; data class Apps(override val key: HomeRowKey, val apps: List<InstalledApp>) : HomeRowUi }`. Plan 2 og 4 legg til fleire variantar.
  - `fun interface HomeRowSource { fun rows(profile: Profile): Flow<List<HomeRowUi>> }`. `HomeViewModel` slår saman alle kjelder.
  - `class HomeViewModel(session: ProfileSession, sources: List<HomeRowSource>) : ViewModel` med `val rows: StateFlow<List<HomeRowUi>>`, sortert med `arrangeRows`, og `val initialFocus: StateFlow<HomeRowKey?>`. `FAVORITE_APPS` gir `APPS`. `FIRST_MEDIA_ROW` gir første rad som ikkje er `APPS`, og fell tilbake til `APPS`.
  - `class AppsRowSource(apps: AppRepository, session: ProfileSession) : HomeRowSource` gir `HomeRowUi.Apps(HomeRowKey.APPS, favorites)`, eller ingenting når favorittlista er tom.
  - `@Composable fun HomeScreen(vm: HomeViewModel, art: AppArtCache, catalog: AppCatalog)`: øvst ein toppfelt med `Clock` til høgre (statusbrikker kjem i Plan 4), under `LazyColumn` av rader. Kvar rad er `RowHeader` + `LazyRow` med 16 dp mellomrom og 48 dp TV-kant.
  - `@Composable fun AppsScreen(apps: AppRepository, profile: Profile, art: AppArtCache, catalog: AppCatalog)`: rutenett (`LazyVerticalGrid`, `GridCells.Adaptive(248.dp)`) med «Favorittar» og «Alle appar». «Skjulte appar» ligg i ei samanfalda gruppe, ikkje for barneprofil. Lang OK eller Meny-tasten opnar `AppActionsMenu` med «Legg til i favorittar»/«Fjern frå favorittar», «Flytt til venstre», «Flytt til høgre», «Skjul»/«Vis» og «Appinfo». Barneprofil får berre opne appar.
  - Strengar for alle etikettane over, nynorsk og engelsk.

- [ ] **Step 1: Skriv testane som feilar**

```kotlin
class HomeViewModelTest {
    @Test fun startFocusFallsBackToAppsWithoutMediaRows() = runTest { /* sources: berre AppsRowSource-fake med 1 app; profile.home.startFocus = FIRST_MEDIA_ROW → initialFocus == HomeRowKey.APPS */ }
    @Test fun rowsFollowProfileOrder() = runTest { /* to fake-kjelder gir APPS og HomeRowKey("x"); rowOrder=[x] → rows.map{key} == [x, APPS] */ }
}
@RunWith(AndroidJUnit4::class) @Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class AppsScreenTest {
    @get:Rule val compose = createComposeRule()
    @Test fun addToFavoritesFromMenu() { /* FakeAppCatalog med "Banan"; long-click "Banan" → klikk "Legg til i favorittar" → profile.apps.favorites inneheld nøkkelen */ }
    @Test fun kidProfileHasNoMenuAndNoHiddenGroup() { /* kid-profil, kidAllowed={banan} → long-click "Banan" gir ingen meny; "Skjulte appar" finst ikkje */ }
}
```

- [ ] **Step 2: Køyr og sjå feil**

Run: `pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest --tests "app.stova.home.HomeViewModelTest" --tests "app.stova.apps.AppsScreenTest"`
Expected: FAIL.

- [ ] **Step 3: Implementer filene og kople dei inn i `StovaApp`**

`AppContainer` får `appArt: AppArtCache` og `homeSources: List<HomeRowSource>` (no berre `AppsRowSource`).

- [ ] **Step 4: Køyr testane**

Run: `pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(home): home skeleton with favorite apps row and full apps screen"
```

---

### Task 11: Innstillingar

**Files:**
- Create: `app/src/main/java/app/stova/settings/SettingsScreen.kt`, `ProfileSettings.kt`, `HomeRowSettings.kt`, `AppearanceSettings.kt`, `KidAppsSettings.kt`, `AboutSettings.kt`
- Modify: `app/src/main/java/app/stova/navigation/StovaApp.kt` (SETTINGS → `SettingsScreen`)
- Test: `app/src/test/java/app/stova/settings/SettingsScreenTest.kt`

**Interfaces:**
- Consumes: `ProfileRepository`, `ProfileSession`, `AppRepository`, `SettingsGroup`, `SettingsSwitchRow`, `SettingsNavRow`, `PinDialog`, `ProfileEditor`, `HomeRowKey`.
- Produces:
  - `@Composable fun SettingsScreen(container: AppContainer, profile: Profile)`. To-kolonne-oppsett som Spole sine TV-innstillingar: grupper til venstre, innhald til høgre. Gruppene er ei utvidbar liste, `enum class SettingsSection { PROFILE, HOME, HOME_BUTTON, APPEARANCE, ABOUT }`. Plan 2 legg til `ACCOUNTS`, Plan 4 `SMART_HOME`.
  - **Profil:** namn/avatar via `ProfileEditor`, PIN (set/endre/fjern, krev noverande PIN), «Start i sist brukte profil», «Barneprofilar», og for kvar barneprofil «Tillatne appar» (`KidAppsSettings`, brytarar per app via `setKidAllowed`), «Legg til profil» og «Slett profil».
  - **Heim:** startfokus (`FIRST_MEDIA_ROW` / `FAVORITE_APPS`), bakgrunnsrotasjon, og liste over tilgjengelege rader med «Vis»-brytar og flytt opp/ned. Lista kjem frå `HomeViewModel.availableRows` (legg til `val availableRows: StateFlow<List<HomeRowKey>>`).
  - **Utsjånad:** «Reduser rørsle», «Kraftig fokus».
  - **Om:** `BuildConfig.VERSION_NAME`.
  - **Barneprofil:** heile skjermen blir erstatta av éin knapp: «Byt profil».
  - `HOME_BUTTON`-seksjonen er ein `SettingsNavRow` som oppgåve 12 fyller.

- [ ] **Step 1: Skriv testane som feilar**

```kotlin
@RunWith(AndroidJUnit4::class) @Config(sdk = [30], qualifiers = TV_QUALIFIERS)
class SettingsScreenTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @Test fun reduceMotionPersists() { /* seed vaksen "A"; Innstillingar → Utsjånad → slå på "Reduser rørsle" → profile.appearance.reduceMotion == true */ }
    @Test fun kidSeesOnlySwitchProfile() { /* seed "A" + aktiv barneprofil "K" → klikk "Innstillingar" → "Byt profil" finst, "Utsjånad" finst ikkje */ }
    @Test fun allowAppForKid() { /* seed "A" (aktiv) + "K"; Profil → K → Tillatne appar → slå på "Banan" → K.apps.kidAllowed inneheld nøkkelen */ }
    @Test fun changingPinRequiresCurrentPin() { /* seed "A" med PIN 1357 → Profil → "Endre PIN" viser "Skriv PIN for A" først */ }
}
```

- [ ] **Step 2: Køyr og sjå feil**

Run: `pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest --tests "app.stova.settings.*"`
Expected: FAIL.

- [ ] **Step 3: Implementer innstillingssidene og strengane**

- [ ] **Step 4: Køyr testane**

Run: `pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest lintDebug`
Expected: PASS, 0 lint-feil.

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(settings): profile, home rows, appearance and kid app allow-list"
```

---

### Task 12: Home-knappen og kræsjvakt

**Files:**
- Create: `app/src/main/java/app/stova/homebutton/HomeKeyLogic.kt`, `HomeButtonService.kt`, `HomeButtonStatus.kt`, `CrashGuard.kt`, `HomeButtonSettings.kt`
- Create: `app/src/main/java/app/stova/settings/HouseholdSettings.kt`
- Create: `app/src/main/res/xml/home_button_service.xml`
- Modify: `AndroidManifest.xml` (service med `BIND_ACCESSIBILITY_SERVICE`), `StovaApplication.kt` (installer `CrashGuard`, kall `HomeButtonStatus.ensureEnabled`), `AppContainer.kt` (legg til `household`), `settings/SettingsScreen.kt`
- Test: `app/src/test/java/app/stova/homebutton/HomeKeyLogicTest.kt`, `CrashGuardTest.kt`, `HomeButtonStatusTest.kt`

**Interfaces:**
- Consumes: `JsonFileStore`, `SettingsSwitchRow`, `StovaButton`.
- Produces:
  - `@Serializable data class HouseholdSettings(val homeButtonOverride: Boolean = true)` i `files/household.json`, som `AppContainer.household: JsonFileStore<HouseholdSettings>`. Plan 4 legg til HA-felt.
  - `object StockLaunchers { val PACKAGES = setOf("com.google.android.tvlauncher", "com.google.android.apps.tv.launcherx") }`
  - `enum class KeyDecision { PASS, CONSUME, CONSUME_AND_LAUNCH }`
  - `class HomeKeyLogic(clock: () -> Long)` med `fun onKey(keyCode: Int, action: Int, repeatCount: Int, overrideOn: Boolean): KeyDecision` og `fun onWindowChanged(packageName: CharSequence?, overrideOn: Boolean): Boolean`. Konstantar: `RECENT_KEY_WINDOW_MS = 1500`, `LAUNCH_DEBOUNCE_MS = 1000`.
  - `class HomeButtonService : AccessibilityService`. `onServiceConnected` legg til `FLAG_REQUEST_FILTER_KEY_EVENTS`. `onKeyEvent` → `HomeKeyLogic`. `onAccessibilityEvent` → `onWindowChanged`. Oppstart med `Intent(ACTION_MAIN).addCategory(CATEGORY_HOME).setClass(this, MainActivity::class.java)` og flagga `NEW_TASK | REORDER_TO_FRONT | SINGLE_TOP`. `overrideOn` = `household.homeButtonOverride && !CrashGuard.tripped(context)`.
  - `home_button_service.xml`: `accessibilityEventTypes="typeWindowStateChanged"`, `packageNames="com.google.android.tvlauncher,com.google.android.apps.tv.launcherx"`, `accessibilityFlags="flagRequestFilterKeyEvents"`, `canRequestFilterKeyEvents="true"`, `canRetrieveWindowContent="false"`, `accessibilityFeedbackType="feedbackGeneric"`, `notificationTimeout="0"` og `description="@string/home_button_service_description"`.
  - `object HomeButtonStatus { fun isEnabled(enabledServices: String?, component: ComponentName): Boolean; fun withComponent(enabledServices: String?, component: ComponentName): String; fun ensureEnabled(context: Context): Boolean }`. `ensureEnabled` skriv `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES` og `ACCESSIBILITY_ENABLED=1` berre når `WRITE_SECURE_SETTINGS` er gitt, elles ingenting.
  - `object CrashGuard { const val WINDOW_MS = 300_000L; const val THRESHOLD = 3; fun record(times: List<Long>, now: Long): Pair<List<Long>, Boolean>; fun install(context: Context); fun tripped(context: Context): Boolean; fun reset(context: Context) }`. `files/crash_guard.json` = `@Serializable data class CrashGuardState(val times: List<Long> = emptyList(), val tripped: Boolean = false)`, skrive med `JsonFileStore.writeBlocking` i `UncaughtExceptionHandler`. Deretter blir den førre handsamaren kalla.
  - `@Composable fun HomeButtonSettings(household: JsonFileStore<HouseholdSettings>)`:
    - Status: «Tilgjengetenesta er på» / «av», og knappen «Opne tilgjengeinnstillingar» (`Settings.ACTION_ACCESSIBILITY_SETTINGS`).
    - Brytaren «Home-knappen opnar Stova».
    - Kræsjmeldinga frå spesifikasjonen §9 med knappen «Slå på att» (`CrashGuard.reset`).
    - adb-oppskrifta som tekst.
    - Åtvaringa «Ikkje slå av systemlauncheren. Nokre einingar startar ikkje då.»

- [ ] **Step 1: Skriv testane som feilar**

```kotlin
class HomeKeyLogicTest {
    private var now = 10_000L; private val l = HomeKeyLogic { now }
    private val H = KeyEvent.KEYCODE_HOME; private val D = KeyEvent.ACTION_DOWN; private val U = KeyEvent.ACTION_UP
    @Test fun otherKeysPass() = assertEquals(KeyDecision.PASS, l.onKey(KeyEvent.KEYCODE_DPAD_UP, D, 0, true))
    @Test fun homePassesWhenOverrideOff() = assertEquals(KeyDecision.PASS, l.onKey(H, D, 0, false))
    @Test fun homeDownConsumedUpLaunches() {
        assertEquals(KeyDecision.CONSUME, l.onKey(H, D, 0, true)); assertEquals(KeyDecision.CONSUME_AND_LAUNCH, l.onKey(H, U, 0, true)) }
    @Test fun windowFallbackSkippedRightAfterKey() {
        l.onKey(H, D, 0, true); now += 1_499
        assertFalse(l.onWindowChanged("com.google.android.tvlauncher", true)) }
    @Test fun windowFallbackLaunchesForStockOnly() {
        assertTrue(l.onWindowChanged("com.google.android.tvlauncher", true)); now += 1_001
        assertFalse(l.onWindowChanged("com.netflix.ninja", true)) }
    @Test fun launchesAreDebounced() {
        assertTrue(l.onWindowChanged("com.google.android.apps.tv.launcherx", true)); now += 999
        assertFalse(l.onWindowChanged("com.google.android.apps.tv.launcherx", true)) }
}
class CrashGuardTest {
    @Test fun threeCrashesInFiveMinutesTrip() {
        val (t1, a) = CrashGuard.record(emptyList(), 0); val (t2, b) = CrashGuard.record(t1, 60_000); val (_, c) = CrashGuard.record(t2, 120_000)
        assertFalse(a); assertFalse(b); assertTrue(c) }
    @Test fun oldCrashesExpire() {
        val (t, tripped) = CrashGuard.record(listOf(0L, 1_000L), 301_001); assertFalse(tripped); assertEquals(listOf(301_001L), t) }
}
class HomeButtonStatusTest {
    private val me = ComponentName("app.stova", "app.stova.homebutton.HomeButtonService")
    @Test fun detectsEnabledAmongOthers() = assertTrue(HomeButtonStatus.isEnabled("x.y/.A:app.stova/app.stova.homebutton.HomeButtonService", me))
    @Test fun appendsWithoutDuplicates() {
        assertEquals("x.y/.A:app.stova/app.stova.homebutton.HomeButtonService", HomeButtonStatus.withComponent("x.y/.A", me))
        assertEquals("app.stova/app.stova.homebutton.HomeButtonService", HomeButtonStatus.withComponent("app.stova/app.stova.homebutton.HomeButtonService", me)) }
}
```

- [ ] **Step 2: Køyr og sjå feil**

Run: `pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest --tests "app.stova.homebutton.*"`
Expected: FAIL.

- [ ] **Step 3: Implementer `homebutton/*`, `HouseholdSettings`, service-XML, manifest og innstillingssida**

`isEnabled` samanliknar også mot den korte forma `app.stova/.homebutton.HomeButtonService`.

- [ ] **Step 4: Køyr alle testar og lint**

Run: `pwsh -NoProfile -File scripts/gradle.ps1 testDebugUnitTest lintDebug assembleDebug`
Expected: PASS, 0 lint-feil.

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(homebutton): accessibility Home override with window fallback and crash guard"
```

---

### Task 13: Verifisering på emulator og Shield

**Files:**
- Create: `scripts/Start-StovaEmulator.ps1`, `docs/verification/plan-1.md`

**Interfaces:**
- Consumes: alt over.
- Produces: `Stova_TV`-AVD i `C:\Launcher\.avd` (port 5580) og ein verifiseringsrapport.

- [ ] **Step 1: Lag emulatoren (éin gong)**

```powershell
& C:\Android\sdk\cmdline-tools\latest\bin\sdkmanager.bat "system-images;android-30;android-tv;x86"
$env:ANDROID_AVD_HOME = 'C:\Launcher\.avd'
& C:\Android\sdk\cmdline-tools\latest\bin\avdmanager.bat create avd -n Stova_TV -k "system-images;android-30;android-tv;x86" -d tv_1080p
```
`scripts/Start-StovaEmulator.ps1` set `ANDROID_AVD_HOME` og startar `C:\Android\sdk\emulator\emulator.exe -avd Stova_TV -port 5580 -no-snapshot-save`. Deretter ventar skriptet på `adb -s emulator-5580 wait-for-device` og `sys.boot_completed=1`.

- [ ] **Step 2: Køyr instrumenterte testar på Stova_TV**

Run: `$env:ANDROID_SERIAL='emulator-5580'; pwsh -NoProfile -File scripts/gradle.ps1 connectedDebugAndroidTest`
Expected: `KeystoreSecretStoreTest` PASS.

- [ ] **Step 3: Manuell sjekkliste på Stova_TV, deretter på Shield (`adb connect <ip>:5555`)**

Installer: `adb -s <serial> install -r app/build/outputs/apk/debug/app-debug.apk`. Kryss av i `docs/verification/plan-1.md`:
- Første oppsett → profil → heim med klokke.
- Legg til barneprofil med PIN på vaksenprofilen. Byt profil frå barneprofilen krev PIN.
- Appar: favoritt, flytt, skjul, appinfo, og at appane startar.
- Slå på tilgjengetenesta. Home på fjernkontrollen gir Stova utan at systemlauncheren blinkar. Home frå mobilfjernkontrollen gir Stova via fallback.
- Tvinga kræsj tre gonger (`adb shell am crash app.stova.debug`). Home går då til systemlauncheren, og «Slå på att» fungerer.
- Skriftstorleik 2,0 (`adb shell settings put system font_scale 2.0`): profilval, sidemeny og innstillingar er lesbare, og ingenting er klipt. Set tilbake til 1.0 etterpå.

- [ ] **Step 4: Commit**

```bash
git add -A && git commit -m "docs: plan 1 verification on Stova_TV and Shield"
```
